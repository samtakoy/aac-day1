package com.example.day.ragserver.indexing

import com.example.day.ragserver.db.ClassMetadata

object MetadataValidator {

    fun validate(metadata: ClassMetadata): ClassMetadata? {
        if (metadata.className.isBlank()) return null
        return metadata.copy(
            className = metadata.className.trim(),
            responsibility = metadata.responsibility.trim().take(200),
            dependencies = metadata.dependencies
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length < 100 },
            keyMethods = metadata.keyMethods
                .take(10)
                .map { it.copy(
                    name = it.name.trim(),
                    description = it.description.trim().take(150),
                    params = it.params?.trim()?.take(200),
                ) }
                .filter { it.name.isNotBlank() },
            domainTags = metadata.domainTags
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5),
            usedBy = metadata.usedBy
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length < 100 },
        )
    }
}
