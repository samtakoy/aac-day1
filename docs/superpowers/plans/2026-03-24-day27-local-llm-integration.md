# Day 27: Local LLM Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Ollama local LLM into the Android app via a new `ai-gateway` Ktor server, with per-chat `isLocal` toggle and global server URL setting.

**Architecture:** Android picks `LocalLlmApiImpl` or `RemoteLlmApiImpl` based on `ModelRequest.isLocal`. `LocalLlmApiImpl` posts to `ai-gateway` (Ktor 3.2.3) which forwards to Ollama using the OpenAI-compatible `/v1/chat/completions` endpoint. Shared DTOs live in `shared/simple-chat-api` (plain JVM module used by both `app` and `ai-gateway`).

**Tech Stack:** Kotlin, Ktor 3.2.3 (server), Ktor client (Android), kotlinx.serialization, Dagger (no Hilt), DataStore Preferences, Room (no migration needed), Jetpack Compose.

---

## File Map

### New files
| File | Purpose |
|------|---------|
| `shared/simple-chat-api/build.gradle.kts` | JVM module declaration |
| `shared/simple-chat-api/src/main/kotlin/com/example/day/shared/dto/OpenAiDtos.kt` | OpenAI-compatible DTOs (5 data classes) |
| `ai-gateway/build.gradle.kts` | Ktor server module |
| `ai-gateway/Dockerfile` | Docker image |
| `ai-gateway/src/main/kotlin/com/example/day/aigateway/AiGatewayServer.kt` | `main()` entry point |
| `ai-gateway/src/main/kotlin/com/example/day/aigateway/config/AiGatewayConfig.kt` | Env-based config |
| `ai-gateway/src/main/kotlin/com/example/day/aigateway/llm/LlmProvider.kt` | Provider interface |
| `ai-gateway/src/main/kotlin/com/example/day/aigateway/llm/OllamaProvider.kt` | Ollama HTTP client |
| `ai-gateway/src/main/kotlin/com/example/day/aigateway/llm/LlmRouter.kt` | Routes to OllamaProvider |
| `ai-gateway/src/main/kotlin/com/example/day/aigateway/api/routes/ChatRoutes.kt` | POST /v1/chat/completions, GET /v1/models |
| `ai-gateway/src/test/kotlin/com/example/day/aigateway/ChatRoutesTest.kt` | Ktor testApplication test |
| `app/src/main/java/com/example/day/core/app_settings/AppSettings.kt` | DataStore wrapper for localServerUrl |
| `app/src/main/java/com/example/day/app/di/AppSettingsModule.kt` | @Provides @Singleton AppSettings |
| `app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApi.kt` | Interface: sendRequest(ChatCompletionRequest, serverUrl) |
| `app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApiImpl.kt` | HTTP POST to ai-gateway |
| `app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt` | ModelRequest → ChatCompletionRequest |
| `app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt` | ChatCompletionResponse → ModelResult |
| `app/src/test/java/com/example/day/core/core_features/llm/OpenAiMappersTest.kt` | Unit tests for both mappers |
| `app/src/test/java/com/example/day/core/core_features/llm/ModelSettingsSerializationTest.kt` | Verify isLocal round-trips through JSON |

### Modified files
| File | Change |
|------|--------|
| `settings.gradle.kts` | Add `:shared:simple-chat-api`, `:ai-gateway` |
| `app/build.gradle.kts` | Add `project(":shared:simple-chat-api")` dependency |
| `app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelSettings.kt` | Add `isLocal: Boolean = false` |
| `app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelRequest.kt` | Add `isLocal: Boolean = false` |
| `app/src/main/java/com/example/day/core/core_features/llm/data/local/model/ModelSettingsEntity.kt` | Add `isLocal: Boolean = false` |
| `app/src/main/java/com/example/day/core/core_features/llm/data/local/mapper/ModelSettingsMapper.kt` | Map `isLocal` in toDomain/toEntity |
| `app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRequestUseCaseImpl.kt` | Pass `isLocal = modelSettings.isLocal` |
| `app/src/main/java/com/example/day/core/core_features/llm/data/LlmRepositoryImpl.kt` | Add local branch with mappers + AppSettings |
| `app/src/main/java/com/example/day/core/core_features/llm/di/LlmCoreFeatureModule.kt` | Bind LocalLlmApi |
| `app/src/main/java/com/example/day/app/di/AppComponent.kt` | Add AppSettingsModule, expose AppSettings |
| `app/src/main/java/com/example/day/features/console/impl/ui/components/ModelSettingsView.kt` | Add isLocal Switch |
| `docker-compose.yml` | Add ai-gateway service |

