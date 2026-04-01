# Этап 3: TelegramPollingWorker + StartPrReviewUseCase

## Общее описание

Создание периодического WorkManager-воркера, который опрашивает Telegram и запускает ревью при обнаружении нового PR. А также use case-ов для управления состоянием и запуском ревью.

**Зависимости:** Этап 2 (PrHandleRepository, TelegramRepository, BuildConfig)

**Что получим:**
- `TelegramPollingWorker` — запускается каждую минуту (цепочка OneTimeWorkRequest), опрашивает Telegram, при новом PR вызывает `StartPrReviewUseCase`
- `GetPrHandleStateUseCase` — Flow для наблюдения состояния фичи
- `SetPrHandleEnabledUseCase` — включает/выключает мониторинг (стартует/останавливает WorkManager)
- `StartPrReviewUseCase` — добавляет инфо-сообщения, уведомления, запускает `PrReviewWorker` (реализуется в Этапе 4, сейчас заглушка)

**Критерии успеха:**
- Включить HandlePr в настройках чата → в logcat видно `TelegramPolling: пусто` каждую минуту
- Отправить тестовое JSON-сообщение в Telegram бот → в logcat видно `TelegramPolling: новый pr: 42`
- Выключить HandlePr → цепочка воркеров останавливается (нет новых logcat записей)

---

## Задача 3.1: GetPrHandleStateUseCase

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/usecase/GetPrHandleStateUseCase.kt`**

```kotlin
class GetPrHandleStateUseCase @Inject constructor(
    private val prHandleRepository: PrHandleRepository
) {
    operator fun invoke(): Flow<PrHandleState> = prHandleRepository.getPrHandleStateFlow()
}
```

---

## Задача 3.2: SetPrHandleEnabledUseCase

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/usecase/SetPrHandleEnabledUseCase.kt`**

- Инжектирует: `PrHandleRepository`, `WorkManager`

```kotlin
class SetPrHandleEnabledUseCase @Inject constructor(
    private val prHandleRepository: PrHandleRepository,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(isEnabled: Boolean, chatId: Long, modelSettings: ModelSettings? = null) {
        prHandleRepository.setPrHandleState(isEnabled, chatId, modelSettings)
        if (isEnabled) {
            val request = OneTimeWorkRequestBuilder<TelegramPollingWorker>().build()
            workManager.enqueueUniqueWork(
                TelegramPollingWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        } else {
            workManager.cancelUniqueWork(TelegramPollingWorker.WORK_NAME)
        }
    }
}
```

**Импорты:** `androidx.work.*`, `com.example.day.core.core_features.pr_review.data.worker.TelegramPollingWorker`

---

## Задача 3.3: StartPrReviewUseCase

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/domain/usecase/StartPrReviewUseCase.kt`**

- Инжектирует: `PrHandleRepository`, `ChatTools`, `NotificationManager` (или android Context для уведомлений), `PrReviewWorker` (из Этапа 4)

**На данном этапе (3):** создать класс, но `PrReviewWorker` заменить на заглушку:
```kotlin
// TODO Stage 4: inject PrReviewWorker
// Временно: просто добавить сообщение что ревью "запущено"
```

```kotlin
class StartPrReviewUseCase @Inject constructor(
    private val prHandleRepository: PrHandleRepository,
    private val chatTools: ChatTools,
    private val prReviewWorker: PrReviewWorker  // добавить после Этапа 4
) {
    suspend operator fun invoke(prNumber: Int, repo: String) {
        val state = prHandleRepository.getPrHandleStateFlow().first()
        val chatId = state.chatId
        if (chatId < 0) return  // нет привязанного чата

        chatTools.addInfoMessage(chatId, "🔍 Начинаем ревью PR #$prNumber: $repo")

        // TODO: Android Notification здесь (опционально)

        val result = prReviewWorker.doWork(prNumber, repo, chatId)  // Этап 4

        result.fold(
            onSuccess = {
                chatTools.addInfoMessage(chatId, "✅ Ревью PR #$prNumber завершено")
            },
            onFailure = { error ->
                chatTools.addInfoMessage(chatId, "❌ Ревью PR #$prNumber завершилось с ошибкой: ${error.message}")
            }
        )
    }
}
```

**Временная заглушка на Этапе 3** (без `PrReviewWorker`): после `addInfoMessage("🔍...")` просто ждать 2 секунды и добавить `addInfoMessage("⏳ PrReviewWorker — TODO Этап 4")`. Это позволяет протестировать Этап 3 независимо.

**Импорты:** `kotlinx.coroutines.flow.first`

---

## Задача 3.4: TelegramPollingWorker

### Файл для создания

**`app/src/main/java/com/example/day/core/core_features/pr_review/data/worker/TelegramPollingWorker.kt`**

- Наследует: `CoroutineWorker(appContext, params)`
- Константы: `companion object { const val WORK_NAME = "telegram_polling" }`

### Реализация `doWork()`

```
1. Получить appComponent:
   val appComponent = (applicationContext as? MyApp)?.appComponent ?: return Result.failure()

