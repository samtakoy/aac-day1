package com.example.day.ragserver.indexing

import com.example.day.ragserver.db.ChunkEntity

interface ChunkingStrategy {
    val strategyName: String
    fun split(content: String, filePath: String, fileName: String): List<ChunkEntity>
}
