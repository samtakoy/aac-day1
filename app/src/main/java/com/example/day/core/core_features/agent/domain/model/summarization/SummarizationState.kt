package com.example.day.core.core_features.agent.domain.model.summarization

// TODO все переделать - УДАЛИТЬ

/**
 * Интерфейс состояния стратегии сжатия контекста.
 * 
 * Инкапсулирует и тип стратегии и её параметры.
 * Позволяет легко менять стратегию заменой объекта.
 */
sealed interface SummarizationState {
    
    /**
     * Уникальное имя стратегии
     */
    val strategyName: String
    
    /**
     * Проверить, нужно ли сжатие при текущем количестве пар сообщений
     * @param messageCount количество сообщений
     * @return true если нужно сжатие
     */
    fun shouldCompress(messageCount: Int): Boolean
    
    /**
     * Получить threshold (порог) для сжатия
     * @return количество сообщений при котором происходит сжатие
     */
    fun getCompressionThreshold(): Int

    /**
     * Создать копию с обновлённым summary
     */
    fun withSummary(newSummary: String?): SummarizationState
    
    /**
     * Получить текущее summary
     */
    fun retrieveSummary(): String?
    
    companion object {
        /**
         * Создать состояние с отключённым сжатием (по умолчанию)
         */
        fun disabled(): SummarizationState = NoSummarizationState
        
        /**
         * Создать состояние с включённым сжатием
         */
        fun enabled(msgLimit: Int, extraLimit: Int): SummarizationState =
            SummarizationEnabledState(msgLimit, extraLimit, null)
    }
}

/**
 * Состояние с отключённым сжатием (по умолчанию)
 */
object NoSummarizationState : SummarizationState {
    override val strategyName: String = "none"
    
    override fun shouldCompress(messageCount: Int): Boolean = false
    
    override fun getCompressionThreshold(): Int = Int.MAX_VALUE
    
    override fun withSummary(newSummary: String?): SummarizationState = this
    
    override fun retrieveSummary(): String? = null
    
    override fun toString(): String = "NoSummarizationState"
}

/**
 * Состояние с включённым сжатием (суммаризация)
 */
data class SummarizationEnabledState(
    val msgLimit: Int,           // сколько пар хранить как есть
    val extraLimit: Int,   // сколько пар накопить для сжатия
    val summary: String?     // текущее summary
) : SummarizationState {
    
    override val strategyName: String = "summarization"
    
    override fun shouldCompress(messageCount: Int): Boolean {
        return messageCount >= getCompressionThreshold()
    }
    
    override fun getCompressionThreshold(): Int = msgLimit + extraLimit
    
    override fun withSummary(newSummary: String?): SummarizationState {
        return copy(summary = newSummary)
    }
    
    override fun retrieveSummary(): String? = summary
    
    override fun toString(): String = "SummarizationEnabledState(msgLimit=$msgLimit, extraLimit=$extraLimit, summaryLength=${summary?.length ?: 0})"
}
