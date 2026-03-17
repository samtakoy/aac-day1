package com.example.day.ragserver.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.ByteBuffer

object CodeChunksTable : Table("code_chunks") {
    val id = long("id").autoIncrement()
    val content = text("content")
    val filePath = text("file_path")
    val fileName = varchar("file_name", 255)
    val packageName = varchar("package_name", 200).default("")
    val declarationName = varchar("declaration_name", 200).nullable()
    val startLine = integer("start_line").default(0)
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

class CodeDatabase(private val dbPath: String) {

    fun connect() {
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(CodeChunksTable, CodeVectorsTable)
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
            it[startLine] = entity.startLine
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
                    startLine = row[CodeChunksTable.startLine],
                    strategy = row[CodeChunksTable.strategy],
                    chunkOrder = row[CodeChunksTable.chunkOrder],
                    indexedAt = row[CodeChunksTable.indexedAt],
                )
                val bytes = row[CodeVectorsTable.embedding].bytes
                val vector = bytes.toFloatArray2()
                entity to vector
            }
    }

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
