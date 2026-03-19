# День 23 — Верификация: Требования → Реализация → Проверка

## Как читать этот документ

Для каждого требования продуктовой задачи:
1. **Реализация** — какие классы и файлы отвечают за это
2. **Как проверить** — конкретные curl-команды и ожидаемый результат
3. **Где смотреть сравнение** — какие отчёты читать

---

## Требование 1: Reranker или фильтр релевантности

> *"reranker или фильтр релевантности (порог similarity / отдельная модель / heuristic)"*

### Реализация

| Компонент | Файл | Роль |
|-----------|------|------|
| `ThresholdFilterStep` | `pipeline/steps/ThresholdFilterStep.kt` | Отсекает результаты с `score < threshold` |
| `HeuristicReranker` | `search/rerank/HeuristicReranker.kt` | Keyword overlap bonus (+score за совпадения слов) |
| `LlmReranker` | `search/rerank/LlmReranker.kt` | LLM оценивает релевантность каждого чанка (0.0–1.0) |
| `Reranker` | `search/rerank/Reranker.kt` | Общий интерфейс |
| `RerankStep` | `pipeline/steps/RerankStep.kt` | Интегрирует Reranker в pipeline |

### Как работает

```
ThresholdFilterStep: results.filter { it.score >= threshold }
HeuristicReranker:   bonus = (queryWords ∩ chunkWords).size / queryWords.size * 0.1
LlmReranker:         prompt → "Chunk N: X.XX" → regex parse → sort by new score
```

### Как проверить

```bash
# Без фильтра (baseline)
curl "http://localhost:3001/search?query=ContextPacker&preset=baseline"

# С threshold filter
curl "http://localhost:3001/search?query=ContextPacker&preset=filtered"

# С heuristic reranker
curl "http://localhost:3001/search?query=ContextPacker&preset=reranked_heuristic"

# С LLM reranker
curl "http://localhost:3001/search?query=ContextPacker&preset=reranked_llm"
```

**Ожидаемый результат:** в debug-заголовке ответа виден эффект каждого шага:
```
Pipeline: RERANKED_LLM | Retrieved: 15 → Filtered: 9 → Final: 5
Timings: retrieve=312ms, filter=0ms, rerank=2100ms, top_k=0ms, pack=1ms
```

---

## Требование 2: Порог отсечения нерелевантных результатов

> *"👉 порог отсечения нерелевантных результатов"*

### Реализация

| Компонент | Файл | Параметр |
|-----------|------|---------|
| `ThresholdFilterStep` | `pipeline/steps/ThresholdFilterStep.kt` | `threshold: Double` |
| `PipelineConfig.threshold` | `pipeline/PipelineConfig.kt` | 0.0 = выключен |
| Query param | `RagServer.kt` → `parsePipelineConfig()` | `?threshold=0.65` |

Пресеты с включённым фильтром:
- `filtered`: threshold = 0.65
- `reranked_heuristic`: threshold = 0.50
- `reranked_llm`: threshold = 0.50

### Как проверить

```bash
# Разные пороги — сравниваем сколько проходит
curl "http://localhost:3001/search?query=ContextPacker&retrieval_topK=20&threshold=0.3&final_topK=10"
curl "http://localhost:3001/search?query=ContextPacker&retrieval_topK=20&threshold=0.65&final_topK=10"
curl "http://localhost:3001/search?query=ContextPacker&retrieval_topK=20&threshold=0.8&final_topK=10"
```

**Ожидаемый результат:** `Retrieved: 20 → Filtered: N` — N уменьшается с ростом порога.

**Что смотреть:** первая строка ответа. Если все отфильтрованы — ответ "Ничего не найдено".

---

## Требование 3: Топ-K до и после фильтрации

> *"👉 топ-K до и после фильтрации"*

### Реализация

