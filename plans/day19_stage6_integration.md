# Этап 6: Интеграция и тестирование

## Общее описание

Финальный этап интеграции всех компонентов и тестирование полного пайплайна исследования файлов GitHub.

**Цель этапа:** Обеспечить корректную совместную работу всех компонентов и протестировать автоматическое выполнение цепочки инструментов.

---

## Задачи этапа

### Шаг 6.1: Инициализация агентов при первом вызове

**Описание:** Агенты "git_file_investigator" и "content_analyzer" создаются и настраиваются при первом вызове соответствующих инструментов.

**Место инициализации:** В инструментах LocalMcpService через JustWorkWorker.

#### Инициализация в InvestigateGitFileTool

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/InvestigateGitFileTool.kt`

**Логика:**
```kotlin
override suspend fun call(arguments: JsonObject, context: McpToolCallContext?): Result<String> {
    val config = JustWorkConfig(
        agentName = AGENT_NAME,  // "git_file_investigator"
        chatId = context?.chatId ?: error("chatId required"),
        systemPrompt = buildSystemPrompt(),  // См. Шаг 4.1
        allowedTools = listOf(
            McpToolNames.GET_GIT_FILE_LIST,
            McpToolNames.GET_FILE_ANALYSIS
        ),
        defaultModel = { /* модель по умолчанию */ },
        defaultContext = { AContextDefaultFactory.createFull() }
    )
    
    // JustWorkWorker вызовет aiAgentFactory.getOrCreate с onCreateCallback
    // onCreateCallback сработает ТОЛЬКО при первом создании агента
    justWorkWorker.doWork(
        config = config,
        userPrompt = "Мне нужен результат анализа файла. $fileRequestMessage"
    )
}
```

**Что происходит при первом вызове:**
1. JustWorkWorker вызывает `aiAgentFactory.getOrCreate`
2. `AgentRepository.getOrCreateAgent` проверяет наличие агента в БД
3. Если агента нет — создает и вызывает `onCreateCallback`
4. Callback сохраняет systemPrompt и allowedTools в AgentMemoryRepository
5. При последующих вызовах агент берется из БД без вызова callback

---

#### Инициализация в AnalyzeCodeContentTool

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/inmemory/AnalyzeCodeContentTool.kt`

**Логика:**
```kotlin
override suspend fun call(arguments: JsonObject, context: McpToolCallContext?): Result<String> {
    val config = JustWorkConfig(
        agentName = AGENT_NAME,  // "content_analyzer"
        chatId = context?.chatId ?: 0L,
        systemPrompt = buildSystemPrompt(),  // См. Шаг 4.3 (точный текст из задания)
        allowedTools = emptyList(),  // Не требует tools
        defaultModel = { /* модель по умолчанию */ },
        defaultContext = { AContextDefaultFactory.createFull() }
    )
    
    justWorkWorker.doWork(
        config = config,
        userPrompt = "Проанализируй пожалуйста это:\n$content"
    )
}
```

**Важно:**
- Нет отдельного AgentInitializer
- Инициализация происходит лениво, при первом вызове инструмента
- onCreateCallback гарантирует однократную настройку

---

### Шаг 6.2: Интеграция с приложением

**Описание:** Все компоненты интегрируются через Dagger DI.

**Автоматическая интеграция:**
- У всех классов `@Inject constructor` — Dagger создаст экземпляры автоматически
- `@Binds` в модулях связывают интерфейсы с реализациями
- Не требуется явный код инициализации в MyApp

**Минимальные изменения в приложении:**
- Обновление версии Room до 16 (Этап 1)
- Регистрация CommandHandler в CommandDispatcher (Этап 2)

---

### Шаг 6.3: Тестирование пайплайна

**Описание:** Последовательное тестирование каждого инструмента и полного пайплайна.

**Сценарий тестирования:**

#### Тест 1: get_git_file_list

```
User input: @@mcp get_git_file_list

Ожидаемый результат:
{
  "status": "ok",
  "content": [
    "/app/src/main/java/com/example/day/app/MyApp.kt",
    "/app/src/main/java/com/example/day/core/di/NetworkModule.kt",
    ...
  ]
}
```

**Проверка:**
- [ ] Возвращается непустой список файлов
- [ ] Пути начинаются с "/"
- [ ] Формат JSON корректный

---

#### Тест 2: investigate_git_file

```
User input: @@talk Найди файл с именем McpToolProvider

Ожидаемый результат:
1. TalkWorker создает агента "talk_agent"
2. Агент вызывает investigate_git_file tool
3. investigate_git_file создает агента "git_file_investigator" (onCreateCallback при первом вызове)
4. git_file_investigator:
   - Вызывает get_git_file_list
   - Находит файл "/.../McpToolProvider.kt"
   - Вызывает get_file_analysis
5. Возвращается результат анализа

Финальное сообщение:
"Файл: /app/src/main/java/com/example/day/core/core_features/agent/data/tools/McpToolProvider.kt

Анализ:
[результат анализа]"
```

**Проверка:**
- [ ] Агент "git_file_investigator" создается при первом вызове
- [ ] Системный промпт установлен через onCreateCallback
- [ ] Доступны только разрешенные tools
- [ ] get_git_file_list вызывается первым
- [ ] Файл находится по описанию
- [ ] get_file_analysis вызывается вторым
- [ ] Результат возвращается пользователю

---

#### Тест 3: get_file_analysis (кеширование)

