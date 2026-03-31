# RAG Server — Debug Instructions

## Быстрая диагностика: запрос не возвращает нужный файл

### 0. Убить старый процесс сервера, если надо
```bash
lsof -ti:3001 | xargs kill -9
```

### 1. Проверить состояние индекса

```bash
curl -s http://localhost:3001/debug/index
```

Пример вывода:
```
# Debug Index State

## Stats
- Total chunks: 2570
- Structural chunks: 919
- Fixed chunks: 1651
- Class metadata entries: 474
- Metadata vectors: 474
- Last indexed: 2026-03-31T05:00:00Z

## Chunks per file (strategy=structural, 476 files)
  3	AIAgent.kt ✓meta
  5	AIAgentFactory.kt
  2	ChatRepository.kt ✓meta
  ...
```

Что смотреть:
- `Class metadata entries: 0` → `EXTRACT_METADATA` не запускался → Two-Stage fallback на standardSearch
- Файл есть в списке, но без `✓meta` → метаданные не извлечены для этого класса
- Файл полностью отсутствует → не был проиндексирован (проверить `CODE_PATH`)

### 2. Проверить orphan-метаданные (засорение от library-классов)

```bash
curl -s http://localhost:3001/debug/metadata/orphans
```

Пример вывода:
```
# Metadata Orphans (no backing chunks)
total_metadata=1245  orphans=771  backed=474

  AIAgentBase — Abstract base class representing a single-use AI agent with state.
  AIAgentBuilder — Builds and configures AI agents with various components.
  ...
```

Что смотреть:
- `orphans=` — сколько metadata-записей не имеют matching chunks в structural индексе
- Orphans = library-классы, hallucinated LLM-ответы, внутренние sealed-классы без отдельных файлов
- Если `orphans` >> `backed` — Stage 1 засорён, нужна чистка

**Проблема:** orphan-классы занимают слоты Stage 1 и вытесняют проектные классы.
**Решение:** перезапуск с `FORCE_REINDEX=true EXTRACT_METADATA=true` + новый enforcement-fix (className всегда overwrite после LLM).

### 3. Диагностика Stage 1 — почему нужный класс не выбирается

```bash
# Топ-20 классов для запроса + разбивка emb/kw/exact по каждому
curl -s "http://localhost:3001/debug/stage1?query=AIAgent&top_k=20"

# Найти конкретный класс и его rank среди всех
curl -s "http://localhost:3001/debug/stage1?query=AIAgent&class=AIAgent"
```

Пример вывода:
```
# Debug Stage 1: query="AIAgent"
top_k=20  total_metadata=474  tokens=[aiagent]

## Top 20 classes
  [0] total=1,7125  emb=0,5125  kw=0,2000  exact=1,0  — AIAgent ★exact
  [1] total=0,8957  emb=0,6957  kw=0,2000  exact=0,0  — AIAgentFactory
  [2] total=0,8351  emb=0,6351  kw=0,2000  exact=0,0  — AIAgentState.NotStarted ⚠orphan
  ...

## All matching "AIAgent" (rank in full list)
  rank=0   total=1,7125  emb=0,5125  kw=0,2000  exact=1,0  — AIAgent ★exact
  rank=1   total=0,8957  emb=0,6957  kw=0,2000  exact=0,0  — AIAgentFactory
  ...
```

Параметры:
- `query` — запрос (обязательный)
- `top_k` — сколько классов показать (дефолт: 20)
- `class` — фильтр по имени класса + показывает rank в полном списке

Что смотреть:
- `exact=1.0` + `★exact` → класс нашёлся по точному совпадению имени — это правильно
- `⚠orphan` → класс в metadata, но нет chunks → занимает слоты Stage 1 зря
- `kw=0,0000` → токен запроса не найден в metadata-тексте → проблема с tokenization или metadata
- Нужный класс имеет `rank=50+` → metadata засорены orphans или embedding плохо матчит

### 4. Raw similarity поиск в обход pipeline

```bash
# Все чанки с query=AIAgent, топ-20
curl -s "http://localhost:3001/debug/chunks?query=AIAgent"

# Только чанки из файлов, в имени которых есть "AIAgent"
curl -s "http://localhost:3001/debug/chunks?query=AIAgent&file=AIAgent"

# С ограничением количества результатов
curl -s "http://localhost:3001/debug/chunks?query=AIAgent&file=AIAgent&top_k=10"

# По fixed-стратегии вместо structural
curl -s "http://localhost:3001/debug/chunks?query=AIAgent&strategy=fixed&top_k=5"
```