| Шаг | Файл | Параметр | Момент среза |
|-----|------|---------|---|
| `RetrievalStep` | `pipeline/steps/RetrievalStep.kt` | `retrievalTopK` | ДО фильтра — сколько достаём |
| `ThresholdFilterStep` | `pipeline/steps/ThresholdFilterStep.kt` | `threshold` | Убирает нерелевантных |
| `TopKStep` | `pipeline/steps/TopKStep.kt` | `finalTopK` | ПОСЛЕ фильтра/реранка — сколько в LLM |
| `PipelineMetrics` | `pipeline/PipelineContext.kt` | `countAfterRetrieval`, `countAfterFilter` | Хранит историю |

### Как проверить

```bash
curl "http://localhost:3001/search?query=ContextPacker&retrieval_topK=15&threshold=0.65&final_topK=5"
```

**Ожидаемый результат:**
```
Pipeline: NONE | Retrieved: 15 → Filtered: N → Final: 5
```
Значение `N` показывает сколько прошло порог. Это и есть "топ-K после фильтрации" в реальных числах.

---

## Требование 4: Query Rewrite

> *"Улучшенный RAG: фильтрация/реранкинг + query rewrite + сравнение режимов"*

### Реализация

| Компонент | Файл | Роль |
|-----------|------|------|
| `QueryOptimizer` | `search/QueryOptimizer.kt` | LLM rewrite: translate + keyword expansion |
| `QueryOptimizeStep` | `pipeline/steps/QueryOptimizeStep.kt` | Интегрирует в pipeline |
| `TRANSLATE_QUERIES` | env variable | Глобальный разрешитель (создаёт QueryOptimizer) |
| `enable_query_optimize` | query param | Активатор для конкретного запроса |

**Промпт QueryOptimizer:**
1. Делает запрос самодостаточным
2. Переводит на английский
3. Убирает мусорные слова
4. Добавляет технические ключевые слова

### Как проверить

Требует `TRANSLATE_QUERIES=true` при запуске сервера.

```bash
# Без оптимизации
curl "http://localhost:3001/search?query=как+работает+упаковка+контекста"

# С оптимизацией
curl "http://localhost:3001/search?query=как+работает+упаковка+контекста&enable_query_optimize=true"
```

**Где смотреть:** в логах сервера:
```
[QueryOptimizer] 'как работает упаковка контекста' → 'ContextPacker class grouping token limit packing results'
```

**В отчёте evaluation:**
```
## Query 1: "как работает упаковка контекста?"
**Optimized:** "ContextPacker class grouping token limit packing results"
```

---

## Требование 5: Сравнение качества без фильтра vs с фильтром

> *"👉 качество без фильтра/rewriting"*
> *"👉 качество с фильтром"*

### Реализация

| Компонент | Файл | Роль |
|-----------|------|------|
| `EvaluationService` | `evaluation/EvaluationService.kt` | Прогоняет вопросы через пресеты, сохраняет отчёты |
| `PipelinePreset` | `pipeline/PipelineConfig.kt` | 4 именованных конфигурации |
| `/evaluate` endpoint | `RagServer.kt` | REST API для запуска evaluation |
| `@@talk(rag --gentest)` | `RagCommandHandler.kt` | Android-команда |
| `TestQueries` | `TestQueries.kt` | Список тестовых вопросов |

### Как запустить сравнение

**Вариант А — curl:**
```bash
curl -X POST http://localhost:3001/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "questions": [
      "Как работает двухэтапный поиск?",
      "Где хранятся эмбеддинги?",
      "Как работает ContextPacker?",
      "Что делает QueryOptimizer?",
      "Как индексируются файлы?"
    ],
    "presets": ["all"]
  }'
```

**Вариант Б — из Android приложения:**
```
@@talk(rag --gentest)
```

### Где смотреть результаты

Отчёты сохраняются на сервере в `./reports/`:
```
reports/eval_BASELINE_2024-03-18_14-30.md
reports/eval_FILTERED_2024-03-18_14-30.md
reports/eval_RERANKED_HEURISTIC_2024-03-18_14-30.md
reports/eval_RERANKED_LLM_2024-03-18_14-30.md
```

### Как читать отчёт

