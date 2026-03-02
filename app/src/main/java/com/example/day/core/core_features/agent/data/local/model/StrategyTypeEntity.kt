package com.example.day.core.core_features.agent.data.local.model

import androidx.annotation.Keep

/**
 * Типы стратегий управления контекстом.
 */
@Keep
enum class StrategyTypeEntity {
    /**
     * Стратегия скользящего окна - ограничивает количество сообщений.
     */
    SLIDING_WINDOW,

    /**
     * Стратегия саммаризации - сжимает старые сообщения.
     */
    SUMMARIZATION,

    /**
     * Полный контекст - сохраняет все сообщения без ограничений.
     */
    FULL_CONTEXT,

    /**
     * Стратегия Sticky Facts - хранит ключ-значение факты + последние N сообщений.
     */
    STICKY_FACTS,

    /**
     * Стратегия ветвления - позволяет создавать независимые ветки диалога.
     */
    BRANCHING
}