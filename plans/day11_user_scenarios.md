# Day 11: Пользовательские сценарии и архитектура компонентов

## Обзор системы

Реализована трехуровневая модель памяти для AI-ассистента с иерархической структурой чатов (PLANNER тип группы).

---

## Сценарий 1: Создание PLANNER группы

### Описание
Пользователь создает новую группу чатов типа "Project Planner" для планирования проекта.

### Участники и их действия:

| Шаг | Класс/Сущность | Действие | Цель |
|-----|----------------|----------|------|
| 1 | `GroupChoiceScreen` (UI) | Пользователь нажимает "Создать группу" | Инициировать создание |
| 2 | `GroupChoiceViewModel` | Вызывает `CreatePlannerGroupWithMainChatUseCase.execute()` | Делегировать бизнес-логику |
| 3 | `CreatePlannerGroupWithMainChatUseCase` | Создает ChatGroup с типом PLANNER + MainChat | Обеспечить атомарность операции |
| 4 | `ChatRepository` | `createGroupWithMainChat()`: создает записи в ChatGroupDao, ChatDao, ChatSettingsDao | Персистентность данных |
| 5 | `ChatGroupEntity` | Сохраняется с type = "planner" | Определение типа группы |
| 6 | `ChatEntity` (Main Chat) | Сохраняется с isPlannerMain = true, parentId = null | Главный чат планировщика |
| 7 | Возврат в `GroupChoiceScreen` | Переход к списку чатов | Показать результат |

---

## Сценарий 2: Разговор с Planner агентом (Main Chat)

### Описание
Пользователь вводит сообщение в главном чате планировщика. Агент анализирует, предлагает план, сохраняет факты о пользователе.

### Участники и их действия:

| Шаг | Класс/Сущность | Действие | Цель |
|-----|----------------|----------|------|
| 1 | `ConsoleScreen` (UI) | Пользователь вводит текст, нажимает "Отправить" | Инициировать диалог |
| 2 | `ConsoleViewModel` | `sendMessage(input)` → вызывает TalkDelegate | Обработка ввода |
| 3 | `PlannerTalkDelegate` | `tryAddUserMessage()`: добавляет сообщение пользователя | Сохранение сообщения |
| 4 | `ChatTools` | `addUserMessage()` → `AddChatMessageUseCase` | Персистентность |
| 5 | `MessageEntity` | Сохраняется в БД | Хранение short-term памяти |
| 6 | `PlannerTalkDelegate` | Вызывает `plannerWorker.doWork()` | Обработка AI логики |
| 7 | `PlannerWorker` | **Извлечение Long-Term Memory**: `longTermMemoryRepository.getFactsByGroup(groupId)` | Получение профиля пользователя |
| 8 | `LongTermMemoryDao` | SELECT * FROM long_term_memory WHERE group_id = ? | Запрос к БД |
| 9 | `PlannerWorker` | **Построение system prompt**: `PlannerPromptBuilder.buildMainPlannerPrompt()` | Формирование контекста |
| 10 | `PlannerPromptBuilder` | Инжектирует LTM факты в system prompt | Контекст для LLM |
| 11 | `PlannerWorker` | Вызывает `aiAgentFactory.getOrCreate()` → `agent.process()` | Запрос к LLM |
| 12 | `AIAgent` (через LlmRequestUseCase) | Отправляет запрос к LLM API | Получение ответа |
| 13 | `PlannerWorker` | Получает ответ, парсит: `ToolResponseParser.parse()` | Обработка инструментов |
| 14 | `ToolResponseParser` | Ищет паттерны: SAVE_FACT[key:category:fact] | Обнаружение команд |
| 15 | `PlannerWorker` | **SAVE_FACT**: вызывает `longTermMemoryRepository.upsertFact()` | Сохранение в LTM |
| 16 | `LongTermMemoryDao` | UPSERT с составным ключом (memoryKey + groupId) | Изоляция по группе |
| 17 | `PlannerWorker` | **Очистка ответа**: удаляет маркеры инструментов из текста | Чистый ответ |
| 18 | `PlannerWorker` | `chatTools.addBotMessage()` | Сохранение ответа |
| 19 | `ConsoleViewModel` | Получает WorkerEvent, обновляет StateFlow | UI реактивность |
| 20 | `ConsoleScreen` | Отображает ответ агента | Показ пользователю |

---

## Сценарий 3: Предложение создания этапа (CREATE_STAGE)

### Описание
Агент определяет, что пора перейти к детальной проработке этапа, и предлагает создать новый чат-этап.

### Участники и их действия:

