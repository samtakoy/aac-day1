package com.example.day.ragserver.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.ByteBuffer
import java.time.Instant

object CodeChunksTable : Table("code_chunks") {
    val id = long("id").autoIncrement()
    val content = text("content")
    val filePath = text("file_path")
    val fileName = varchar("file_name", 255)
    val packageName = varchar("package_name", 200).default("")
    val declarationName = varchar("declaration_name", 200).nullable()
    val parentScope = varchar("parent_scope", 255).nullable()
    val contextPath = varchar("context_path", 500).nullable()
    val startLine = integer("start_line").default(0)
    val nodeType = varchar("node_type", 50).nullable()
    val strategy = varchar("strategy", 50)
    val chunkOrder = integer("chunk_order")
    val indexedAt = varchar("indexed_at", 50)
    override val primaryKey = PrimaryKey(id)
}

object CodeVectorsTable : Table("code_vectors") {
    val chunkId = long("chunk_id").references(CodeChunksTable.id)
    val embedding = blob("embedding")
    override val primaryKey = PrimaryKey(chunkId)
}

object ClassMetadataTable : Table("class_metadata") {
    val id = long("id").autoIncrement()
    val className = varchar("class_name", 200)
    val filePath = text("file_path")
    val metadataJson = text("metadata_json")
    val indexedAt = varchar("indexed_at", 50)
    override val primaryKey = PrimaryKey(id)
}

// Векторы эмбеддингов для поля responsibility каждого класса.
// Хранятся отдельно от текстовых метаданных — разные жизненные циклы:
// метаданные генерирует LLM, векторы — embedding-модель.
// Это позволяет пересчитывать только векторы при смене embedding-модели,
// не затрагивая дорогостоящую LLM-генерацию.
object ClassMetadataVectorsTable : Table("class_metadata_vectors") {
    val className = varchar("class_name", 255)
    val vector = blob("vector")
    override val primaryKey = PrimaryKey(className)
}