```
User input: @@mcp get_file_analysis {"file_full_path": "/app/src/main/java/com/example/day/core/core_features/agent/data/tools/McpToolProvider.kt"}

Первый вызов:
- Проверяет FileAnalysisEntity (не найдено)
- Скачивает файл
- Вызывает analyze_code_content
- Сохраняет результат в кеш
- Возвращает результат

Второй вызов (сразу после первого):
- Проверяет FileAnalysisEntity (найдено)
- Возвращает из кеша (без вызова analyze_code_content)
```

**Проверка:**
- [ ] Первый вызов выполняет анализ
- [ ] Результат сохраняется в FileAnalysisEntity
- [ ] Второй вызов возвращает из кеша
- [ ] Кеш работает корректно

---

#### Тест 4: analyze_code_content

```
User input: @@mcp analyze_code_content {"content": "class McpToolProvider { ... }"}

Ожидаемый результат:
1. Создается агент "content_analyzer" (onCreateCallback при первом вызове)
2. Агент получает системный промпт через onCreateCallback
3. Агент анализирует содержимое
4. Возвращается форматированный анализ

Формат ответа:
{
  "status": "ok",
  "analysis_result": "1. Краткое описание...\n2. Сильные стороны...\n..."
}
```

**Проверка:**
- [ ] Агент "content_analyzer" создается при первом вызове
- [ ] Системный промпт установлен
- [ ] Анализ соответствует формату
- [ ] JSON формат корректный

---

#### Тест 5: reset_git_file_list_cache

```
User input: @@mcp reset_git_file_list_cache

Ожидаемый результат:
{
  "status": "ok",
  "content": []
}
```

**Проверка:**
- [ ] Кеш очищается
- [ ] Следующий get_git_file_list запрашивает API

---

#### Тест 6: Полный пайплайн

```
User input: @@talk Найди и проанализируй файл McpToolProvider

Полная цепочка:
1. TalkWorker → talk_agent
2. talk_agent → investigate_git_file tool
3. investigate_git_file → JustWorkWorker → git_file_investigator (onCreateCallback при первом вызове)
4. git_file_investigator → get_git_file_list
5. get_git_file_list → GitHub API (или кеш)
6. git_file_investigator → находит файл
7. git_file_investigator → get_file_analysis
8. get_file_analysis → проверяет кеш
9. get_file_analysis → analyze_code_content (если нет в кеше)
10. analyze_code_content → JustWorkWorker → content_analyzer (onCreateCallback при первом вызове)
11. content_analyzer → анализ
12. Результат поднимается по цепочке к пользователю
```

**Проверка:**
- [ ] Все инструменты вызываются последовательно
- [ ] Данные передаются корректно между инструментами
- [ ] Ошибки обрабатываются на каждом уровне
- [ ] Финальный результат понятен пользователю

---

### Шаг 6.4: Тестирование управления tools

**Сценарий:**

```
# Проверяем текущие rules (для сравнения)
User: @@talk(agent --listrules)
Bot: Правила диалога отсутствуют...

# Добавляем tool
User: @@talk(agent --addtool get_git_file_list)
Bot: Инструмент добавлен. Всего инструментов: 1

# Проверяем список
User: @@talk(agent --listtools)
Bot: 
Доступные инструменты (1):
1. get_git_file_list

# Добавляем еще один
User: @@talk(agent --addtool get_file_analysis)
Bot: Инструмент добавлен. Всего инструментов: 2

# Очищаем список
User: @@talk(agent --cleartools)
Bot: Все инструменты удалены
```

**Проверка:**
- [ ] --addtool добавляет инструмент
- [ ] --listtools показывает текущий список
- [ ] --cleartools очищает список
- [ ] McpToolProvider фильтрует инструменты по списку

---

### Шаг 6.5: Отладка и исправление ошибок

**Типичные проблемы и решения:**

| Проблема | Возможная причина | Решение |
|----------|-------------------|---------|
| Agent not found | Агент не создан | Проверить JustWorkWorker вызов |
| Tool not available | Tool не в списке разрешенных | Проверить allowedTools в JustWorkConfig |
| Cache not working | БД не обновлена | Проверить миграцию Room |
| MCP connection error | Сервер не запущен | Проверить Docker контейнер |
| System prompt not applied | MemoryProvider не подключен | Проверить MemoryProviderFactory |
| onCreateCallback не вызывается | Агент уже существует в БД | Очистить таблицу agents для теста |

---

## Резюме этапа

**Что получим:**
- ✅ Ленивая инициализация агентов при первом вызове инструмента
- ✅ onCreateCallback срабатывает ТОЛЬКО при создании агента
- ✅ Интеграция через Dagger DI (без явного кода)
- ✅ Набор тестов для каждого инструмента
- ✅ Тест полного пайплайна
- ✅ Тест управления tools через команды

**Критерии успеха:**
1. Агенты создаются при первом вызове соответствующего инструмента
2. onCreateCallback настраивает systemPrompt и allowedTools однократно
3. Полный пайплайн выполняется от начала до конца
4. Данные корректно передаются между инструментами
5. Кеширование работает
6. Команды управления tools работают
7. Ошибки обрабатываются корректно

---

## Зависимости от других этапов

- ✅ Зависит от всех предыдущих этапов (1-5)
- ✅ Финальный интеграционный этап

---

## План реализации (подробный)

1. Протестировать get_git_file_list
2. Протестировать investigate_git_file (проверить создание агента)
3. Протестировать get_file_analysis (кеширование)
4. Протестировать analyze_code_content (проверить создание агента)
5. Протестировать reset_git_file_list_cache
6. Протестировать полный пайплайн
7. Протестировать команды управления tools (--addtool/--listtools/--cleartools)
8. Исправить найденные ошибки
9. Задокументировать результаты тестирования