| Шаг | Класс/Сущность | Действие | Цель |
|-----|----------------|----------|------|
| 1 | `ToolResponseParser` | Находит паттерн CREATE_STAGE[title:context] | Обнаружение намерения |
| 2 | `PlannerWorker` | Создает `WorkerEvent.StageCreationSuggested(stageTitle, workingSummary)` | Уведомление UI |
| 3 | `PlannerTalkDelegate` | Получает event через SharedFlow | Трансляция события |
| 4 | `ConsoleViewModel` | Подписывается: `getPlannerEvents<PlannerUiEvent>()` | Обработка события |
| 5 | `ConsoleViewModel` | `_stageCreationState.value = StageCreationState.Suggested(...)` | Показ UI |
| 6 | `ConsoleScreen` | Показывает диалог с кнопками "Создать" / "Отмена" | Human-in-the-loop |
| 7 | Пользователь | Нажимает "Создать этап" | Подтверждение |
| 8 | `ConsoleViewModel` | `confirmStageCreation()` → `createPlannerStageChatUseCase.execute()` | Создание чата-этапа |
| 9 | `CreatePlannerStageChatUseCase` | Вызывает `chatRepository.createSubChat()` | Создание в БД |
| 10 | `ChatDao` | INSERT с parentId = mainChatId, workingSummary = context | Иерархия чатов |
| 11 | `ConsoleViewModel` | Вызывает `navigator.navigate(Chat(route))` | Переход к этапу |
| 12 | `ConsoleScreen` | Загружает новый чат-этап | Новый контекст |

---

## Сценарий 4: Работа в чате-этапе

### Описание
Пользователь перешел в чат-этап "Этап 1" и продолжает работу.

### Участники и их действия:

| Шаг | Класс/Сущность | Действие | Цель |
|-----|----------------|----------|------|
| 1 | `ConsoleViewModel` | Загружает Chat с parentId (определен как этап) | Определение типа чата |
| 2 | `PlannerWorker` | **Определение типа**: `chat.isStageChat = (parentId != null)` | Контекст обработки |
| 3 | `PlannerWorker` | **Working Memory**: `chat.workingSummary` (получено от родителя) | Наследование контекста |
| 4 | `PlannerWorker` | **Short-term**: Загружает сообщения текущего чата | Текущий диалог |
| 5 | `PlannerWorker` | **System Prompt**: `PlannerPromptBuilder.buildStagePrompt(stageTitle, parentSummary, ltmFacts)` | Контекст для LLM |
| 6 | `PlannerPromptBuilder` | Формирует: "Вы в этапе X. Общая цель: Y. Профиль пользователя: Z" | Инструкции агенту |
| 7 | Обработка аналогична Сценарию 2 | LLM запрос → ответ → парсинг инструментов | Стандартный цикл |

---

## Сценарий 5: Завершение этапа (COMPLETE_STAGE)

### Описание
Пользователь или агент решает завершить этап и сохранить результаты.

### Участники и их действия:

| Шаг | Класс/Сущность | Действие | Цель |
|-----|----------------|----------|------|
| 1 | `ToolResponseParser` | Находит паттерн COMPLETE_STAGE[outcome] | Обнаружение завершения |
| 2 | `PlannerWorker` | **Сохранение артефакта**: `artifactRepository.saveArtifact()` | Персистентность результата |
| 3 | `ArtifactDao` | INSERT в project_artifacts | Хранение результата этапа |
| 4 | `PlannerWorker` | **Обновление родителя**: Читает parentChat, добавляет результат в workingSummary | Наследование результата |
| 5 | `ChatDao` | UPDATE working_summary родительского чата | Агрегация результатов |
| 6 | `PlannerWorker` | `WorkerEvent.StageCompleted(chatId, outcome)` | Уведомление UI |
| 7 | `ConsoleViewModel` | Может показать toast "Этап завершен" | Обратная связь |

---

## Сценарий 6: Просмотр памяти (Memory Inspector)

### Описание
Пользователь хочет увидеть все три слоя памяти текущего чата.

### Участники и их действия:

| Шаг | Класс/Сущность | Действие | Цель |
|-----|----------------|----------|------|
| 1 | `ConsoleScreen` | Пользователь нажимает кнопку "Память" | Инициировать просмотр |
| 2 | `ConsoleViewModel` | `loadMemoryInspectorData()` | Загрузка данных |
| 3 | **Short-term** | `getMessagesUseCase.execute(chatId)` | Сообщения чата |
| 4 | **Working** | `chat.workingSummary` (уже загружен) | Контекст задачи |
| 5 | **Long-term** | `longTermMemoryRepository.getFactsByGroupFlow(groupId)` | Факты профиля |
| 6 | `MemoryInspectorUiModel` | Формирует UI модель с тремя слоями | Агрегация для UI |
| 7 | `MemoryInspectorView` (Compose) | Отображает 3 секции с данными | Визуализация |

