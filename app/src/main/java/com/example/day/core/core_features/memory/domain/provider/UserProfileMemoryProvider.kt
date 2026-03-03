package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.usecase.GetCurrentUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByUserProfileUseCase
import javax.inject.Inject

class UserProfileMemoryProvider @Inject constructor(
    private val getCurrentUserProfile: GetCurrentUserProfileUseCase,
    private val getFactsByUserProfile: GetFactsByUserProfileUseCase
) : MemoryProvider {

    override suspend fun getMemoryContext(): List<AContextMessage> {
        val profile = getCurrentUserProfile() ?: return emptyList()
        val facts = getFactsByUserProfile()
        if (facts.isEmpty()) return emptyList()

        val content = buildString {
            appendLine("## Профиль пользователя: ${profile.title}")
            facts.groupBy { it.category }.forEach { (category, categoryFacts) ->
                appendLine("### $category")
                categoryFacts.forEach { fact ->
                    appendLine("- [${fact.memoryKey}] ${fact.fact}")
                }
            }
        }
        return listOf(AContextMessage(role = Role.SYSTEM, content = content.trim()))
    }
}
