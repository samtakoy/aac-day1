# Этап 1: Инфраструктура данных

## Общее описание

Создание инфраструктуры данных для кеширования:
1. Списка файлов GitHub (GitFileCacheEntity)
2. Результатов анализа файлов (FileAnalysisEntity)

**Цель этапа:** Подготовить слой данных для хранения кеша, чтобы избежать лишних запросов к GitHub API и повторного анализа файлов.

---

## Задачи этапа

### Шаг 1.1: GitFileCacheEntity

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/entity/GitFileCacheEntity.kt`

**Описание:** Room entity для кеширования списка файлов GitHub.

**Структура:**
```kotlin
@Entity(tableName = "git_file_cache")
data class GitFileCacheEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "file_list_json") val fileListJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long
)
```

**Детали:**
- Таблица содержит только одну запись (singleton кеш)
- `fileListJson` — JSON массив строк с полными путями к файлам
- `expiresAt` — timestamp истечения кеша (для возможности обновления)

---

### Шаг 1.2: FileAnalysisEntity

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/entity/FileAnalysisEntity.kt`

**Описание:** Room entity для кеширования результатов анализа файлов.

**Структура:**
```kotlin
@Entity(
    tableName = "file_analysis",
    indices = [Index(value = ["file_path"], unique = true)]
)
data class FileAnalysisEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
```

**Детали:**
- Уникальный индекс на `filePath` для предотвращения дублирования
- `content` — текстовый результат анализа файла

---

### Шаг 1.3: GitFileCacheDao

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/GitFileCacheDao.kt`

**Описание:** DAO интерфейс для работы с кешем списка файлов.

**Методы:**
```kotlin
@Dao
interface GitFileCacheDao {
    @Query("SELECT * FROM git_file_cache LIMIT 1")
    suspend fun getCachedFileList(): GitFileCacheEntity?
    
    @Query("SELECT * FROM git_file_cache LIMIT 1")
    fun getCachedFileListAsFlow(): Flow<GitFileCacheEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: GitFileCacheEntity)
    
    @Query("DELETE FROM git_file_cache")
    suspend fun clearCache()
}
```

---

### Шаг 1.4: FileAnalysisDao

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/data/local/dao/FileAnalysisDao.kt`

**Описание:** DAO интерфейс для работы с кешем анализа файлов.

**Методы:**
```kotlin
@Dao
interface FileAnalysisDao {
    @Query("SELECT * FROM file_analysis WHERE file_path = :filePath")
    suspend fun getAnalysis(filePath: String): FileAnalysisEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: FileAnalysisEntity)
    
    @Query("DELETE FROM file_analysis WHERE file_path = :filePath")
    suspend fun deleteAnalysis(filePath: String)
    
    @Query("SELECT * FROM file_analysis")
    fun getAllAnalysesAsFlow(): Flow<List<FileAnalysisEntity>>
}
```

---

### Шаг 1.5: Обновление ChatDatabase

**Расположение:** `app/src/main/java/com/example/day/core/core_features/chat/data/local/ChatDatabase.kt`

**Описание:** Добавление новых сущностей в базу данных.

**Изменения:**
1. Добавить `GitFileCacheEntity::class` в список `entities`
2. Добавить `FileAnalysisEntity::class` в список `entities`
3. Увеличить `version` с 15 до 16
4. Добавить методы для получения DAO:
   ```kotlin
   abstract fun gitFileCacheDao(): GitFileCacheDao
   abstract fun fileAnalysisDao(): FileAnalysisDao
   ```

---

### Шаг 1.6: Domain модели

**Расположение:**
- `app/src/main/java/com/example/day/core/core_features/mcp/domain/model/GitFileCache.kt`
- `app/src/main/java/com/example/day/core/core_features/mcp/domain/model/FileAnalysis.kt`

**Описание:** Domain модели для представления данных в бизнес-логике.

**Структура:**
```kotlin
// GitFileCache.kt
data class GitFileCache(
    val id: Long,
    val fileListJson: String,
    val createdAt: Long,
    val expiresAt: Long
)

// FileAnalysis.kt
data class FileAnalysis(
    val id: Long,
    val filePath: String,
    val content: String,
    val createdAt: Long
)
```

---

### Шаг 1.7: Мапперы

**Расположение:**
- `app/src/main/java/com/example/day/core/core_features/mcp/data/mapper/GitFileCacheMapper.kt`
- `app/src/main/java/com/example/day/core/core_features/mcp/data/mapper/FileAnalysisMapper.kt`

