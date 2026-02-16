package com.example.day.core.feature_entries

import dagger.MapKey
import kotlin.reflect.KClass

@MapKey
annotation class FeatureEntryKey(val value: KClass<out FeatureEntry>)