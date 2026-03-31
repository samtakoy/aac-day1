package com.example.day.core.core_features.pr_review.domain.worker

import com.example.day.core.core_features.agent.domain.strategy.AContextDefaultFactory
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkConfig
import com.example.day.core.core_features.agent.domain.workers.concrete.JustWorkWorker
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.llm.domain.model.ModelSettings
import com.example.day.core.core_features.pr_review.domain.model.PrFileInfo
import com.example.day.core.core_features.pr_review.domain.model.PrInfoResult
import com.example.day.core.core_features.pr_review.domain.repository.PrHandleRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

class PrReviewWorker @Inject constructor(
    private val justWorkWorker: JustWorkWorker,
    private val chatTools: ChatTools,
    private val prHandleRepository: PrHandleRepository,
    private val json: Json
) {

    suspend fun doWork(prNumber: Int, repo: String, chatId: Long): Result<String> {
        // Step A - Get ModelSettings
        val prState = prHandleRepository.getPrHandleStateFlow().first()
        val modelSettings = prState.modelSettings ?: ModelSettings.default()

        // Step B - Agent 1: pr_info_collector
        val infoConfig = JustWorkConfig(
            agentName = PrAgentNames.INFO_COLLECTOR,
            chatId = chatId,
            systemPrompt = "Ты помощник по сбору данных о Pull Request. Твоя задача — вызвать инструменты и вернуть структурированный JSON с информацией о PR. Не добавляй пояснений, выводи только JSON.",
            allowedTools = listOf("get_pr_info", "get_pr_diff"),
            defaultModel = { modelSettings },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true
        )

        val infoPrompt = """Собери информацию о Pull Request #$prNumber в репозитории $repo.

Используй инструменты get_pr_info и get_pr_diff.

Верни ТОЛЬКО валидный JSON (без markdown, без пояснений) в следующем формате:
{"title":"название PR","description":"описание PR","headSha":"sha коммита","files":[{"path":"путь/к/файлу.kt","status":"modified"}]}

Статусы файлов: added, modified, removed, renamed.
Включи все изменённые файлы."""

        val infoResult = justWorkWorker.doWork(infoConfig, infoPrompt)
        val result = infoResult.getOrElse { error ->
            return Result.failure(Exception("Не удалось получить данные о PR: ${error.message}"))
        }

        val prInfoResult = runCatching {
            val jsonText = result.trim()
                .let { if (it.contains("{")) it.substring(it.indexOf("{"), it.lastIndexOf("}") + 1) else it }
            json.decodeFromString<PrInfoResult>(jsonText)
        }.getOrElse {
            return Result.failure(Exception("Не удалось распарсить данные о PR: ${it.message}\nОтвет: $result"))
        }

        chatTools.addInfoMessage(chatId, "📋 PR: ${prInfoResult.title} | Файлов: ${prInfoResult.files.size}")

        // Step C - Loop over files
        val fileReports = mutableListOf<String>()

        for (file in prInfoResult.files) {
            chatTools.addInfoMessage(chatId, "👀 Смотрю файл: ${file.path}")

            val fileConfig = JustWorkConfig(
                agentName = PrAgentNames.FILE_REVIEWER,
                chatId = chatId,
                systemPrompt = """Ты опытный Kotlin-разработчик, проводишь ревью кода Android-приложения.
Проект использует Clean Architecture, Dagger, Jetpack Compose, Room, Kotlin Coroutines.
Будь конкретен: указывай строки кода, имена методов, объясняй почему это проблема.
Если что-то выглядит нормально — скажи об этом явно, не придумывай проблемы.""",
                allowedTools = listOf(
                    "search_codebase",
                    "get_git_file_list",
                    "get_file_content",
                    "get_pr_file_diff",
                    "add_pr_review_comment"
                ),
                defaultModel = { modelSettings },
                defaultContext = { AContextDefaultFactory.createFull() },
                recreateAgent = true
            )

            val filePrompt = """Проведи ревью изменений в файле "${file.path}" из Pull Request #$prNumber в репозитории $repo.

Информация о PR:
- Название: ${prInfoResult.title}
- Описание: ${prInfoResult.description}
- HEAD SHA: ${prInfoResult.headSha}

Что нужно сделать:
1. Получи diff этого файла: get_pr_file_diff(pr_number=$prNumber, repo=$repo, file_path="${file.path}")
2. По необходимости изучи контекст: используй search_codebase для поиска связанных классов
3. Если видишь конкретную проблему в конкретной строке — оставь комментарий через add_pr_review_comment (commit_id="${prInfoResult.headSha}")

ОБЯЗАТЕЛЬНО выдай структурированный отчёт:
## Файл: ${file.path}
### Потенциальные баги
### Архитектурные проблемы
### Рекомендации
### Что было сделано"""

            justWorkWorker.doWork(fileConfig, filePrompt)
                .onSuccess { report -> fileReports.add("### ${file.path}\n$report") }
                .onFailure { error -> fileReports.add("### ${file.path}\nОшибка ревью: ${error.message}") }
        }

        // Step D - Summary agent
        val summaryConfig = JustWorkConfig(
            agentName = PrAgentNames.SUMMARY_AGENT,
            chatId = chatId,
            systemPrompt = "Ты технический лид, пишешь итоговое резюме code review. Будь лаконичен. Выдели самое важное.",
            allowedTools = emptyList(),
            defaultModel = { modelSettings },
            defaultContext = { AContextDefaultFactory.createFull() },
            recreateAgent = true
        )

        val summaryPrompt = """По результатам ревью Pull Request #$prNumber "${prInfoResult.title}" в репозитории $repo напиши краткое резюме.

Результаты ревью файлов:
${fileReports.joinToString("\n\n---\n\n")}

Структура резюме:
## Резюме ревью PR #$prNumber: ${prInfoResult.title}
**Изменено файлов:** ${prInfoResult.files.size}
### 🐛 Потенциальные баги
### 🏗️ Архитектурные проблемы
### 💡 Рекомендации
### Общая оценка"""

        val summaryText = justWorkWorker.doWork(summaryConfig, summaryPrompt).getOrDefault("Резюме недоступно")

        // Step E - Save final report
        val fullReport = buildString {
            appendLine("# Ревью PR #$prNumber: ${prInfoResult.title}")
            appendLine("**Репозиторий:** $repo | **Файлов:** ${prInfoResult.files.size}")
            appendLine()
            fileReports.forEach { appendLine(it); appendLine() }
            appendLine("---")
            appendLine(summaryText)
        }
        chatTools.addBotMessage(chatId, fullReport)
        return Result.success(fullReport)
    }
}
