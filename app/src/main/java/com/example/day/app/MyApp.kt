package com.example.day.app

import android.app.Application
import com.example.day.app.di.AppComponent
import com.example.day.app.di.DaggerAppComponent

class MyApp : Application() {
    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }
}
