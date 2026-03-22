package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.PromptMessages
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider

class CompositeMemoryProvider(
    private val providers: List<MemoryProvider>
) : MemoryProvider {

    override suspend fun getMemoryContext(): List<AContextMessage> =
        providers.flatMap { it.getMemoryContext() }

    // Каждый провайдер получает текущий prompt и может:
    //   - добавить эфемерный контекст (context)
    //   - модифицировать prompt (например, перевести)
    // Итоговый context = объединение context от всех провайдеров.
    // Итоговый prompt = результат цепочки модификаций.
    override suspend fun appendUserPrompt(prompt: AContextMessage): PromptMessages {
        val allContext = mutableListOf<AContextMessage>()
        var currentPrompt = prompt
        for (provider in providers) {
            val result = provider.appendUserPrompt(currentPrompt)
            allContext.addAll(result.context)
            currentPrompt = result.prompt
        }
        return PromptMessages(context = allContext, prompt = currentPrompt)
    }
}
