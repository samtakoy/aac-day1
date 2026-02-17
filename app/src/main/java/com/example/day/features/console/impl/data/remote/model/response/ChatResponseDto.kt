package com.example.day.features.console.impl.data.remote.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

internal sealed interface ChatResultDto

/**
 * Модель успешного ответа от OpenRouter API
 *
 * @property id Уникальный идентификатор запроса
 * @property model Идентификатор использованной модели
 * @property choices Список вариантов ответа (зависит от типа запроса: streaming/non-streaming/non-chat)
 * @property usage Информация об использовании токенов
 * @property created Timestamp создания (Unix time)
 * @property systemFingerprint System fingerprint для воспроизводимости
 * @property objectType Тип объекта ("chat.completion" или "chat.completion.chunk" для стриминга)
 */
@Serializable
internal data class ChatResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("model")
    val model: String,
    @SerialName("choices")
    val choices: List<ChoiceDto>,
    @SerialName("usage")
    val usage: UsageDto? = null,
    @SerialName("created")
    val created: Long? = null,
    @SerialName("system_fingerprint")
    val systemFingerprint: String? = null,
    @SerialName("object")
    val objectType: String? = null
) : ChatResultDto

/**
 * Вариант ответа от модели
 *
 * @property message Объект сообщения с ролью и содержимым
 * @property finishReason Причина завершения: "stop", "length", "content_filter", "model_error" или null при стриминге
 * @property index Индекс варианта в списке
 * @property logProbs Лог-вероятности для top n токенов
 */
@Serializable
internal data class ChoiceDto(
    @SerialName("message")
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String?,
    @SerialName("index")
    val index: Int? = null,
    @SerialName("logprobs")
    val logProbs: LogProbsDto? = null
)

/**
 * Сообщение от модели
 *
 * @property role Роль: "assistant" или "system" (для не-chat моделей)
 * @property content Текст ответа модели
 * @property reasoning Содержимое поля "reasoning" для моделей с поддержкой extended thinking (например, Claude)
 */
@Serializable
internal data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String,
    @SerialName("reasoning")
    val reasoning: String? = null
)

/**
 * Информация об использовании токенов
 *
 * @property promptTokens Количество токенов в запросе
 * @property completionTokens Количество токенов в ответе
 * @property totalTokens Общее количество использованных токенов
 */
@Serializable
internal data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int
)

/**
 * Лог-вероятности для top n токенов
 *
 * @property content Список лог-вероятностей для каждого токена в контенте
 */
@Serializable
internal data class LogProbsDto(
    @SerialName("content")
    val content: List<LogProbContentDto>? = null
)

/**
 * Лог-вероятность для одного токена
 *
 * @property token Сгенерированный токен
 * @property logprob Логарифм вероятности токена
 * @property bytes Байтовое представление токена (опционально)
 * @property topLogProbs Список top альтернативных токенов
 */
@Serializable
internal data class LogProbContentDto(
    @SerialName("token")
    val token: String,
    @SerialName("logprob")
    val logprob: Double,
    @SerialName("bytes")
    val bytes: List<Int>? = null,
    @SerialName("top_logprobs")
    val topLogProbs: List<TopLogProbDto>? = null
)

/**
 * Top альтернативный токен
 *
 * @property token Альтернативный токен
 * @property logprob Логарифм вероятности
 * @property bytes Байтовое представление (опционально)
 */
@Serializable
internal data class TopLogProbDto(
    @SerialName("token")
    val token: String,
    @SerialName("logprob")
    val logprob: Double,
    @SerialName("bytes")
    val bytes: List<Int>? = null
)

/**
 * Модель ошибки от OpenRouter API
 *
 * @property error Объект с деталями ошибки
 */
@Serializable
internal data class ErrorResponseDto(
    @SerialName("error")
    val error: ErrorDetails
) : ChatResultDto

/**
 * Детали ошибки
 *
 * @property message Сообщение об ошибке
 * @property code Код ошибки (если доступен)
 * @property metadata Дополнительные метаданные ошибки
 * @property param Параметр, вызвавший ошибку (для ошибок валидации)
 * @property type Тип ошибки
 */
@Serializable
internal data class ErrorDetails(
    @SerialName("message")
    val message: String,
    @SerialName("code")
    val code: Int? = null,
    @SerialName("metadata")
    val metadata: String? = null,
    @SerialName("param")
    val param: String? = null,
    @SerialName("type")
    val type: String? = null
)
