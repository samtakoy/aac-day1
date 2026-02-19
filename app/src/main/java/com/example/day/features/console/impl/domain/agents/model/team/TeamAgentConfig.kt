package com.example.day.features.console.impl.domain.agents.model.team

/**
 * Командный агент - описание.
 * @property id уникальный идентификатор (автоинкремент)
 * @property modelName имя облачной модели
 * @property teamRole роль в команде агентов, например "Senior Android разработчик"
 * */
data class TeamAgentConfig(
    val id: Long,
    val modelName: String,
    val teamRole: String,
)