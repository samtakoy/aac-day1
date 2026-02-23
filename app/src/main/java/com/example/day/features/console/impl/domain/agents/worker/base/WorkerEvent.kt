package com.example.day.features.console.impl.domain.agents.worker.base

import com.example.day.core.core_features.llm.domain.model.ModelResult

internal sealed interface WorkerEvent {
    /** Агент отправляет сообщение пользователю  в чат (говорит) */
    class Speech(val text: String) : WorkerEvent
    /** Агент уведомляет, что будет делать запрос - отправляет информационно перед каждым запросо к Llm */
    object RequestStart : WorkerEvent
    /** Агент уведомляет что получил успешный ответ на запрос */
    class  RequestSuccess(val result: ModelResult.Success) : WorkerEvent
    /** Агент уведомляет что запрос к llm вернул ошибку */
    class RequestError(val text: String) : WorkerEvent
}