---

## Task 1: shared/simple-chat-api Module

**Files:**
- Create: `shared/simple-chat-api/build.gradle.kts`
- Create: `shared/simple-chat-api/src/main/kotlin/com/example/day/shared/dto/OpenAiDtos.kt`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create shared module directory structure**

```bash
mkdir -p shared/simple-chat-api/src/main/kotlin/com/example/day/shared/dto
mkdir -p shared/simple-chat-api/src/test/kotlin/com/example/day/shared/dto
```

- [ ] **Step 2: Create build.gradle.kts for shared module**

```kotlin
// shared/simple-chat-api/build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example.day"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

- [ ] **Step 3: Create OpenAiDtos.kt**

```kotlin
// shared/simple-chat-api/src/main/kotlin/com/example/day/shared/dto/OpenAiDtos.kt
package com.example.day.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val model: String = "",
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)
```

- [ ] **Step 4: Add includes to settings.gradle.kts**

Open `settings.gradle.kts` and add after `include(":rag-server")`:
```kotlin
include(":shared:simple-chat-api")
include(":ai-gateway")
```

- [ ] **Step 5: Add shared dependency to app/build.gradle.kts**

In `app/build.gradle.kts` dependencies block, add:
```kotlin
implementation(project(":shared:simple-chat-api"))
```

- [ ] **Step 6: Verify it compiles**

```bash
./gradlew :shared:simple-chat-api:compileKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add shared/simple-chat-api/ settings.gradle.kts app/build.gradle.kts
git commit -m "feat: add shared/simple-chat-api module with OpenAI-compatible DTOs"
```

---

## Task 2: ai-gateway Ktor Server

**Files:**
- Create: `ai-gateway/build.gradle.kts`, `ai-gateway/Dockerfile`
- Create: all `ai-gateway/src/main/kotlin/...` files
- Create: `ai-gateway/src/test/kotlin/.../ChatRoutesTest.kt`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p ai-gateway/src/main/kotlin/com/example/day/aigateway/config
mkdir -p ai-gateway/src/main/kotlin/com/example/day/aigateway/llm
mkdir -p ai-gateway/src/main/kotlin/com/example/day/aigateway/api/routes
mkdir -p ai-gateway/src/test/kotlin/com/example/day/aigateway
```

- [ ] **Step 2: Create build.gradle.kts**

```kotlin
// ai-gateway/build.gradle.kts
import org.gradle.jvm.tasks.Jar

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.example.day"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared:simple-chat-api"))
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation("io.ktor:ktor-server-test-host:3.2.3")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.day.aigateway.AiGatewayServerKt")
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("ai-gateway")
    archiveVersion.set("")
    manifest { attributes["Main-Class"] = "com.example.day.aigateway.AiGatewayServerKt" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
```

- [ ] **Step 3: Write failing test first**

```kotlin
// ai-gateway/src/test/kotlin/com/example/day/aigateway/ChatRoutesTest.kt
package com.example.day.aigateway

import com.example.day.aigateway.api.routes.ChatRoutes
import com.example.day.aigateway.llm.LlmProvider
import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import com.example.day.shared.dto.Choice
import com.example.day.shared.dto.Message
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatRoutesTest {

    private val fakeProvider = object : LlmProvider {
        override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse =
            ChatCompletionResponse(
                id = "test-id",
                model = request.model,
                choices = listOf(Choice(message = Message("assistant", "Hello!"), finishReason = "stop"))
            )
    }

    @Test
    fun `POST chat completions returns 200 with assistant message`() = testApplication {
        application {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            ChatRoutes(fakeProvider).apply { configureRoutes() }
        }
        val response = client.post("/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"llama3","messages":[{"role":"user","content":"Hi"}]}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Hello!"))
    }

    @Test
    fun `GET models returns list`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            ChatRoutes(fakeProvider).apply { configureRoutes() }
        }
        val response = client.get("/v1/models")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("llama3"))
    }
}
```