---

## Диаграмма потока данных

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           UI LAYER                                      │
│  ┌──────────────┐    ┌─────────────────┐    ┌────────────────────┐   │
│  │GroupChoice   │    │   ConsoleScreen │    │MemoryInspectorView │   │
│  │   Screen     │    │                 │    │                    │   │
│  └──────┬───────┘    └────────┬────────┘    └─────────┬──────────┘   │
│         │                      │                       │               │
│         ▼                      ▼                       ▼               │
│  ┌──────────────┐    ┌─────────────────┐    ┌────────────────────┐   │
│  │GroupChoice   │    │ConsoleViewModel  │    │MemoryInspector     │   │
│  │ViewModel    │    │                 │    │UiModel              │   │
│  └──────┬───────┘    └────────┬────────┘    └────────────────────┘   │
│         │                      │                                        │
│         │                      │ StageCreationState                     │
│         ▼                      ▼                                        │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │                    TalkDelegate (PlannerTalkDelegate)        │       │
│  └─────────────────────────────┬────────────────────────────────┘       │
└────────────────────────────────┼───────────────────────────────────────┘
                                 │
                                 ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         DOMAIN LAYER                                      │
│  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────┐  │
│  │CreatePlannerGroup  │  │CreatePlannerStage  │  │ ChatRepository │  │
│  │WithMainChatUseCase│  │ChatUseCase          │  │                │  │
│  └─────────┬──────────┘  └─────────┬───────────┘  └───────┬────────┘  │
│            │                       │                       │            │
│            └───────────────────────┼───────────────────────┘            │
│                                    ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────┐     │
│  │                      PlannerWorker                              │     │
│  │  ┌───────────────┐ ┌────────────────┐ ┌───────────────────┐  │     │
│  │  │ToolResponse   │ │PlannerPrompt   │ │   AIAgentFactory  │  │     │
│  │  │Parser         │ │Builder         │ │                   │  │     │
│  │  └───────────────┘ └────────────────┘ └───────────────────┘  │     │
│  │         │                │                    │                │     │
│  │         ▼                ▼                    ▼                │     │
│  │  ┌──────────────────────────────────────────────────────────┐ │     │
│  │  │              LongTermMemoryRepository                    │ │     │
│  │  └──────────────────────────────────────────────────────────┘ │     │
│  └─────────────────────────────────────────────────────────────────┘     │
└───────────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                      │
│  ┌──────────────────┐  ┌───────────────────┐  ┌────────────────────┐   │
│  │LongTermMemoryDao │  │   ArtifactDao     │  │     ChatDao        │   │
│  └────────┬─────────┘  └────────┬──────────┘  └─────────┬──────────┘   │
│           │                      │                       │               │
│           ▼                      ▼                       ▼               │
│  ┌──────────────────────────────────────────────────────────────────┐    │
│  │                      ChatDatabase (Room)                         │    │
│  │  ┌──────────────┐ ┌────────────────┐ ┌───────────────────┐   │    │
│  │  │long_term_    │ │project_        │ │ chats              │   │    │
│  │  │memory        │ │artifacts       │ │ (with parent_id)  │   │    │
│  │  └──────────────┘ └────────────────┘ └───────────────────┘   │    │
│  └──────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## Три слоя памяти: где что хранится

| Слой | Источник данных | Хранилище | Пример |
|-------|-----------------|-----------|--------|
| **Short-term** | Сообщения текущего чата | `messages` table | "Что он сказал секунду назад?" |
| **Working** | Контекст задачи | `ChatEntity.working_summary` | Цель проекта, текущий этап |
| **Long-term** | Профиль пользователя | `long_term_memory` table | Навыки, предпочтения, опыт |

---

## Ключевые классы и их ответственность

| Класс | Слой | Ответственность |
|-------|------|----------------|
| `PlannerWorker` | Domain | Главная бизнес-логика: извлечение памяти, запрос LLM, обработка инструментов |
| `ToolResponseParser` | Domain | Парсинг псевдо-команд из текста LLM |
| `PlannerPromptBuilder` | Domain | Формирование system prompts с инжекцией памяти |
| `LongTermMemoryRepository` | Domain | Интерфейс для работы с долгосрочной памятью |
| `CreatePlannerStageChatUseCase` | Domain | Создание чата-этапа (UseCase) |
| `LongTermMemoryDao` | Data | DAO для работы с LTM в БД |
| `LongTermMemoryEntity` | Data | Сущность БД с составным ключом |
| `PlannerTalkDelegate` | UI | Делегат для обработки диалога в консоли |
| `ConsoleViewModel` | UI | State management, обработка событий этапов |
| `MemoryInspectorView` | UI | Визуализация трех слоев памяти |
