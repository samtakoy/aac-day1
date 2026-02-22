package com.example.day.core.core_features.chat.domain.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

object ChatGroupColors {
    val colors = listOf(
        Color(0xFF42A5F5),  // Синий (первый цвет по умолчанию)
        Color(0xFF66BB6A),  // Зеленый
        Color(0xFFFFCA28),  // Желтый
        Color(0xFFAB47BC),  // Фиолетовый
        Color(0xFF26A69A),  // Бирюзовый
        Color(0xFFFF7043),  // Оранжевый
        Color(0xFF78909C),  // Серый
        Color(0xFFEC407A),  // Розовый
    )

    fun getColor(colorIndex: Int): Color {
        return colors[colorIndex % colors.size]
    }

    fun getRandomColorIndex(): Int {
        return Random.nextInt(colors.size)
    }
}
