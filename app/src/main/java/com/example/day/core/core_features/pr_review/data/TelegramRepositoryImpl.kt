package com.example.day.core.core_features.pr_review.data

import android.util.Log
import com.example.day.BuildConfig
import com.example.day.core.core_features.pr_review.domain.model.TelegramPrEvent
import com.example.day.core.core_features.pr_review.domain.repository.TelegramRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TelegramRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : TelegramRepository {

    private companion object {
        const val TAG = "TelegramRepo"
    }

    override suspend fun getPrUpdates(offset: Long): Result<List<TelegramPrEvent>> =
        runCatching {
            val url = "https://api.telegram.org/bot${BuildConfig.TELEGRAM_BOT_TOKEN}" +
                "/getUpdates?offset=$offset&limit=10&timeout=0"
            val response: TelegramUpdatesResponse = httpClient.get(url).body()
            val targetChatId = BuildConfig.TELEGRAM_CHAT_ID.toLong()
            Log.d(TAG, "targetChatId=$targetChatId, total updates=${response.result.size}")
            response.result.forEach { u ->
                Log.d(TAG, "  update_id=${u.updateId} chat=${u.message?.chat?.id} text=${u.message?.text?.take(80)}")
            }

            response.result
                .filter { update -> update.message?.chat?.id == targetChatId }
                .mapNotNull { update ->
                    val message = update.message ?: return@mapNotNull null
                    val text = message.text ?: return@mapNotNull null
                    val dto = runCatching {
                        json.decodeFromString<TelegramPrEventDto>(text)
                    }.getOrNull() ?: return@mapNotNull null
                    if (dto.event != "pr_opened") return@mapNotNull null
                    TelegramPrEvent(
                        updateId = update.updateId,
                        prNumber = dto.prNumber,
                        repo = dto.repo,
                        title = dto.title
                    )
                }
        }
}

@Serializable
private data class TelegramUpdatesResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate> = emptyList()
)

@Serializable
private data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,
    val message: TelegramMessage? = null
)

@Serializable
private data class TelegramMessage(
    val chat: TelegramChat,
    val text: String? = null
)

@Serializable
private data class TelegramChat(
    val id: Long
)

@Serializable
private data class TelegramPrEventDto(
    val event: String,
    val repo: String,
    @SerialName("pr_number") val prNumber: Int,
    val title: String
)
