package com.example.day.features.console.impl.domain.model

data class ModelRequest(
    val model: String,
    val messages: List<Message>,
    val responseFormat: ResponseFormat,
    val stream: Boolean? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val stop: List<String>? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val seed: Int? = null,
    val logProbs: Boolean? = null,
    val topLogProbs: Int? = null
) {
    /**
     * Параметр response_format радикально меняет то, как модель «упаковывает» свои мысли.
     * */
    enum class ResponseFormat {
        /** Обычный текст */
        None,
        /** Модель понимает, что нужно выдать код, но схему придумывает сама.
         *  Вы получите валидный JSON, но ключи в нем могут меняться от запроса к запросу
         *  (например, weight вместо mass) */
        JsonObject,
        /** Это самый строгий вариант. Вы заранее передаете «чертеж» (схему),
         *  и модель обязана в него вписаться.
         *  Если вы указали, что вес должен быть числом, модель не сможет написать «около 150г» текстом.
         *  Гарантия: 100% совместимость с вашим программным кодом.
         *  Strict Mode: Если включен strict: true, модель даже не начнет генерацию, если не уверена,
         *  что сможет заполнить все обязательные поля.
         * */
        JsonSchema
    }

    enum class Role {
        System,
        Assistant,
        User
    }

    data class Message(
        val role: Role,
        val content: String,
        val thinking: String?,
        val cachePrompt: Boolean
    )
}

