package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.usecase.BindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.CreateUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetCurrentUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.RemoveUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UnbindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpdateProfileAvatarUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactForProfileUseCase
import javax.inject.Inject

/**
 * Handles all @@talk(profile ...) commands.
 *
 * Supported sub-commands:
 *   --create NAME         create a new profile
 *   --remove NAME         delete a profile by name
 *   --bind NAME           bind profile to current user
 *   --unbind              unbind current profile from user
 *   --show_facts          show all facts of the bound profile
 *   --add_fact key:cat:text  add a fact to the bound profile
 *   --avatar reset        clear the text avatar
 *   --avatar generate     generate avatar via LLM (30x30 ASCII art)
 *   --avatar show         display current avatar
 */
class ProfileCommandHandler @Inject constructor(
    private val createProfile: CreateUserProfileUseCase,
    private val removeProfile: RemoveUserProfileUseCase,
    private val bindProfile: BindUserProfileUseCase,
    private val unbindProfile: UnbindUserProfileUseCase,
    private val getCurrentProfile: GetCurrentUserProfileUseCase,
    private val getFactsByProfile: GetFactsByUserProfileUseCase,
    private val upsertFact: UpsertFactForProfileUseCase,
    private val updateAvatar: UpdateProfileAvatarUseCase,
    private val aiAgentFactory: AIAgentFactory
) : CommandHandler {

    override val commandName = "profile"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        return when {
            "create" in paramsMap -> handleCreate(paramsMap["create"])
            "remove" in paramsMap -> handleRemove(paramsMap["remove"])
            "bind" in paramsMap   -> handleBind(paramsMap["bind"])
            "unbind" in paramsMap -> handleUnbind()
            "show_facts" in paramsMap -> handleShowFacts()
            "add_fact" in paramsMap   -> handleAddFact(paramsMap["add_fact"])
            "avatar" in paramsMap     -> handleAvatar(paramsMap["avatar"], chat)
            else -> CommandResult.Error(
                "Неизвестная команда profile.\n" +
                "Доступные: --create NAME | --remove NAME | --bind NAME | --unbind | " +
                "--show_facts | --add_fact key:category:текст | --avatar [reset|generate|show]"
            )
        }
    }

    private suspend fun handleCreate(name: String?): CommandResult {
        if (name.isNullOrBlank()) return CommandResult.Error("Укажите имя: @@talk(profile --create NAME)")
        return createProfile(name).fold(
            onSuccess = { CommandResult.Success("Профиль '${it.title}' создан (id=${it.id})") },
            onFailure = { CommandResult.Error(it.message ?: "Ошибка создания профиля") }
        )
    }

    private suspend fun handleRemove(name: String?): CommandResult {
        if (name.isNullOrBlank()) return CommandResult.Error("Укажите имя: @@talk(profile --remove NAME)")
        return removeProfile(name).fold(
            onSuccess = { CommandResult.Success("Профиль '$name' удалён") },
            onFailure = { CommandResult.Error(it.message ?: "Ошибка удаления профиля") }
        )
    }

    private suspend fun handleBind(name: String?): CommandResult {
        if (name.isNullOrBlank()) return CommandResult.Error("Укажите имя: @@talk(profile --bind NAME)")
        return bindProfile(name).fold(
            onSuccess = { CommandResult.Success("Профиль '$name' привязан к пользователю") },
            onFailure = { CommandResult.Error(it.message ?: "Ошибка привязки профиля") }
        )
    }

    private suspend fun handleUnbind(): CommandResult {
        return unbindProfile().fold(
            onSuccess = { CommandResult.Success("Профиль отвязан от пользователя") },
            onFailure = { CommandResult.Error(it.message ?: "Ошибка отвязки профиля") }
        )
    }

    private suspend fun handleShowFacts(): CommandResult {
        val profile = getCurrentProfile()
            ?: return CommandResult.Success("Профиль не привязан. Используйте @@talk(profile --bind NAME)")
        val facts = getFactsByProfile()
        if (facts.isEmpty()) {
            return CommandResult.Success("Профиль '${profile.title}': факты отсутствуют")
        }
        return CommandResult.Success(buildFactsText(profile.title, facts))
    }

    private suspend fun handleAddFact(rawFact: String?): CommandResult {
        if (rawFact.isNullOrBlank()) {
            return CommandResult.Error("Укажите факт: @@talk(profile --add_fact key:category:текст)")
        }
        return upsertFact(rawFact).fold(
            onSuccess = { CommandResult.Success("Факт сохранён: $rawFact") },
            onFailure = { CommandResult.Error(it.message ?: "Ошибка сохранения факта") }
        )
    }

    private suspend fun handleAvatar(subCommand: String?, chat: Chat): CommandResult {
        return when (subCommand?.trim()) {
            "reset" -> {
                updateAvatar.reset().fold(
                    onSuccess = { CommandResult.Success("Аватар сброшен") },
                    onFailure = { CommandResult.Error(it.message ?: "Ошибка сброса аватара") }
                )
            }
            "generate" -> handleAvatarGenerate(chat)
            "show" -> handleAvatarShow()
            else -> CommandResult.Error(
                "Неизвестное действие для avatar. Доступные: reset | generate | show"
            )
        }
    }

    private suspend fun handleAvatarGenerate(chat: Chat): CommandResult {
        val facts = getFactsByProfile()
        val prompt = buildAvatarPrompt(facts)

        val agent = aiAgentFactory.getOrCreate(AGENT_NAME, false, chat)
        val result = agent.process(chat.settings, prompt, null)
            .getOrElse { return CommandResult.Error("Ошибка генерации аватара: ${it.message}") }

        val avatarText = result.responseText
        updateAvatar.update(avatarText).onFailure {
            return CommandResult.Error("Аватар сгенерирован, но не сохранён: ${it.message}")
        }
        return CommandResult.Success("Аватар сгенерирован и сохранён:\n\n$avatarText")
    }

    private suspend fun handleAvatarShow(): CommandResult {
        val profile = getCurrentProfile()
            ?: return CommandResult.Success("Профиль не привязан")
        val avatar = profile.textAvatar
            ?: return CommandResult.Success("Аватар не задан. Используйте @@talk(profile --avatar generate)")
        return CommandResult.Success("Аватар профиля '${profile.title}':\n\n$avatar")
    }

    private fun buildFactsText(profileTitle: String, facts: List<LongTermMemoryFact>): String {
        return buildString {
            appendLine("Профиль: $profileTitle")
            appendLine("Факты (${facts.size}):")
            facts.groupBy { it.category }.forEach { (category, categoryFacts) ->
                appendLine("\n[$category]")
                categoryFacts.forEach { fact ->
                    appendLine("  ${fact.memoryKey}: ${fact.fact}")
                }
            }
        }.trim()
    }

    private fun buildAvatarPrompt(facts: List<LongTermMemoryFact>): String {
        val factsSection = if (facts.isEmpty()) {
            "Факты о пользователе отсутствуют."
        } else {
            facts.joinToString("\n") { "- [${it.category}/${it.memoryKey}] ${it.fact}" }
        }
        val count = 10
        return """
            Действуй как пиксель-арт художник. Создай квадратный портрет-аватар размером ${count}x${count} символов, используя только эмодзи-квадраты.
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
            Ровно ${count} строк по ${count} символов в каждой.
            Используй только эмодзи-квадраты, чтобы ширина была идеально ровной.
            
            Визуальный ориентир:
            Используй всё пространство ${count}x${count}, не оставляй пустых белых углов, если это не обосновано фоном.
            Рисуй крупным планом: глаза должны занимать 2-3 строки, рот — широкую линию.
            Используй 🟩 и 🟥 для передачи атмосферы (например, красный фон для ярости или зеленый для спокойствия), а не просто как отдельные точки.
            
            Перед выводом пересчитай, чтобы в каждой строке было ровно ${count} квадратов.
            Возвращай ТОЛЬКО текст аватара (${count} строк × ${count} символов), без пояснений и оформления.
        """.trimIndent()
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
