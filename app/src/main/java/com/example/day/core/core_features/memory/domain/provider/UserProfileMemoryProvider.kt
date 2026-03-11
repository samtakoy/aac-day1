package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactForProfileUseCase
import javax.inject.Inject

class UserProfileMemoryProvider @Inject constructor(
    private val getFactsByUserProfile: GetFactsByUserProfileUseCase
) : MemoryProvider {

    override suspend fun getMemoryContext(): List<AContextMessage> {
        val allFacts = getFactsByUserProfile()
        if (allFacts.isEmpty()) return emptyList()

        val content = buildString {
            // 1. Блок "О пользователе"
            val userFacts = allFacts.filter { it.memoryKey.equals(UpsertFactForProfileUseCase.DEFAULT_PROFILE_KEY, ignoreCase = true) }
            if (userFacts.isNotEmpty()) {
                appendLine("## Факты о пользователе:")
                userFacts.groupBy { it.category }.forEach { (category, categoryFacts) ->
                    appendLine("### ${category}: ${categoryFacts.joinToString(", ") { it.fact }}")
                }
                appendLine() // Отступ между блоками
            }

            // 2. Блоки об остальных вещах
            allFacts.filter { !it.memoryKey.equals(UpsertFactForProfileUseCase.DEFAULT_PROFILE_KEY, ignoreCase = true) }
                .groupBy { it.memoryKey }
                .forEach { (key, otherFacts) ->
                    appendLine("## Факты о ${key}:")
                    otherFacts.groupBy { it.category }.forEach { (category, categoryFacts) ->
                        appendLine("### ${category}: ${categoryFacts.joinToString(", ") { it.fact }}")
                    }
                    appendLine()
                }
        }
        return listOf(AContextMessage(role = AContextMessage.Role.SYSTEM, content = content.trim()))
    }
}
