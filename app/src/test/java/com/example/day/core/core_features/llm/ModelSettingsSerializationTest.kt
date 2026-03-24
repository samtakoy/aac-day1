package com.example.day.core.core_features.llm

import com.example.day.core.core_features.llm.data.local.mapper.ModelSettingsMapper
import com.example.day.core.core_features.llm.data.local.model.ModelSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModelSettingsSerializationTest {

    private val mapper = ModelSettingsMapper()

    @Test
    fun `isLocal=true round-trips through JSON`() {
        val entity = ModelSettingsEntity(name = "llama3", isLocal = true)
        val domain = mapper.toDomain(entity)
        val json = mapper.toJson(domain)
        val decoded = mapper.fromJson(json)
        assertEquals(true, decoded.isLocal)
    }

    @Test
    fun `old JSON without isLocal field deserializes with isLocal=false`() {
        val oldJson = """{"name":"gpt-4","stopSequence":[],"jsonFormat":false}"""
        val decoded = mapper.fromJson(oldJson)
        assertFalse(decoded.isLocal)
    }

    @Test
    fun `isLocal maps through toDomain and toEntity`() {
        val entity = ModelSettingsEntity(name = "mistral", isLocal = true)
        val domain = mapper.toDomain(entity)
        val backToEntity = mapper.toEntity(domain)
        assertEquals(true, backToEntity.isLocal)
    }
}
