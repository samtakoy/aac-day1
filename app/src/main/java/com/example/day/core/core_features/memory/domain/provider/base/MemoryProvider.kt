package com.example.day.core.core_features.memory.domain.provider.base

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.PromptMessages

/**
 * UserProfileMemoryProvider: Тянет только LTM (скиллы, имя).
 * TaskWorkingMemoryProvider: Тянет только workingSummary (текущий проект).
 * CompositeMemoryProvider: Собирает данные из нескольких источников.
 */
interface MemoryProvider {
    /**
     * Возвращает список системных сообщений или инструкций,
     * сформированных на основе различных слоев памяти.
     */
    suspend fun getMemoryContext(): List<AContextMessage>

    /**
     * Optionally enriches the user prompt before it is sent to the LLM.
     *
     * @return [PromptMessages] — эфемерный контекст (не сохраняется в историю) + prompt (сохраняется).
     * Default: prompt без изменений, контекст пустой.
     */
    suspend fun appendUserPrompt(prompt: AContextMessage): PromptMessages = PromptMessages(prompt = prompt)
}