**Описание:** Мапперы для преобразования между Entity и Domain моделями.

**Пример:**
```kotlin
// GitFileCacheMapper.kt
internal object GitFileCacheMapper {
    fun toDomain(entity: GitFileCacheEntity): GitFileCache = GitFileCache(
        id = entity.id,
        fileListJson = entity.fileListJson,
        createdAt = entity.createdAt,
        expiresAt = entity.expiresAt
    )
    
    fun toEntity(domain: GitFileCache): GitFileCacheEntity = GitFileCacheEntity(
        id = domain.id,
        fileListJson = domain.fileListJson,
        createdAt = domain.createdAt,
        expiresAt = domain.expiresAt
    )
}
```

---

### Шаг 1.8: Repository интерфейсы

**Расположение:**
- `app/src/main/java/com/example/day/core/core_features/mcp/domain/repository/GitFileCacheRepository.kt`
- `app/src/main/java/com/example/day/core/core_features/mcp/domain/repository/FileAnalysisRepository.kt`

**Описание:** Repository интерфейсы для абстракции доступа к данным.

**Методы:**
```kotlin
// GitFileCacheRepository.kt
interface GitFileCacheRepository {
    suspend fun getCachedFileList(): GitFileCache?
    fun getCachedFileListAsFlow(): Flow<GitFileCache?>
    suspend fun cacheFileList(fileList: List<String>, ttlMinutes: Long)
    suspend fun clearCache()
    fun isCacheValid(): Boolean
}

// FileAnalysisRepository.kt
interface FileAnalysisRepository {
    suspend fun getAnalysis(filePath: String): FileAnalysis?
    suspend fun saveAnalysis(filePath: String, content: String)
    suspend fun deleteAnalysis(filePath: String)
}
```

---

### Шаг 1.9: Repository реализации

**Расположение:**
- `app/src/main/java/com/example/day/core/core_features/mcp/data/GitFileCacheRepositoryImpl.kt`
- `app/src/main/java/com/example/day/core/core_features/mcp/data/FileAnalysisRepositoryImpl.kt`

**Описание:** Реализация repository с использованием DAO и мапперов.

---

### Шаг 1.10: DI модули

**Расположение:** `app/src/main/java/com/example/day/core/core_features/mcp/di/McpCoreFeatureModule.kt`

**Описание:** Добавление绑定 в существующий Dagger модуль.

**Изменения:**
```kotlin
@Module
internal interface McpCoreFeatureModule {
    // ... существующие binds
    
    @Binds
    @Singleton
    fun bindsGitFileCacheRepository(impl: GitFileCacheRepositoryImpl): GitFileCacheRepository
    
    @Binds
    @Singleton
    fun bindsFileAnalysisRepository(impl: FileAnalysisRepositoryImpl): FileAnalysisRepository
    
    companion object {
        @Provides
        internal fun provideGitFileCacheDao(db: ChatDatabase): GitFileCacheDao = db.gitFileCacheDao()
        
        @Provides
        internal fun provideFileAnalysisDao(db: ChatDatabase): FileAnalysisDao = db.fileAnalysisDao()
    }
}
```

---

## Резюме этапа

**Что получим:**
- ✅ Room база данных с таблицами для кеширования
- ✅ DAO интерфейсы для CRUD операций
- ✅ Domain модели и мапперы
- ✅ Repository слой для абстракции доступа к данным
- ✅ DI конфигурация для новых компонентов

**Критерии успеха:**
1. Компиляция проекта без ошибок
2. Room создает новые таблицы при миграции (version 16)
3. Repository методы корректно сохраняют и извлекают данные
4. Flow обновляется при изменении данных в БД

---

## Зависимости от других этапов

Этап не зависит от других этапов и может быть выполнен первым.

---

## План реализации (подробный)

1. Создать GitFileCacheEntity.kt
2. Создать FileAnalysisEntity.kt
3. Создать GitFileCacheDao.kt
4. Создать FileAnalysisDao.kt
5. Обновить ChatDatabase.kt (version 16, новые DAO методы)
6. Создать domain модели GitFileCache и FileAnalysis
7. Создать мапперы
8. Создать Repository интерфейсы
9. Создать Repository реализации
10. Обновить McpCoreFeatureModule.kt
11. Собрать проект и проверить компиляцию
