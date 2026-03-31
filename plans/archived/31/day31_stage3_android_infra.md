# Stage 3: Android — ChatType + FeatureEntry интерфейс

## Описание
Минимальные изменения, которые компилируются независимо:
- Добавить `ChatType.ASSISTANT` в enum
- Инкрементировать версию БД
- Создать интерфейс `AssistantConsoleFeatureEntry`

Всё остальное (EntryImpl, DI-биндинги, FeatureEntryProvider, ChatsScreen) добавляется в Stage 5 — потому что AppComponent (через Dagger) требует наличия binding для каждого метода FeatureEntryProvider. Без `@Binds AssistantConsoleFeatureEntryImpl` в `ConsoleFeatureApiModule` AppComponent не скомпилируется.

## Почему Stage 3 такой маленький

`AppComponent` implements `FeatureEntryProvider`. Dagger генерирует реализацию всех методов интерфейса. Если добавить `getAssistantConsoleFeatureEntry()` в `FeatureEntryProvider` без соответствующего `@Binds` в `ConsoleFeatureApiModule` — Dagger не найдёт binding и сборка упадёт. Поэтому `FeatureEntryProvider` + `ConsoleFeatureApiModule` + `AssistantConsoleFeatureEntryImpl` должны появиться вместе (в Stage 5).

## Файлы для изменения

### 1. `ChatType.kt`
**Путь**: `app/src/main/java/com/example/day/core/core_features/chat/domain/model/ChatType.kt`

Добавить новое значение в enum:
```
ASSISTANT("assistant", "Dev Assistant")
```

**GroupChoiceFeature не требует изменений**: `getAllChatTypes()` возвращает `enumValues<ChatType>().toList()` — ASSISTANT появится в выборе типа автоматически. `ensureChatTypesExist()` автосидит новый тип в БД при первом создании группы.

### 2. `ChatDatabase.kt`
**Путь**: `app/src/main/java/com/example/day/core/core_features/chat/data/local/ChatDatabase.kt`

Инкремент версии: `version = 16 → version = 17`
(`fallbackToDestructiveMigration()` уже установлен — явная миграция не нужна)

### 3. Новый файл: `AssistantConsoleFeatureEntry.kt`
**Путь**: `app/src/main/java/com/example/day/features/console/api/AssistantConsoleFeatureEntry.kt`

Интерфейс:
- Пакет: `com.example.day.features.console.api`
- Один метод: `@Composable fun EntryPoint(chatId: Long, modifier: Modifier)`
- Структура идентична `RagConsoleFeatureEntry`

## Резюме
**Что получим**: задел для нового типа чата — enum-значение, DB-версия, интерфейс. Этап компилируется самостоятельно.

**Критерии успеха**:
- Приложение компилируется после Stage 3
- `ChatType.ASSISTANT` доступен в коде
- В GroupChoiceScreen автоматически появляется тип "Dev Assistant" (без дополнительных изменений)
- ChatDatabase поднимается без краша (версия 17, destructive migration)
