package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import javax.inject.Inject

class GenerateProfileAvatarUseCase @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase,
    private val getFactsByProfile: GetFactsByUserProfileUseCase,
    private val updateAvatar: UpdateProfileAvatarUseCase,
) {
    suspend operator fun invoke(model: ModelSettings): Result<Pair<String, String>> {
        val facts = getFactsByProfile()
        val prompt = buildAvatarPrompt(facts)
        val avatarResult = llmRequestUseCase.exec(
            modelSettings = model,
            systemPrompt = "",
            messages = emptyList(),
            prompt = AContextMessage(AContextMessage.Role.USER, prompt),
            tools = null
        ).onFailure {
            return Result.failure(it)
        }
        val avatarText = avatarResult.getOrThrow().getContent()
        return updateAvatar.update(avatarText).map {
            Pair(avatarText, prompt)
        }
    }

    private fun buildAvatarPrompt(facts: List<LongTermMemoryFact>): String {
        val factsSection = if (facts.isEmpty()) {
            "Факты о пользователе отсутствуют."
        } else {
            facts.joinToString("\n") { "- ${it.category}: ${it.fact}" }
        }
        val count = 10
        return """
Действуй как художник портретист. Создай квадратный портрет-аватар размером ${count}x${count} символов, используя только эмодзи-квадраты.
Палитра:
Фон: ⬜ (белый квадрат)
🟩 (Зеленый) — используй для фона или позитивных элементов.
🟥 (Красный) — используй для акцентов или ярких эмоций.
⬛ (Черный) — для контуров и рта.
🟨 (Желтый) — используй для лица.
🟦 (Синий) — для глаз.
Персонаж:
Смайлик, выражающий эмоцию на основе фактов о персонаже:
$factsSection

Технические правила:
Нарисуй лицо.
ВАЖНО: Ровно ${count} строк по ${count} символов в каждой.
Используй только эмодзи-квадраты, чтобы ширина была идеально ровной.

Визуальный ориентир:
Используй всё пространство ${count}x${count}, не оставляй пустых белых углов, если это не обосновано фоном.
Рисуй крупным планом: 2 глаза должно быть, рот — широкая линия.
Используй 🟩 и 🟥 для передачи атмосферы (например, красный фон для ярости или зеленый для спокойствия), а не просто как отдельные точки.

Перед выводом пересчитай, чтобы в каждой строке было ровно ${count} квадратов.
Возвращай ТОЛЬКО текст аватара (${count} строк × ${count} символов), без пояснений и оформления.
        """.trimIndent()
    }
}
