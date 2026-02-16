package com.example.day.core.feature_entries

interface FeatureEntryProvider {
    // Получаем карту всех зарегистрированных фич
    fun getFeatureEntries(): Map<Class<out FeatureEntry>, FeatureEntry>
}

/**
 * Находит фичу в мапе и сразу приводит её к нужному типу интерфейса.
 */
inline fun <reified T : FeatureEntry> Map<Class<out FeatureEntry>, FeatureEntry>.find(): T {
    val key = T::class.java
    val entry = this[key]
        ?: error("Feature entry for ${key.simpleName} not found. Check if the module is added to AppComponent.")

    return entry as T
}