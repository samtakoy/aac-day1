# Stage 1: Доработки RagServer — search_codebase через PipelineExecutor

## Описание
Переработать MCP-инструмент поиска по кодовой базе: заменить прямой вызов `TwoStageSearchService` на `PipelineExecutor` (как в HTTP `/search` эндпоинте). Убрать из видимости устаревшие инструменты.

## Файлы для изменения

### 1. `rag-server/src/main/kotlin/com/example/day/ragserver/tools/RagTools.kt`

**Что меняется**:
- Функция `registerRagTools()` получает новую сигнатуру: вместо `searchService, db, topK, embeddingProvider, queryOptimizer` — принимает `buildPipeline: (PipelineConfig) -> PipelineExecutor` и `sessionLogger: SessionLogger?`
- Удалить из тела `registerRagTools()`: создание `ContextPacker`, создание `TwoStageSearchService`, вызов `registerSearchCodebaseSmart`
- Вместо этого: вызвать `registerSearchCodebase(server, buildPipeline, sessionLogger)`
- Функции `registerSearchCodebaseFixed`, `registerSearchCodebaseSmart`, `registerGetIndexStatus` — закомментировать объявления целиком (включая тело)

**Новый `registerSearchCodebase(server, buildPipeline, sessionLogger)`**:
- Регистрирует инструмент с именем `RagToolNames.SEARCH_CODEBASE`
- Описание: "Поиск по кодовой базе Android-проекта. Возвращает релевантные фрагменты кода, сгруппированные по классам. Используй для вопросов об архитектуре, реализации классов, use cases, DI-компонентах."
- Входные параметры: `query: String` (обязательный)
- Логика:
  1. `sessionLogger?.logSearchStart(query, null, null)`
  2. `val pipeline = buildPipeline(PipelineConfig())`
  3. `var ctx = pipeline.execute(query)`
  4. Fallback (идентично `/search`): если `ctx.results.isEmpty()` и `pipelineConfig.postRerankThreshold > 0.0` — создать `fallbackPipeline = buildPipeline(pipelineConfig.copy(enableQueryOptimize = false))`, выполнить `fallbackPipeline.execute(ctx.originalQuery)`, если у fallback есть результаты — заменить `ctx`. Примечание: с дефолтным `PipelineConfig()` (`postRerankThreshold = 0.0`) этот блок никогда не выполнится, но он сохраняется для полной идентичности с `/search`
  5. Если `ctx.packed == null || ctx.packed.groups.isEmpty()` — вернуть "НЕДОСТАТОЧНО_КОНТЕКСТА: ..."
  6. `val contextText = ContextFormatter.format(ctx.packed)`
  7. `sessionLogger?.logSearchFinish(ctx.results, droppedResults, contextText)` где `droppedResults = ctx.resultsAfterRerank.drop(ctx.results.size)`
  8. Вернуть `CallToolResult(content = listOf(TextContent(text = contextText)))`

### 2. `rag-server/src/main/kotlin/com/example/day/ragserver/RagServer.kt`

**Что меняется**:
- Найти вызов `registerRagTools(mcpServer, searchService, db, config.searchTopK, embeddingProvider, queryOptimizer, sessionLogger)`
- Заменить на: `registerRagTools(mcpServer, buildPipeline = { config -> buildPipeline(config) }, sessionLogger = sessionLogger)`
- Это возможно т.к. функция `buildPipeline(pipelineConfig: PipelineConfig, ...)` уже определена локально в `main()`

**Важно**: в `buildPipeline()` сейчас принимает `taskState` и `history` — для MCP-инструмента они не нужны (передаются как `null`). Нужно убедиться что лямбда `{ config -> buildPipeline(config) }` компилируется (т.е. `buildPipeline` без `taskState`/`history` использует дефолты `null`).

## Импорты которые понадобятся в RagTools.kt
- `com.example.day.ragserver.pipeline.PipelineConfig`
- `com.example.day.ragserver.pipeline.PipelineExecutor`
- `com.example.day.ragserver.search.context.ContextFormatter`
- `com.example.day.ragserver.logging.SessionLogger`

## Резюме
**Что получим**: единственный зарегистрированный MCP-инструмент `search_codebase`, использующий полный пайплайн (TWO_STAGE поиск → реранк → TopK → форматирование), идентичный по логике HTTP `/search` эндпоинту.

**Критерии успеха**:
- RagServer компилируется и запускается без ошибок
- В списке MCP-инструментов сервера виден только `search_codebase`
- Запрос к `search_codebase` с осмысленным `query` возвращает непустой контекст из кодовой базы
- `search_codebase_smart`, `search_codebase_fixed`, `get_index_status` в списке инструментов отсутствуют