2. Получить зависимости через appComponent:
   val telegramRepo = appComponent.telegramRepository()
   val prHandleRepo = appComponent.prHandleRepository()
   val startPrReviewUseCase = appComponent.startPrReviewUseCase()

3. Получить последний update_id:
   val lastUpdateId = prHandleRepo.getLastTelegramUpdateId()

4. Запросить обновления:
   val result = telegramRepo.getPrUpdates(offset = lastUpdateId + 1)

5. Обработать результат:
   result.fold(
     onSuccess = { events ->
       if (events.isEmpty()) {
         Log.d("TelegramPolling", "пусто")
       } else {
         events.forEach { event ->
           Log.d("TelegramPolling", "новый pr: ${event.prNumber}")
           startPrReviewUseCase(event.prNumber, event.repo)
           // Сохраняем последний update_id
           prHandleRepo.saveLastTelegramUpdateId(event.updateId)
         }
       }
     },
     onFailure = { error ->
       Log.e("TelegramPolling", "ошибка: ${error.message}")
     }
   )

6. Запланировать следующий запуск через 1 минуту:
   val nextRequest = OneTimeWorkRequestBuilder<TelegramPollingWorker>()
     .setInitialDelay(1, TimeUnit.MINUTES)
     .build()
   WorkManager.getInstance(applicationContext)
     .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, nextRequest)

7. Вернуть Result.success()
```

**Важно:** если `startPrReviewUseCase(...)` долго выполняется (это Вариант А — синхронный), то `doWork()` не завершится до окончания ревью. Следующий `TelegramPollingWorker` будет запланирован (шаг 6) только после завершения ревью. Это приемлемо для демо.

**Обработка нескольких событий:** если в очереди несколько PR — обрабатывать по одному (только первый, у которого `updateId > lastUpdateId`). После сохранения `updateId` первого PR — следующий запуск подхватит остальные.

**Альтернатива:** обрабатывать все события из `events`, последовательно. Сохранять максимальный `updateId` после всех. Для демо — обрабатывать только первый (более безопасно).

**Импорты:**
```kotlin
import android.util.Log
import androidx.work.*
import com.example.day.app.MyApp
import java.util.concurrent.TimeUnit
```

---

## Задача 3.5: Добавить StartPrReviewUseCase в AppComponent

### Файл для изменения

**`app/src/main/java/com/example/day/app/di/AppComponent.kt`**

Добавить метод экспозиции:
```kotlin
fun startPrReviewUseCase(): StartPrReviewUseCase
```

И новые use cases в DI модуль (или в существующий AppModule если там нет отдельного модуля для use cases):

### Файл для изменения

**`app/src/main/java/com/example/day/core/core_features/pr_review/di/PrReviewModule.kt`**

Добавить провижн use cases. Поскольку use cases используют `@Inject constructor`, Dagger создаст их автоматически — явных биндингов не нужно. Но если нужен `@Singleton`:
```kotlin
// Use cases с @Inject constructor не требуют явного биндинга
// Dagger найдёт их автоматически при запросе через AppComponent
```

---

## Задача 3.6: WorkManager в DI

`WorkManager` уже используется в проекте (`ReminderSchedulerImpl`). Проверить где он провайдится в DI:

- Найти файл где `WorkManager` биндится/провайдится (скорее всего в `AppModule` или отдельном `WorkManagerModule`)
- Убедиться что `WorkManager` доступен для инжекции в `SetPrHandleEnabledUseCase`
- Если не доступен — добавить `@Provides` метод:
  ```kotlin
  @Provides
  @Singleton
  fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
      WorkManager.getInstance(context)
  ```
  **Примечание:** В проекте нет Hilt, поэтому `@ApplicationContext` не используется. `Context` инжектируется напрямую через AppComponent — проверить как это сделано в существующем коде.

---

## Структура новых файлов этапа

```
app/src/main/java/com/example/day/core/core_features/pr_review/
├── domain/
│   └── usecase/
│       ├── GetPrHandleStateUseCase.kt    (новый)
│       ├── SetPrHandleEnabledUseCase.kt  (новый)
│       └── StartPrReviewUseCase.kt       (новый, с заглушкой)
└── data/
    └── worker/
        └── TelegramPollingWorker.kt      (новый)

app/src/.../app/di/AppComponent.kt        (изменить — добавить startPrReviewUseCase())
app/src/.../pr_review/di/PrReviewModule.kt (изменить — убедиться что use cases доступны)
```
