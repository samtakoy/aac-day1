package com.example.day.core.core_features.agent.domain.model

/**
 * Параметры контекста для управления сжатием.
 * 
 * @property msgLimit сколько пар сообщений (вопрос/ответ) хранить как есть без компактизации
 * @property extraLimit сколько пар сообщений сверх [msgLimit] надо накопить чтобы произошло сжатие
 * @property strategy стратегия сжатия (по умолчанию "summarization")
 */
data class ContextParameters(
    val msgLimit: Int = AContext.DEFAULT_MSG_LIMIT,
    val extraLimit: Int = AContext.DEFAULT_EXTRA_LIMIT,
    val strategy: String = AContext.DEFAULT_STRATEGY
)
