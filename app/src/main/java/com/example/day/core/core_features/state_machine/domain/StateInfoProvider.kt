package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.state_machine.domain.model.StateId

interface StateInfoProvider {
    fun getStateDescription(state: StateId?, stepNum: Int, totalSteps: Int): String
}