Пример вывода:
```
# Debug Chunks: query="AIAgent"
strategy=structural  top_k=10  file=AIAgent  searched=8 chunks

## [0] score=0.6821  AIAgent.kt:1
decl=AIAgent  parent=-  nodeType=class_declaration
```
class AIAgent @Inject constructor(
    val config: AgentConfig,
    ...
```
...
```

Что смотреть:
- Score у топ-чанков 0.3–0.5 → embedding model плохо матчит короткий запрос
- Нужный файл вообще не появляется → не проиндексирован

### 5. Посмотреть metadata и embedding text конкретного класса

```bash
curl -s "http://localhost:3001/debug/metadata?class=AIAgent"
```

Пример вывода:
```
# Debug Metadata
filter="AIAgent"  found=3

## AIAgent  ✓vec
**Responsibility:** Orchestrates AI processing, handling user messages and delegating to strategies.
**Domain tags:** AI, Agent
**Key methods:** process, getInfo, getFullContext
**Embedding text:** `Orchestrates AI processing, handling user messages...`

## AIAgentFactory  ✓vec
...
```

Что смотреть:
- `✗vec` → metadata есть, но вектор не сгенерирован → Phase C не запускалась
- `Embedding text` → этот текст идёт в модель для Stage 1. Если класс не упоминает себя по имени → низкий emb score для именного запроса
- `Key methods` содержат поля вместо методов → LLM extracter вернул неточные данные

### 6. Поиск через pipeline (с логом TwoStage internals)

```bash
curl -s -X POST http://localhost:3001/search \
  -H "Content-Type: application/json" \
  -d '{"query": "AIAgent"}'
```

После запроса в `./logs/debug_YYYY-MM-DD.md` появится секция:
```
### TWO-STAGE RETRIEVAL  query="AIAgent"
metadata=474  metadataVectors=474  fallback=false

Stage 1 — selected classes (top 8):
  1,712 — AIAgent ★exact
  0,895 — AIAgentFactory
  ...

Stage 2 — drill-down per class:
  AIAgent: exactMatch=3 softMatch=0
    0,682 — AIAgent.kt:1
    0,613 — AIAgent.kt:45
    ...
```

Если в логе `fallback=true` — метаданных нет, Two-Stage работал как обычный embedding search.

---

## Корень типичных проблем

### "AIAgent" не находит AIAgent.kt — почему?

Возможная причина 1: `EXTRACT_METADATA=false` (дефолт) → metadata пустые → Two-Stage fallback на `standardSearch()` = embedding-поиск без роутинга.

Возможная причина 2: Metadata засорены orphan-классами от koog-библиотеки. Все они содержат "AIAgent" в имени → все получают keyword boost → реальный `AIAgent` вытесняется на rank 100+.

**Диагностика:** `curl -s "http://localhost:3001/debug/stage1?query=AIAgent&class=AIAgent"` — смотрим rank нужного класса.

**Решение:** exact match boost уже включён (score +1.0 если query == className). При следующем FORCE_REINDEX orphans будут вычищены enforcement-fix'ом.

### Метаданные есть, но класс не попадает в Stage 1

Stage 1 отбирает топ-8 классов по:
- embedding similarity метаданных
- keyword boost: `min(kwScore * 0.2, 0.2)` — максимум +0.2
- exact match boost: +1.0 если `query == className` (case-insensitive)

Смотреть Stage 1 скоры: `curl -s "http://localhost:3001/debug/stage1?query=X&class=X"`

### Класс в Stage 1, но его чанки не находятся в Stage 2

Stage 2 ищет чанки по:
1. **Exact match:** `fileName.removeSuffix(".kt") == className` ИЛИ `declarationName == className` ИЛИ `parentScope contains className`
2. **Soft match (fallback):** `content contains className` — только если `className.length >= 6`

Если `exactMatch=0 softMatch=0` — чанки не привязаны к классу. Возможные причины:
- Структурный чанкер не смог выделить `declarationName`
- Имя класса в metadata и имя файла не совпадают

---

## Логи

| Файл | Содержимое |
|------|------------|
| `./logs/session_YYYY-MM-DD.md` | Компактный лог: запросы, оптимизация, финальные результаты |
| `./logs/debug_YYYY-MM-DD.md` | Детальный: TwoStage internals, промпты LLM, сырые ответы |
| `./logs/index_YYYY-MM-DD.md` | Лог индексации: чанки по каждому файлу (только при FORCE_REINDEX=true) |

---

## Env переменные для диагностики

```bash
EXTRACT_METADATA=true       # Включить LLM-извлечение метаданных для Two-Stage
FORCE_REINDEX=true          # Пересобрать индекс с нуля
TRANSLATE_QUERIES=true      # Включить оптимизацию запросов через LLM
USE_AST_CHUNKING=true       # AST-чанкинг вместо regex (лучше declarationName)
```

---

## Диагностический сценарий: шаг за шагом

**Шаг 1.** Проверить индекс и orphans:

```bash
curl -s http://localhost:3001/debug/index
# Смотрим: metadata=0? → EXTRACT_METADATA не запускался

curl -s http://localhost:3001/debug/metadata/orphans
# Смотрим: orphans >> backed? → засорение library-классами
```

**Шаг 2.** Найти класс в Stage 1:

```bash
curl -s "http://localhost:3001/debug/stage1?query=AIAgent&class=AIAgent"
# Смотрим: rank > 8? → класс не попадает в Stage 1
# exact=1.0? → exact boost сработал
# ⚠orphan рядом с конкурентами? → засорение
```

**Шаг 3.** Посмотреть raw chunk scores:

```bash
curl -s "http://localhost:3001/debug/chunks?query=AIAgent&file=AIAgent"
# Смотрим: score у нужных чанков > 0.55? → проблема в Stage 1, не в embedding
# score < 0.4? → проблема с моделью или чанкингом
```

**Шаг 4.** Запустить поиск и прочитать debug лог:

```bash
curl -s -X POST http://localhost:3001/search \
  -H "Content-Type: application/json" \
  -d '{"query": "AIAgent"}'

cat ./logs/debug_$(date +%Y-%m-%d).md | grep -A 20 "TWO-STAGE RETRIEVAL"
```