**Summary таблица** в конце каждого файла:
```markdown
## Summary
| Метрика | Значение |
|---------|---------|
| Всего вопросов | 10 |
| Avg top score | 0.74 |       ← главная метрика для сравнения
| retrievalTopK | 15 |
| threshold | 0.65 |
| rerankStrategy | NONE |
| finalTopK | 5 |
```

**Для каждого вопроса:**
```markdown
## Query 3: "Как работает ContextPacker?"
**Optimized:** "ContextPacker class grouping token limit packing"
**Metrics:** Retrieved: 15 → After filter: 9 → Final: 5
**Top score:** 0.84 | Avg score: 0.71
**Timings:** retrieve=312ms, filter=0ms, top_k=0ms, pack=1ms

### RAG Context:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[CLASS] ContextPacker | score: 0.841
...
```

### Интерпретация сравнения

| Что сравниваем | Что значит "лучше" |
|---|---|
| `Avg top score` | Выше = найденные чанки более релевантны |
| `After filter / Final` | Соотношение показывает сколько "шума" отсеяли |
| `Timings` | Видна стоимость каждого шага |
| RAG Context | Качественный анализ — смотрим глазами что вернулось |

**Типичный паттерн:**
- `FILTERED avg score` < `BASELINE avg score` — нормально, фильтр убирает легкие попадания
- `RERANKED_LLM avg score` — может быть выше или ниже, зависит от качества модели
- Главное — смотреть RAG Context: стал ли он более релевантным по содержанию

---

## Чеклист полной проверки

```
□ 1. Сервер запускается с новыми параметрами
      export TRANSLATE_QUERIES=true
      java -jar rag-server.jar

□ 2. /search baseline работает
      curl "http://localhost:3001/search?query=ContextPacker"
      Ожидание: ответ с debug заголовком "Retrieved: 10 → Final: 5"

□ 3. Threshold filter работает
      curl "http://localhost:3001/search?query=ContextPacker&preset=filtered"
      Ожидание: "Retrieved: 15 → Filtered: N → Final: 5", N < 15

□ 4. Heuristic reranker работает
      curl "http://localhost:3001/search?query=ContextPacker&preset=reranked_heuristic"
      Ожидание: "Reranked: N" в debug строке, порядок результатов изменился

□ 5. LLM reranker работает
      curl "http://localhost:3001/search?query=ContextPacker&preset=reranked_llm"
      Ожидание: timing rerank=~2000ms, scores обновлены

□ 6. Query optimization работает (требует TRANSLATE_QUERIES=true)
      curl "http://localhost:3001/search?query=как+работает+пакинг&enable_query_optimize=true"
      Ожидание: лог "[QueryOptimizer] '...' → '...'"

□ 7. /evaluate endpoint работает
      curl -X POST http://localhost:3001/evaluate \
        -H "Content-Type: application/json" \
        -d '{"questions":["Как работает ContextPacker?"],"presets":["baseline","filtered"]}'
      Ожидание: JSON с savedReports, файлы созданы в ./reports/

□ 8. Отчёты читаемы
      cat reports/eval_BASELINE_*.md
      cat reports/eval_FILTERED_*.md
      Сравнить Avg top score в Summary

□ 9. @@talk(rag --gentest) работает (из Android)
      Ожидание: summary с avg scores для всех пресетов

□ 10. Кастомные параметры работают
       curl "http://localhost:3001/search?query=test&retrieval_topK=20&threshold=0.7&rerank_strategy=heuristic&final_topK=3"
       Ожидание: "Retrieved: 20 → Filtered: N → Final: 3"
```

---

## Где смотреть при проблемах

| Проблема | Где смотреть |
|----------|---|
| LLM reranker не работает | Логи сервера: `[LlmReranker] Failed to parse scores` |
| Query optimizer пропускается | Лог: `[Pipeline] enable_query_optimize=true but TRANSLATE_QUERIES=false` |
| Filter убивает все результаты | Снизить `threshold` (попробовать 0.3–0.5) |
| /evaluate не создаёт файлы | Проверить права на директорию `./reports/` |
| Android --gentest ошибка | Лог сервера, проверить URL через `@@talk(rag --state)` |
