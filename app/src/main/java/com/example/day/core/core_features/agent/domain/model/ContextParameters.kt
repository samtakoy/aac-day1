package com.example.day.core.core_features.agent.domain.model

/**
 * TODO remove
 * Параметры контекста для управления сжатием.
 * 
 * @property msgLimit сколько пар сообщений (вопрос/ответ) хранить как есть без компактизации
 * @property extraLimit сколько пар сообщений сверх [msgLimit] надо накопить чтобы произошло сжатие
 * @property strategy стратегия сжатия (по умолчанию "summarization")
 */
data class ContextParameters(
    val msgLimit: Int = Int.MAX_VALUE,
    val extraLimit: Int = 0,
    val strategy: String = "summarization"
)
