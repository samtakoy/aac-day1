package com.example.day.features.console.impl.di

import io.ktor.client.HttpClient
import kotlin.properties.Delegates.notNull

interface ConsoleFeatureDeps {
    fun httpClient(): HttpClient
}

interface ConsoleFeatureDepsProvider {
    val deps: ConsoleFeatureDeps

    companion object: ConsoleFeatureDepsProvider by ConsoleFeatureDepsStore
}

object ConsoleFeatureDepsStore : ConsoleFeatureDepsProvider {
    override var deps: ConsoleFeatureDeps by notNull()
}