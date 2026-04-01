# CRM Server — Инструкция по настройке и запуску

MCP-сервер для управления пользователями и тикетами поддержки. Хранит данные в SQLite, предоставляет инструменты через MCP Streamable HTTP. Используется Support-агентом Android-приложения.

---

## Требования

- JDK 21+
- Gradle 8.5+ (или `./gradlew` из корня проекта)

---

## 1. Переменные окружения

| Переменная | По умолчанию | Описание |
|-----------|---|---|
| `CRM_DB_PATH` | `crm.db` | Путь к файлу SQLite базы данных |
| `CRM_SERVER_PORT` | `3002` | Порт, на котором слушает crm-server |

---

## 2. Сборка

```bash
./gradlew :crm-server:build
```

Jar: `crm-server/build/libs/crm-server.jar`

---

## 3. Варианты запуска

### Локально через Gradle

```bash
./gradlew :crm-server:run
```

### Из JAR

```bash
java -jar crm-server/build/libs/crm-server.jar
```

### С кастомными параметрами

```bash
CRM_DB_PATH=/data/crm.db CRM_SERVER_PORT=3002 java -jar crm-server/build/libs/crm-server.jar
```

---

## 4. Проверка работы

```bash
# Сервер запущен?
curl http://localhost:3002/health
```

Ожидаемый ответ:
```json
{"status": "ok"}
```

---

## 5. MCP-инструменты

Транспорт: **Streamable HTTP** (`/mcp`).

| Инструмент | Описание | Параметры |
|-----------|---|---|
| `get_crm_user_by_chat` | Найти пользователя по chatId. Возвращает объект или `{"found": false}` | `chatId: integer` |
| `create_crm_user` | Создать пользователя (или вернуть существующего) | `chatId: integer`, `userName: string` |
| `get_crm_user_tickets` | Список тикетов пользователя | `chatId: integer` |
| `create_crm_ticket` | Создать тикет (статус `open` выставляется автоматически) | `chatId: integer`, `title: string`, `description: string` |
| `update_crm_ticket` | Обновить статус тикета | `ticketId: integer`, `status: open\|closed\|operator`, `result?: string` |

Статусы тикета: `open` → `closed` (решено) или `operator` (эскалация к оператору).

### Пример вызова через curl (MCP JSON-RPC)

```bash
curl -X POST http://localhost:3002/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "get_crm_user_by_chat",
      "arguments": {"chatId": 123456}
    }
  }'
```

---

## 6. Подключение в Android-приложении

По умолчанию приложение обращается к `http://10.0.2.2:3002` (адрес хоста из эмулятора).

Настройка через MCP Settings в приложении:
1. Открыть вкладку **MCP** в нижней панели навигации
2. Добавить сервер: URL `http://10.0.2.2:3002`
3. Убедиться, что сервер активен (статус подключения)

Для **физического устройства**: заменить `10.0.2.2` на IP компьютера в локальной сети.

---

## 7. База данных

SQLite-файл создаётся автоматически при первом запуске по пути `CRM_DB_PATH`.

Таблицы:
- `crm_users` — пользователи (id, chatId, userName)
- `crm_tickets` — тикеты (id, userId, title, description, status, result)

---

## 8. Связанные сервисы

| Сервис | Порт | Описание | Документация |
|--------|:----:|----------|---|
| **ai-gateway** | `8081` | Прокси Ollama → OpenAI-совместимый API | `ai-gateway/SETUP.md` |
| **rag-server** | `3001` | Индексация кодовой базы, семантический поиск | `rag-server/SETUP.md` |
| **crm-server** | `3002` | CRM: пользователи и тикеты поддержки (этот сервис) | `crm-server/SETUP.md` |

Полная локальная среда для Support-агента:

```bash
# Терминал 1 — AI Gateway (LLM)
./gradlew :ai-gateway:run

# Терминал 2 — RAG Server (поиск по коду)
CODE_PATH=/path/to/src ./gradlew :rag-server:run

# Терминал 3 — CRM Server (тикеты)
./gradlew :crm-server:run
```
