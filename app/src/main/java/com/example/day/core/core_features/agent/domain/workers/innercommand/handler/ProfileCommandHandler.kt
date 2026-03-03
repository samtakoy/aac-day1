package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.AIAgentFactory
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.getContent
import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import com.example.day.core.core_features.memory.domain.usecase.BindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.CreateUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GenerateProfileAvatarUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetCurrentUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.GetFactsByUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.RemoveUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UnbindUserProfileUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpdateProfileAvatarUseCase
import com.example.day.core.core_features.memory.domain.usecase.UpsertFactForProfileUseCase
import javax.inject.Inject
import kotlin.compareTo

/**
 * Handles all @@talk(profile ...) commands.
 *
 * Supported sub-commands:
 *   --create NAME         create a new profile
 *   --remove NAME         delete a profile by name
 *   --bind NAME           bind profile to current user
 *   --unbind              unbind current profile from user
 *   --list                list all profiles (marks current with ✓)
 *   --show_facts          show all facts of the bound profile
 *   --add_fact key:cat:text  add a fact to the bound profile
 *   --avatar reset        clear the text avatar
 *   --avatar generate     generate avatar via LLM (20x20 emoji art)
 *   --avatar show         display current avatar
 */
class ProfileCommandHandler @Inject constructor(
    private val createProfile: CreateUserProfileUseCase,
    private val removeProfile: RemoveUserProfileUseCase,
    private val bindProfile: BindUserProfileUseCase,
    private val unbindProfile: UnbindUserProfileUseCase,
    private val getCurrentProfile: GetCurrentUserProfileUseCase,
    private val getFactsByProfile: GetFactsByUserProfileUseCase,
    private val generateAvatar: GenerateProfileAvatarUseCase,
    private val upsertFact: UpsertFactForProfileUseCase,
    private val updateAvatar: UpdateProfileAvatarUseCase,
    private val profileRepository: UserProfileRepository
) : CommandHandler {

    override val commandName = "profile"

    override suspend fun handle(params: List<Pair<String, String?>>, chat: Chat): CommandResult {
        val paramsMap = params.toMap()
        return when {
            "create" in paramsMap -> handleCreate(paramsMap["create"])
            "remove" in paramsMap -> handleRemove(paramsMap["remove"])
            "bind" in paramsMap   -> handleBind(paramsMap["bind"])
            "unbind" in paramsMap -> handleUnbind()
            "list" in paramsMap   -> handleList()
            "show_facts" in paramsMap -> handleShowFacts()
            "add_fact" in paramsMap   -> handleAddFact(paramsMap["add_fact"])
            "avatar" in paramsMap     -> handleAvatar(paramsMap["avatar"], chat)
            else -> CommandResult.Error(
                "Неизвестная команда profile.\n" +
                "Доступные: --create NAME | --remove NAME | --bind NAME | --unbind | --list | " +
                "--show_facts | --add_fact category:текст | --avatar [reset|generate|show]"
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

    private suspend fun handleList(): CommandResult {
        val all = profileRepository.getAllProfiles()
        if (all.isEmpty()) {
            return CommandResult.Success("Профилей нет. Создайте: @@talk(profile --create NAME)")
        }
        val current = getCurrentProfile()
        val text = buildString {
            appendLine("Профили (${all.size}):")
            all.forEach { profile ->
                val marker = if (profile.id == current?.id) " ✓" else ""
                appendLine("  • ${profile.title}$marker")
            }
        }.trim()
        return CommandResult.Success(text)
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
            return CommandResult.Error("Укажите факт: @@talk(profile --add_fact category:текст)")
        }
        val parts = rawFact.split(":", limit = 3).mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
        if (parts.size != 2 && parts.size != 3) {
            return CommandResult.Error("Укажите факт: @@talk(profile --add_fact category:текст)")
        }

        return when (parts.size) {
            2 -> {
                upsertFact("", parts[0], parts[1]).fold(
                    onSuccess = { CommandResult.Success("Факт сохранён: $rawFact") },
                    onFailure = { CommandResult.Error(it.message ?: "Ошибка сохранения факта") }
                )
            }
            3 -> {
                upsertFact(parts[0], parts[1], parts[2]).fold(
                    onSuccess = { CommandResult.Success("Факт сохранён: $rawFact") },
                    onFailure = { CommandResult.Error(it.message ?: "Ошибка сохранения факта") }
                )
            }
            else -> {
                CommandResult.Error("Неверный формат. Ожидается: category:текст_факта или сущность:category:текст_факта")
            }
        }
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
        val (avatarText, prompt) = generateAvatar(chat.settings.model).getOrElse {
            return CommandResult.Error("Ошибка генерации аватара: ${it.message}")
        }
        return CommandResult.Success("Аватар сгенерирован и сохранён:\n\n$avatarText\n\n---\nИспользован промпт:\n$prompt")
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
            // 1. Группируем по сущности (User, Dog, и т.д.)
            facts.groupBy { it.memoryKey }.forEach { (entity, entityFacts) ->
                appendLine("\n[$entity]")

                // 2. Внутри сущности группируем по категориям
                entityFacts.groupBy { it.category }.forEach { (category, categoryFacts) ->
                    val allFactsInCategory = categoryFacts.joinToString(", ") { it.fact }
                    appendLine("  $category: $allFactsInCategory")
                }
            }
        }.trim()
    }

    companion object {
        private const val AGENT_NAME = "talk_agent"
    }
}
