package com.example.day.core.core_features.llm.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запрос к OpenRouter Chat API
 *
 * @property model Идентификатор модели для использования
 * @property messages Список сообщений в формате {role, content}
 * @property responseFormat Формат ответа (например, JSON schema)
 * @property stream Включить стриминг ответа
 * @property maxTokens Максимальное количество токенов для генерации
 * @property temperature Sampling temperature (0-2)
 * @property topP Nucleus sampling parameter
 * @property topK Top-k sampling parameter
 * @property stop Массив стоп-последовательностей
 * @property presencePenalty Presence penalty (-2 to 2)
 * @property frequencyPenalty Frequency penalty (-2 to 2)
 * @property seed Seed для воспроизводимых результатов
 * @property logProbs Включить лог-вероятности в ответ
 * @property topLogProbs Количество top logprobs для включения
 * @property transforms Массив трансформаций для применения к ответу модели
 */
@Serializable
class ChatRequestDto(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<MessageDto>,
    @SerialName("response_format")
    val responseFormat: ResponseFormatDto? = null,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("stop")
    val stop: List<String>? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Double? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @SerialName("reasoning")
    val reasoning: Reasoning? = null,
    @SerialName("seed")
    val seed: Int? = null,
    @SerialName("logprobs")
    val logProbs: Boolean? = null,
    @SerialName("top_logprobs")
    val topLogProbs: Int? = null,
    @SerialName("transforms")
    val transforms: List<String>? = null
)

/**
 *        reasoning:
 *           type: object
 *           properties:
 *             effort:
 *               anyOf:
 *                 - type: string
 *                   enum:
 *                     - xhigh
 *                     - high
 *                     - medium
 *                     - low
 *                     - minimal
 *                     - none
 *                   x-speakeasy-unknown-values: allow
 *                 - type: 'null'
 * */
@Serializable
class Reasoning(
    val effort: String?
)

/**
 * Формат ответа для генерации
 *
 * @property type Тип формата ("json_object" или "json_schema")
 * @property jsonSchema Опциональная JSON schema для валидации
 */
@Serializable
class ResponseFormatDto(
    @SerialName("type")
    val type: String,
    @SerialName("json_schema")
    val jsonSchema: JsonSchemaDto? = null
)

/**
 * JSON Schema для валидации ответа
 *
 * @property name Название схемы
 * @property description Описание схемы
 * @property schema JSON schema
 * @property strict Включить strict mode
 */
@Serializable
class JsonSchemaDto(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("schema")
    val schema: String? = null,
    @SerialName("strict")
    val strict: Boolean? = null
)