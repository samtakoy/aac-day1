# Название задачи: рефакторинг чат-приложении для работы с llm - в слое TalkWorker

## Описание текущего состояния

AWorker - интерфейс обработчиков команд порльзователя

TalkWorker - использует агента для работы с llm

Поддерживает команды:
 - @@talk(info) - вывести настройки контекста
 - @@talk(context) - вывести полное содержимое контекста
 - @@talk(setup --msg X --extra Y) - настроить параметры сжатия
 - @@talk текст - отправить текст к LLM

```
// Получение агента
	val agent = agentTools.getOrCreateAgent(
            systemName = AGENT_NAME,
            chatId = chat.id,
            isCommonAgent = false
        )
// получение контекста агента
val context = agentTools.getContext(agent.id)
// получение состояния контекста
        val state = context.summarizationState
// Форматированный вывод
        val params = ContextParameters(
            msgLimit = if (state is SummarizationEnabledState) state.msgLimit else NO_SUMMARY_LIMIT,
            extraLimit = if (state is SummarizationEnabledState) state.extraLimit else DEFAULT_EXTRA_LIMIT,
            strategy = state.strategyName
        )
        
        val message = contextFormatter.formatContextInfo(params)
```

### Проблема
Не масштабируемо.
Трудно добавить новый тип обработки контекста и вывод отчета по состоянию контекста


## Что хотим поменять для достижение гибкости и масштабируемости

### Data слой

#### Рефакторинг чата
Начат и не закончен рефакторинг.
Git commit проделанной работы: 5f484052c22247b43c0ba856d3832dac19d2f8ea

Цель рефакторинга:
TalkWorker - будет как и раньше использоваться для обработки команд к llm-агенту
Логика TalkWorker коректна - и работала до начала изменений. 
Но код llm-агента теперь долджен находиться в классе AIAgent.
Статус класса AIAgent - взят из соседнего проекта и его адаптация под текущий проект еще не проводилась.
AIAgent это класс - который знает настройки агента - AgentConfig (и через настройки - стратегию агента для работы с контекстом),
знает как обращаться к llm и на вход получате промпты, а на выход выдает ошибку, либо ответ

TalkWorker больше не использует AgentTools, LlmRequestUseCase и сам не знает и не работает с контекстом агента.
Он должен разгрузится и использовать AIAgent

Для создания агентов он должен использовать StrategyFactory
Логика создания отчетов, установки параметров стратегии, обработки стратегии теперь будут инкапсулированы в классах наследниках ContextStrategy
(которые присутствуют но не доделаны)

ContextSummaryStrategy должна делать ту логику которую раньше делали ContextCompressionHandler и SummarizationUseCase


## Полезная информация

файл /prompts/product_briefing.md содержит давнюю переписку с заказчиком проекта, на основе которой можно составить мнение о том куда проект движется в будущем

## Замечания

Миграция не нужна

## Задача

Проанализируй проект, задай вопросы по непонятным местам.
Твоя цель сформировать план чтобы закончить начатый рефакторинг опираясь на логику TalkWorler и набросок AIAgent. Адаптировать работу TalkWorler на использование AIAgent, дописать AIAgent, дописать наследников ContextStrategy
Исполнитель сказал, что рефакторинг data слоя примерно закончен.