class CodeDatabase(private val dbPath: String) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun connect() {
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                CodeChunksTable,
                CodeVectorsTable,
                ClassMetadataTable,
                ClassMetadataVectorsTable,
            )
        }
        println("CodeDatabase connected: $dbPath")
    }

    fun hasIndex(strategy: String): Boolean = transaction {
        CodeChunksTable.selectAll()
            .where { CodeChunksTable.strategy eq strategy }
            .count() > 0
    }

    fun clearIndex(strategy: String) = transaction {
        // Delete vectors for chunks of this strategy via subquery-style manual iteration
        val ids = CodeChunksTable
            .selectAll()
            .where { CodeChunksTable.strategy eq strategy }
            .map { it[CodeChunksTable.id] }
        ids.forEach { id ->
            CodeVectorsTable.deleteWhere { chunkId eq id }
        }
        CodeChunksTable.deleteWhere { CodeChunksTable.strategy eq strategy }
    }

    fun saveChunk(entity: ChunkEntity, embedding: FloatArray): Long = transaction {
        val id = CodeChunksTable.insert {
            it[content] = entity.content
            it[filePath] = entity.filePath
            it[fileName] = entity.fileName
            it[packageName] = entity.packageName
            it[declarationName] = entity.declarationName
            it[parentScope] = entity.parentScope
            it[contextPath] = entity.contextPath
            it[startLine] = entity.startLine
            it[nodeType] = entity.nodeType
            it[strategy] = entity.strategy
            it[chunkOrder] = entity.chunkOrder
            it[indexedAt] = entity.indexedAt
        } get CodeChunksTable.id

        CodeVectorsTable.insert {
            it[chunkId] = id
            it[CodeVectorsTable.embedding] = ExposedBlob(embedding.toByteArray())
        }
        id
    }

    fun getAllVectors(strategy: String): List<Pair<ChunkEntity, FloatArray>> = transaction {
        (CodeChunksTable innerJoin CodeVectorsTable)
            .selectAll()
            .where { CodeChunksTable.strategy eq strategy }
            .map { row ->
                val entity = ChunkEntity(
                    id = row[CodeChunksTable.id],
                    content = row[CodeChunksTable.content],
                    filePath = row[CodeChunksTable.filePath],
                    fileName = row[CodeChunksTable.fileName],
                    packageName = row[CodeChunksTable.packageName],
                    declarationName = row[CodeChunksTable.declarationName],
                    parentScope = row[CodeChunksTable.parentScope],
                    contextPath = row[CodeChunksTable.contextPath],
                    startLine = row[CodeChunksTable.startLine],
                    nodeType = row[CodeChunksTable.nodeType],
                    strategy = row[CodeChunksTable.strategy],
                    chunkOrder = row[CodeChunksTable.chunkOrder],
                    indexedAt = row[CodeChunksTable.indexedAt],
                )
                val bytes = row[CodeVectorsTable.embedding].bytes
                val vector = bytes.toFloatArray2()
                entity to vector
            }
    }

    fun getChunksByFile(filePath: String, strategy: String): List<ChunkEntity> = transaction {
        CodeChunksTable.selectAll()
            .where { (CodeChunksTable.filePath eq filePath) and (CodeChunksTable.strategy eq strategy) }
            .map { row ->
                ChunkEntity(
                    id = row[CodeChunksTable.id],
                    content = row[CodeChunksTable.content],
                    filePath = row[CodeChunksTable.filePath],
                    fileName = row[CodeChunksTable.fileName],
                    packageName = row[CodeChunksTable.packageName],
                    declarationName = row[CodeChunksTable.declarationName],
                    parentScope = row[CodeChunksTable.parentScope],
                    contextPath = row[CodeChunksTable.contextPath],
                    startLine = row[CodeChunksTable.startLine],
                    nodeType = row[CodeChunksTable.nodeType],
                    strategy = row[CodeChunksTable.strategy],
                    chunkOrder = row[CodeChunksTable.chunkOrder],
                    indexedAt = row[CodeChunksTable.indexedAt],
                )
            }
    }

    // --- Методы для текстовых метаданных классов ---

    fun hasClassMetadata(className: String): Boolean = transaction {
        ClassMetadataTable.selectAll()
            .where { ClassMetadataTable.className eq className }
            .count() > 0
    }

    fun saveClassMetadata(metadata: ClassMetadata, filePath: String) = transaction {
        ClassMetadataTable.deleteWhere { className eq metadata.className }
        ClassMetadataTable.insert {
            it[className] = metadata.className
            it[ClassMetadataTable.filePath] = filePath
            it[metadataJson] = Json.encodeToString(metadata)
            it[indexedAt] = Instant.now().toString()
        }
    }

    fun deleteClassMetadata(classNames: List<String>): Int = transaction {
        val deleted = ClassMetadataTable.deleteWhere { className inList classNames }
        ClassMetadataVectorsTable.deleteWhere { ClassMetadataVectorsTable.className inList classNames }
        deleted
    }

    fun getClassMetadata(name: String): ClassMetadata? = transaction {
        ClassMetadataTable.selectAll()
            .where { ClassMetadataTable.className eq name }
            .firstOrNull()
            ?.let { json.decodeFromString<ClassMetadata>(it[ClassMetadataTable.metadataJson]) }
    }

    // Возвращает первую запись метаданных для файла по его полному пути.
    // Используется в ContextPacker для точного lookup без коллизий по имени класса.
    // Ограничение: файлы с несколькими top-level классами вернут только первую запись.
    // Исправление (Вариант C): getClassMetadataListByFilePath() + List<ClassMetadata> в ClassGroup.
    fun getClassMetadataByFilePath(filePath: String): ClassMetadata? = transaction {
        ClassMetadataTable.selectAll()
            .where { ClassMetadataTable.filePath eq filePath }
            .firstOrNull()
            ?.let { json.decodeFromString<ClassMetadata>(it[ClassMetadataTable.metadataJson]) }
    }

    fun getAllClassMetadata(): List<ClassMetadata> = transaction {
        ClassMetadataTable.selectAll()
            .map { json.decodeFromString<ClassMetadata>(it[ClassMetadataTable.metadataJson]) }
    }

    fun getAllClassMetadataWithPaths(): List<Pair<ClassMetadata, String>> = transaction {
        ClassMetadataTable.selectAll()
            .map { row ->
                val metadata = json.decodeFromString<ClassMetadata>(row[ClassMetadataTable.metadataJson])
                val filePath = row[ClassMetadataTable.filePath]
                metadata to filePath
            }
    }

    // --- Методы для векторов метаданных (Stage 1 поиска) ---

    fun hasMetadataVector(className: String): Boolean = transaction {
        ClassMetadataVectorsTable.selectAll()
            .where { ClassMetadataVectorsTable.className eq className }
            .count() > 0
    }

    fun saveMetadataVector(className: String, vector: FloatArray) = transaction {
        ClassMetadataVectorsTable.deleteWhere { ClassMetadataVectorsTable.className eq className }
        ClassMetadataVectorsTable.insert {
            it[ClassMetadataVectorsTable.className] = className
            it[ClassMetadataVectorsTable.vector] = ExposedBlob(vector.toByteArray())
        }
    }

    fun getAllMetadataVectors(): List<Pair<String, FloatArray>> = transaction {
        ClassMetadataVectorsTable.selectAll().map { row ->
            val name = row[ClassMetadataVectorsTable.className]
            val vector = row[ClassMetadataVectorsTable.vector].bytes.toFloatArray2()
            name to vector
        }
    }

    // --- Debug / диагностика ---

    /** Количество чанков по каждому файлу для заданной стратегии. */
    fun getChunkCountsByFile(strategy: String): Map<String, Int> = transaction {
        CodeChunksTable
            .selectAll()
            .where { CodeChunksTable.strategy eq strategy }
            .map { it[CodeChunksTable.fileName] }
            .groupingBy { it }
            .eachCount()
    }

    /** Все чанки (без векторов) для конкретного файла. */
    fun getChunksByFileName(fileName: String, strategy: String): List<ChunkEntity> = transaction {
        CodeChunksTable.selectAll()
            .where { (CodeChunksTable.fileName eq fileName) and (CodeChunksTable.strategy eq strategy) }
            .map { row ->
                ChunkEntity(
                    id = row[CodeChunksTable.id],
                    content = row[CodeChunksTable.content],
                    filePath = row[CodeChunksTable.filePath],
                    fileName = row[CodeChunksTable.fileName],
                    packageName = row[CodeChunksTable.packageName],
                    declarationName = row[CodeChunksTable.declarationName],
                    parentScope = row[CodeChunksTable.parentScope],
                    contextPath = row[CodeChunksTable.contextPath],
                    startLine = row[CodeChunksTable.startLine],
                    nodeType = row[CodeChunksTable.nodeType],
                    strategy = row[CodeChunksTable.strategy],
                    chunkOrder = row[CodeChunksTable.chunkOrder],
                    indexedAt = row[CodeChunksTable.indexedAt],
                )
            }
    }

    // --- Статистика ---

    fun getStats(): IndexStats {
        val total = transaction { CodeChunksTable.selectAll().count().toInt() }
        val structural = transaction {
            CodeChunksTable.selectAll()
                .where { CodeChunksTable.strategy eq "structural" }
                .count().toInt()
        }
        val fixed = transaction {
            CodeChunksTable.selectAll()
                .where { CodeChunksTable.strategy eq "fixed" }
                .count().toInt()
        }
        val maxIndexedAt = CodeChunksTable.indexedAt.max()
        val lastIndexed = transaction {
            CodeChunksTable
                .select(maxIndexedAt)
                .firstOrNull()
                ?.getOrNull(maxIndexedAt)
        }
        return IndexStats(
            totalChunks = total,
            structuralChunks = structural,
            fixedChunks = fixed,
            indexedAt = lastIndexed,
            isReady = structural > 0 && fixed > 0,
        )
    }
}

fun FloatArray.toByteArray(): ByteArray {
    val buf = ByteBuffer.allocate(this.size * 4)
    this.forEach { buf.putFloat(it) }
    return buf.array()
}

fun ByteArray.toFloatArray2(): FloatArray {
    val buf = ByteBuffer.wrap(this)
    return FloatArray(this.size / 4) { buf.float }
}
