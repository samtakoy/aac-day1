# Этап 3: Embedding Providers

## Общее описание
Реализация интерфейса `EmbeddingProvider` и двух провайдеров: Ollama (локальный) и OpenRouter (облачный). HTTP-клиент — Ktor Client (уже есть в зависимостях). Выбор провайдера — через `RagConfig.embeddingProvider`.

## Что получим
Замена заглушки на реальные эмбеддинги. После запуска с `EMBEDDING_PROVIDER=ollama` и работающим Ollama — чанки в `code_vectors` содержат реальные векторы размерностью 768 (nomic-embed-text) или 1536 (text-embedding-3-small).

## Критерии успеха
- Запуск с `EMBEDDING_PROVIDER=ollama` и запущенным `ollama serve` — индексация проходит без ошибок
- Размер BLOB в `code_vectors` ≠ 0 (для nomic-embed-text = 768 * 4 = 3072 байта)
- Запуск с `EMBEDDING_PROVIDER=openrouter` и валидным `OPENROUTER_API_KEY` — аналогично работает
- При недоступном провайдере — сервер выдаёт внятную ошибку и не падает молча

---

## Задачи этапа

### 3.1 EmbeddingProvider ✅

Файл: `embedding/EmbeddingProvider.kt`

```
fun interface EmbeddingProvider {
    suspend fun embed(text: String): FloatArray
}
```

`fun interface` (SAM) — позволяет использовать лямбду, в том числе заглушку из Этапа 2 без изменений.

---

### 3.2 Вспомогательные типы для HTTP ✅

Файл: `embedding/EmbeddingProvider.kt` (или отдельный `embedding/EmbeddingModels.kt`)

Два набора kotlinx.serialization data class:

**Для Ollama** (POST `/api/embeddings`):
- Request: `OllamaEmbedRequest(model: String, prompt: String)`
- Response: `OllamaEmbedResponse(embedding: List<Float>)`

**Для OpenRouter** (POST `/v1/embeddings`):
- Request: `OpenRouterEmbedRequest(model: String, input: String)`
- Response: `OpenRouterEmbedResponse(data: List<EmbedData>)`, `EmbedData(embedding: List<Float>)`

Все помечены `@Serializable`.

---

### 3.3 OllamaEmbeddingProvider ✅

Файл: `embedding/OllamaEmbeddingProvider.kt`

`class OllamaEmbeddingProvider(private val baseUrl: String, private val model: String, private val httpClient: HttpClient)`

Метод `embed(text: String): FloatArray`:
1. POST на `$baseUrl/api/embeddings`
2. Body: `OllamaEmbedRequest(model, prompt = text)`
3. Ответ парсится в `OllamaEmbedResponse`
4. Возвращает `response.embedding.toFloatArray()`
5. При HTTP-ошибке — выбрасывает `RuntimeException("Ollama embed failed: ${response.status}")`

Важно: Ollama может работать медленно при большом количестве чанков — это нормально, никаких таймаутов не сокращать.

---

### 3.4 OpenRouterEmbeddingProvider ✅

Файл: `embedding/OpenRouterEmbeddingProvider.kt`

`class OpenRouterEmbeddingProvider(private val apiKey: String, private val model: String, private val httpClient: HttpClient)`

Базовый URL: `https://openrouter.ai/api/v1`

Метод `embed(text: String): FloatArray`:
1. POST на `$BASE_URL/embeddings`
2. Headers: `Authorization: Bearer $apiKey`, `Content-Type: application/json`
3. Body: `OpenRouterEmbedRequest(model, input = text)`
4. Ответ: `OpenRouterEmbedResponse.data[0].embedding.toFloatArray()`
5. При HTTP-ошибке — выбрасывает `RuntimeException`

Поддерживаемые модели через OpenRouter для эмбеддингов:
- `openai/text-embedding-3-small` (1536 dim)
- `openai/text-embedding-3-large` (3072 dim)

---

### 3.5 EmbeddingProviderFactory ✅

Файл: `embedding/EmbeddingProvider.kt` (companion object или top-level function)

`fun createEmbeddingProvider(config: RagConfig, httpClient: HttpClient): EmbeddingProvider`

Логика:
```
when (config.embeddingProvider) {
    "ollama" -> OllamaEmbeddingProvider(config.ollamaBaseUrl, config.embeddingModel, httpClient)
    "openrouter" -> {
        require(config.openRouterApiKey.isNotBlank()) { "OPENROUTER_API_KEY is required" }
        OpenRouterEmbeddingProvider(config.openRouterApiKey, config.embeddingModel, httpClient)
    }
    else -> error("Unknown EMBEDDING_PROVIDER: ${config.embeddingProvider}. Use 'ollama' or 'openrouter'")
}
```

---

### 3.6 Инициализация Ktor HttpClient ✅ (реализована в main(), Этап 4)

Место создания: `RagServer.kt` (main), передаётся в провайдер через dependency injection вручную.

Конфигурация клиента:
- Engine: OkHttp
- Install ContentNegotiation с kotlinx json (ignoreUnknownKeys = true)
- Install Logging (уровень INFO)
- HttpTimeout: `requestTimeoutMillis = 120_000` (2 минуты — Ollama может быть медленным)

Клиент создаётся один раз и передаётся в `createEmbeddingProvider()`.
