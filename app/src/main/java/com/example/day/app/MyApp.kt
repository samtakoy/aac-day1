package com.example.day.app

import android.app.Application
import com.example.day.app.di.AppComponent
import com.example.day.app.di.DaggerAppComponent
import com.example.day.features.console.impl.di.ConsoleFeatureDeps
import com.example.day.features.console.impl.di.ConsoleFeatureDepsStore


class MyApp : Application() {
    val appComponent: AppComponent by lazy {
        DaggerAppComponent.create()
    }

    override fun onCreate() {
        super.onCreate()
        ConsoleFeatureDepsStore.deps = appComponent
    }
}