- [ ] **Step 4: Run test — expect FAIL (classes don't exist yet)**

```bash
./gradlew :ai-gateway:test
```
Expected: compilation error (classes not found)

- [ ] **Step 5: Create AiGatewayConfig.kt**

```kotlin
// ai-gateway/src/main/kotlin/com/example/day/aigateway/config/AiGatewayConfig.kt
package com.example.day.aigateway.config

data class AiGatewayConfig(
    val ollamaUrl: String = System.getenv("OLLAMA_URL") ?: "http://localhost:11434",
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8081
)
```

- [ ] **Step 6: Create LlmProvider.kt**

```kotlin
// ai-gateway/src/main/kotlin/com/example/day/aigateway/llm/LlmProvider.kt
package com.example.day.aigateway.llm

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse

interface LlmProvider {
    suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse
}
```

- [ ] **Step 7: Create OllamaProvider.kt**

```kotlin
// ai-gateway/src/main/kotlin/com/example/day/aigateway/llm/OllamaProvider.kt
package com.example.day.aigateway.llm

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class OllamaProvider(
    private val client: HttpClient,
    private val ollamaUrl: String
) : LlmProvider {
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse {
        val response = client.post("$ollamaUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error("Ollama error ${response.status}")
        }
        return response.body()
    }
}
```

- [ ] **Step 8: Create LlmRouter.kt**

```kotlin
// ai-gateway/src/main/kotlin/com/example/day/aigateway/llm/LlmRouter.kt
package com.example.day.aigateway.llm

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse

// MVP: always routes to Ollama. Future: route by model prefix.
class LlmRouter(private val ollamaProvider: LlmProvider) : LlmProvider {
    override suspend fun chat(request: ChatCompletionRequest): ChatCompletionResponse =
        ollamaProvider.chat(request)
}
```

- [ ] **Step 9: Create ChatRoutes.kt**

```kotlin
// ai-gateway/src/main/kotlin/com/example/day/aigateway/api/routes/ChatRoutes.kt
package com.example.day.aigateway.api.routes

import com.example.day.aigateway.llm.LlmProvider
import com.example.day.shared.dto.ChatCompletionRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class ChatRoutes(private val llmProvider: LlmProvider) {
    fun Application.configureRoutes() {
        routing {
            post("/v1/chat/completions") {
                try {
                    val request = call.receive<ChatCompletionRequest>()
                    val response = llmProvider.chat(request)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadGateway, "ai-gateway error: ${e.message}")
                }
            }
            get("/v1/models") {
                call.respond(mapOf("data" to listOf("llama3", "mistral", "qwen2.5:7b")))
            }
        }
    }
}
```

- [ ] **Step 10: Create AiGatewayServer.kt**

```kotlin
// ai-gateway/src/main/kotlin/com/example/day/aigateway/AiGatewayServer.kt
package com.example.day.aigateway

import com.example.day.aigateway.api.routes.ChatRoutes
import com.example.day.aigateway.config.AiGatewayConfig
import com.example.day.aigateway.llm.LlmRouter
import com.example.day.aigateway.llm.OllamaProvider
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun main() {
    val config = AiGatewayConfig()
    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    val ollamaProvider = OllamaProvider(httpClient, config.ollamaUrl)
    val router = LlmRouter(ollamaProvider)
    val chatRoutes = ChatRoutes(router)

    embeddedServer(Netty, port = config.port) {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        with(chatRoutes) { configureRoutes() }
    }.start(wait = true)
}
```

- [ ] **Step 11: Run tests — expect PASS**

```bash
./gradlew :ai-gateway:test
```
Expected: BUILD SUCCESSFUL, 2 tests passed

- [ ] **Step 12: Create Dockerfile**

```dockerfile
# ai-gateway/Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY ai-gateway/build/libs/ai-gateway.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
```

- [ ] **Step 13: Commit**

```bash
git add ai-gateway/
git commit -m "feat: add ai-gateway Ktor server with OllamaProvider"
```

---

## Task 3: Android — ModelSettings, ModelSettingsEntity, ModelRequest

**Files:**
- Modify: `app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelSettings.kt`
- Modify: `app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelRequest.kt`
- Modify: `app/src/main/java/com/example/day/core/core_features/llm/data/local/model/ModelSettingsEntity.kt`
- Modify: `app/src/main/java/com/example/day/core/core_features/llm/data/local/mapper/ModelSettingsMapper.kt`
- Modify: `app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRequestUseCaseImpl.kt`
- Create: `app/src/test/java/com/example/day/core/core_features/llm/ModelSettingsSerializationTest.kt`

> **Note:** `ModelSettingsEntity` is stored as a JSON blob in Room (column `model_settings_json`). It is NOT a Room entity — **no Room migration is needed**. Old JSON rows deserialize correctly because `isLocal` has `= false` default.

- [ ] **Step 1: Write failing serialization test**

```kotlin
// app/src/test/java/com/example/day/core/core_features/llm/ModelSettingsSerializationTest.kt
package com.example.day.core.core_features.llm

import com.example.day.core.core_features.llm.data.local.mapper.ModelSettingsMapper
import com.example.day.core.core_features.llm.data.local.model.ModelSettingsEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ModelSettingsSerializationTest {

    private val mapper = ModelSettingsMapper()

    @Test
    fun `isLocal=true round-trips through JSON`() {
        val entity = ModelSettingsEntity(name = "llama3", isLocal = true)
        val json = mapper.toJson(entity)
        val decoded = mapper.fromJson(json)
        assertEquals(true, decoded.isLocal)
    }

    @Test
    fun `old JSON without isLocal field deserializes with isLocal=false`() {
        val oldJson = """{"name":"gpt-4","stopSequence":[],"jsonFormat":false}"""
        val decoded = mapper.fromJson(oldJson)
        assertFalse(decoded.isLocal)
    }

    @Test
    fun `isLocal maps through toDomain and toEntity`() {
        val entity = ModelSettingsEntity(name = "mistral", isLocal = true)
        val domain = mapper.toDomain(entity)
        val backToEntity = mapper.toEntity(domain)
        assertEquals(true, backToEntity.isLocal)
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "*.ModelSettingsSerializationTest"
```
Expected: compilation error (`isLocal` doesn't exist yet)

- [ ] **Step 3: Add `isLocal` to ModelSettings.kt**

In `ModelSettings.kt`, add as last field before closing `}`:
```kotlin
    val isLocal: Boolean = false
```

Companion object stays the same (default() returns `ModelSettings(ModelConst.DEFAULT_MODEL)` which will have `isLocal = false`).

- [ ] **Step 4: Add `isLocal` to ModelSettingsEntity.kt**

```kotlin
    val isLocal: Boolean = false
```
Add as last field before closing `}` of the data class.

- [ ] **Step 5: Update ModelSettingsMapper.kt — add isLocal to toDomain and toEntity**

In `toDomain()`, add:
```kotlin
isLocal = entity.isLocal
```
In `toEntity()`, add:
```kotlin
isLocal = domain.isLocal
```
The `toJson()`/`fromJson()` methods use kotlinx.serialization on `ModelSettingsEntity` directly — no changes needed there since the new field has a default.

- [ ] **Step 6: Add `isLocal` to ModelRequest.kt**

In `ModelRequest` data class, add after `val seed: Int? = null`:
```kotlin
    val isLocal: Boolean = false
```

- [ ] **Step 7: Update LlmRequestUseCaseImpl.kt — pass isLocal**

In the `ModelRequest(...)` constructor call, add:
```kotlin
    isLocal = modelSettings.isLocal,
```
(Add it anywhere in the constructor call — after `seed = modelSettings.seed` is natural.)

- [ ] **Step 8: Run test — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "*.ModelSettingsSerializationTest"
```
Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 9: Verify full app compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelSettings.kt \
        app/src/main/java/com/example/day/core/core_features/llm/domain/model/ModelRequest.kt \
        app/src/main/java/com/example/day/core/core_features/llm/data/local/model/ModelSettingsEntity.kt \
        app/src/main/java/com/example/day/core/core_features/llm/data/local/mapper/ModelSettingsMapper.kt \
        app/src/main/java/com/example/day/core/core_features/llm/domain/LlmRequestUseCaseImpl.kt \
        app/src/test/java/com/example/day/core/core_features/llm/ModelSettingsSerializationTest.kt
git commit -m "feat: add isLocal flag to ModelSettings, ModelRequest, ModelSettingsEntity"
```

---

## Task 4: Android — AppSettings + DI

**Files:**
- Create: `app/src/main/java/com/example/day/core/app_settings/AppSettings.kt`
- Create: `app/src/main/java/com/example/day/app/di/AppSettingsModule.kt`
- Modify: `app/src/main/java/com/example/day/app/di/AppComponent.kt`

> `AppSettings` uses DataStore name `"app_settings"` (does not conflict with `"mcp_secrets"` used by SecretsVault).

- [ ] **Step 1: Create AppSettings.kt**

```kotlin
// app/src/main/java/com/example/day/core/app_settings/AppSettings.kt
package com.example.day.core.app_settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(private val context: Context) {

    val localServerUrl: Flow<String> = context.appSettingsDataStore.data.map { prefs ->
        prefs[LOCAL_SERVER_URL_KEY] ?: DEFAULT_LOCAL_SERVER_URL
    }

    suspend fun setLocalServerUrl(url: String) {
        context.appSettingsDataStore.edit { it[LOCAL_SERVER_URL_KEY] = url }
    }

    companion object {
        private val LOCAL_SERVER_URL_KEY = stringPreferencesKey("local_server_url")
        const val DEFAULT_LOCAL_SERVER_URL = "http://10.0.2.2:8081"
    }
}
```

- [ ] **Step 2: Create AppSettingsModule.kt**

```kotlin
// app/src/main/java/com/example/day/app/di/AppSettingsModule.kt
package com.example.day.app.di

import android.content.Context
import com.example.day.core.app_settings.AppSettings
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppSettingsModule {
    @Provides
    @Singleton
    fun provideAppSettings(context: Context): AppSettings = AppSettings(context)
}
```

- [ ] **Step 3: Add AppSettingsModule to AppComponent**

In `AppComponent.kt`, in the `@Component(modules = [...])` annotation, add `AppSettingsModule::class` to the list.

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/day/core/app_settings/AppSettings.kt \
        app/src/main/java/com/example/day/app/di/AppSettingsModule.kt \
        app/src/main/java/com/example/day/app/di/AppComponent.kt
git commit -m "feat: add AppSettings DataStore for localServerUrl"
```

---

## Task 5: Android — Local LLM API + Mappers + LlmRepositoryImpl + DI

**Files:**
- Create: `app/src/main/java/.../llm/data/remote/LocalLlmApi.kt`
- Create: `app/src/main/java/.../llm/data/remote/LocalLlmApiImpl.kt`
- Create: `app/src/main/java/.../llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt`
- Create: `app/src/main/java/.../llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt`
- Modify: `app/src/main/java/.../llm/data/LlmRepositoryImpl.kt`
- Modify: `app/src/main/java/.../llm/di/LlmCoreFeatureModule.kt`
- Create: `app/src/test/java/.../llm/OpenAiMappersTest.kt`

**Base package path:** `app/src/main/java/com/example/day/core/core_features/llm`

- [ ] **Step 1: Write failing mapper tests**

```kotlin
// app/src/test/java/com/example/day/core/core_features/llm/OpenAiMappersTest.kt
package com.example.day.core.core_features.llm

import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.shared.dto.ChatCompletionResponse
import com.example.day.shared.dto.Choice
import com.example.day.shared.dto.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OpenAiMappersTest {

    private val requestMapper = OpenAiModelRequestMapperImpl()
    private val responseMapper = OpenAiModelResponseMapperImpl()

    @Test
    fun `request mapper maps model and messages`() {
        val request = ModelRequest(
            model = "llama3",
            messages = listOf(
                ModelRequest.Message(ModelRequest.Role.System, "You are helpful"),
                ModelRequest.Message(ModelRequest.Role.User, "Hello")
            ),
            responseFormat = ModelRequest.ResponseFormat.None,
            temperature = 0.7,
            isLocal = true
        )
        val dto = requestMapper.toDto(request)
        assertEquals("llama3", dto.model)
        assertEquals(2, dto.messages.size)
        assertEquals("system", dto.messages[0].role)
        assertEquals("You are helpful", dto.messages[0].content)
        assertEquals("user", dto.messages[1].role)
        assertEquals(0.7, dto.temperature)
    }

    @Test
    fun `response mapper maps assistant message to ModelResult Success`() {
        val response = ChatCompletionResponse(
            id = "resp-123",
            model = "llama3",
            choices = listOf(
                Choice(message = Message("assistant", "Hi there!"), finishReason = "stop")
            )
        )
        val result = responseMapper.toDomain(response)
        assertIs<ModelResult.Success>(result)
        assertEquals("resp-123", result.id)
        assertEquals("Hi there!", result.choices.first().message.content)
    }

    @Test
    fun `response mapper returns RuntimeError when choices empty`() {
        val response = ChatCompletionResponse(id = "x", choices = emptyList())
        val result = responseMapper.toDomain(response)
        assertIs<ModelResult.RuntimeError>(result)
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "*.OpenAiMappersTest"
```
Expected: compilation error

- [ ] **Step 3: Create LocalLlmApi.kt**

```kotlin
// app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApi.kt
package com.example.day.core.core_features.llm.data.remote

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse

internal interface LocalLlmApi {
    suspend fun sendRequest(request: ChatCompletionRequest, serverUrl: String): ChatCompletionResponse
}
```

- [ ] **Step 4: Create OpenAiModelRequestMapperImpl.kt**

```kotlin
// app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelRequestMapperImpl.kt
package com.example.day.core.core_features.llm.data.remote.mappers

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.Message
import javax.inject.Inject

internal class OpenAiModelRequestMapperImpl @Inject constructor() {
    fun toDto(request: ModelRequest): ChatCompletionRequest = ChatCompletionRequest(
        model = request.model,
        messages = request.messages.map { msg ->
            Message(
                role = when (msg.role) {
                    ModelRequest.Role.System -> "system"
                    ModelRequest.Role.User -> "user"
                    ModelRequest.Role.Assistant -> "assistant"
                    ModelRequest.Role.Tool -> "tool"
                },
                content = msg.content
            )
        },
        temperature = request.temperature,
        maxTokens = request.maxTokens ?: request.maxCompletionTokens,
        stream = false
    )
}
```

- [ ] **Step 5: Create OpenAiModelResponseMapperImpl.kt**

```kotlin
// app/src/main/java/com/example/day/core/core_features/llm/data/remote/mappers/OpenAiModelResponseMapperImpl.kt
package com.example.day.core.core_features.llm.data.remote.mappers

import com.example.day.core.core_features.llm.domain.model.ModelResult
import com.example.day.shared.dto.ChatCompletionResponse
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

internal class OpenAiModelResponseMapperImpl @Inject constructor() {
    fun toDomain(response: ChatCompletionResponse): ModelResult {
        val choice = response.choices.firstOrNull()
            ?: return ModelResult.RuntimeError("No choices in response")
        return ModelResult.Success(
            id = response.id,
            model = response.model,
            choices = persistentListOf(
                ModelResult.Success.Choice(
                    message = ModelResult.Success.Message(
                        role = choice.message.role,
                        content = choice.message.content,
                        reasoning = null,
                        toolCalls = null
                    ),
                    finishReason = choice.finishReason
                )
            ),
            usage = response.usage?.let { u ->
                ModelResult.Success.Usage(
                    promptTokens = u.promptTokens ?: 0,
                    completionTokens = u.completionTokens ?: 0,
                    totalTokens = u.totalTokens ?: 0,
                    cost = null,
                    costDetails = null
                )
            }
        )
    }
}
```

- [ ] **Step 6: Run tests — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "*.OpenAiMappersTest"
```
Expected: 3 tests passed

- [ ] **Step 7: Create LocalLlmApiImpl.kt**

```kotlin
// app/src/main/java/com/example/day/core/core_features/llm/data/remote/LocalLlmApiImpl.kt
package com.example.day.core.core_features.llm.data.remote

import com.example.day.shared.dto.ChatCompletionRequest
import com.example.day.shared.dto.ChatCompletionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject

internal class LocalLlmApiImpl @Inject constructor(
    private val client: HttpClient
) : LocalLlmApi {
    override suspend fun sendRequest(request: ChatCompletionRequest, serverUrl: String): ChatCompletionResponse {
        val response = client.post("$serverUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            error("ai-gateway error ${response.status}: ${response.bodyAsText()}")
        }
        return response.body()
    }
}
```

- [ ] **Step 8: Update LlmRepositoryImpl.kt**

Replace the entire file content:
```kotlin
package com.example.day.core.core_features.llm.data

import android.util.Log
import com.example.day.BuildConfig
import com.example.day.core.app_settings.AppSettings
import com.example.day.core.core_features.llm.data.remote.LocalLlmApi
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApi
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapper
import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.LlmRepository
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class LlmRepositoryImpl @Inject constructor(
    private val remoteApi: RemoteLlmApi,
    private val remoteRequestMapper: ModelRequestMapper,
    private val remoteResponseMapper: ModelResponseMapper,
    private val localApi: LocalLlmApi,
    private val localRequestMapper: OpenAiModelRequestMapperImpl,
    private val localResponseMapper: OpenAiModelResponseMapperImpl,
    private val appSettings: AppSettings
) : LlmRepository {
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        return try {
            if (request.isLocal) {
                val serverUrl = appSettings.localServerUrl.first()
                val dto = localRequestMapper.toDto(request)
                val response = localApi.sendRequest(dto, serverUrl)
                localResponseMapper.toDomain(response)
            } else {
                val result = remoteApi.sendRequest(
                    request = remoteRequestMapper.toDto(request),
                    apiKey = BuildConfig.LLM_API_KEY
                )
                remoteResponseMapper.toDomain(result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("mytest", e.stackTraceToString())
            ModelResult.RuntimeError(e.stackTraceToString())
        }
    }
}
```

- [ ] **Step 9: Update LlmCoreFeatureModule.kt — add LocalLlmApi binding**

Add one new binding to the existing module:
```kotlin
@Binds
fun bindsLocalApi(impl: LocalLlmApiImpl): LocalLlmApi
```

- [ ] **Step 10: Verify full compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/example/day/core/core_features/llm/data/ \
        app/src/main/java/com/example/day/core/core_features/llm/di/ \
        app/src/test/java/com/example/day/core/core_features/llm/OpenAiMappersTest.kt
git commit -m "feat: add LocalLlmApiImpl, OpenAI mappers, update LlmRepositoryImpl for local routing"
```

---

## Task 6: Android — UI Changes

**Files:**
- Modify: `app/src/main/java/com/example/day/features/console/impl/ui/components/ModelSettingsView.kt`
- Determine where global settings (localServerUrl) are shown — read the existing settings UI to find the right place

> For the global URL setting, read the existing app navigation and settings screens before deciding where to add it. If there is a top-level settings screen, add there. If not, create a simple screen or add to MCP settings as a separate section.

- [ ] **Step 1: Add `isLocal` Switch to ModelSettingsView**

Read `ModelSettingsView.kt` first. Find the existing checkboxes section (e.g., near `jsonFormat` checkbox). Add below it:

```kotlin
// Add isLocal toggle — below the jsonFormat checkbox
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {
    Text(
        text = "Локальная LLM (Ollama)",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f)
    )
    Switch(
        checked = settings.isLocal,
        onCheckedChange = { onSettingsChange(settings.copy(isLocal = it)) }
    )
}
```

The `onSettingsChange` callback follows the existing pattern for checkboxes in `ModelSettingsView`. Check the exact callback signature by reading the file — it likely takes a lambda `(ModelSettings) -> Unit`.

- [ ] **Step 2: Add global URL setting**

Read `AppComponent.kt` and existing navigation to find where to add global settings UI. Options:
- If `UserSettingsFeature` has a settings section → add there
- If MCP settings has an app-level section → add there
- Otherwise create a simple `PreferenceTextField` in an existing settings composable

Add an editable URL field that calls `appSettings.setLocalServerUrl(url)` (collect via ViewModel or scope in the composable).

Pattern (adapt to the actual settings screen structure):
```kotlin
// Collect current URL
val serverUrl by appSettings.localServerUrl.collectAsState(
    initial = AppSettings.DEFAULT_LOCAL_SERVER_URL
)

// Display editable field
OutlinedTextField(
    value = serverUrl,
    onValueChange = { /* call setLocalServerUrl via ViewModel */ },
    label = { Text("URL ai-gateway сервера") },
    placeholder = { Text(AppSettings.DEFAULT_LOCAL_SERVER_URL) },
    modifier = Modifier.fillMaxWidth()
)
```

- [ ] **Step 3: Manual verification**

Build and run the app:
```bash
./gradlew :app:assembleDebug
```

In a chat settings screen:
1. Verify the "Локальная LLM (Ollama)" switch appears
2. Toggle it on → `ModelSettings.isLocal` becomes true
3. Go to global settings → verify URL field shows `http://10.0.2.2:8081`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/day/features/
git commit -m "feat: add isLocal toggle in chat settings and globalServerUrl setting"
```

---

## Task 7: docker-compose

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add ai-gateway service**

In `docker-compose.yml`, add after the `mcp-server` service:

```yaml
  ai-gateway:
    build:
      context: .
      dockerfile: ai-gateway/Dockerfile
    container_name: ai-gateway
    ports:
      - "8081:8081"
    environment:
      - OLLAMA_URL=http://host.docker.internal:11434
      - PORT=8081
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8081/v1/models"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

> Ollama must be running on the host machine at port 11434 (`ollama serve`). `host.docker.internal` resolves to the Docker host on both Mac and Linux with Docker Desktop.

- [ ] **Step 2: Build ai-gateway JAR**

```bash
./gradlew :ai-gateway:jar
```
Expected: `ai-gateway/build/libs/ai-gateway.jar` created

- [ ] **Step 3: Start services**

```bash
docker-compose up ai-gateway --build
```
Expected: container starts, `GET http://localhost:8081/v1/models` returns JSON list

- [ ] **Step 4: Smoke test end-to-end**

With Ollama running and a model pulled (e.g., `ollama pull llama3`):
```bash
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"llama3","messages":[{"role":"user","content":"Say hi"}]}'
```
Expected: JSON response with `choices[0].message.content` containing a greeting.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add ai-gateway to docker-compose"
```

---

## End-to-End Acceptance Criteria

- [ ] Android chat with `isLocal=true` and model `llama3` → gets response from Ollama via ai-gateway
- [ ] Android chat with `isLocal=false` → uses OpenRouter, unchanged behaviour
- [ ] Global server URL can be changed in settings without rebuilding the app
- [ ] `ai-gateway` Docker image builds and starts
- [ ] `curl` smoke test against ai-gateway works with Ollama running
- [ ] All unit tests pass: `./gradlew :app:testDebugUnitTest :ai-gateway:test`
