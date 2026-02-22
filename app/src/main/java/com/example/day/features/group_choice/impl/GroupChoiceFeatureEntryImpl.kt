package com.example.day.features.group_choice.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.app.di.LocalAppComponent
import com.example.day.features.group_choice.api.GroupChoiceFeatureEntry
import com.example.day.features.group_choice.impl.di.DaggerGroupChoiceFeatureComponent
import com.example.day.features.group_choice.impl.di.GroupChoiceFeatureComponent
import com.example.day.features.group_choice.impl.ui.GroupChoiceScreen
import com.example.day.features.group_choice.impl.ui.viewmodel.GroupChoiceViewModelImpl
import javax.inject.Inject

class GroupChoiceFeatureEntryImpl @Inject constructor() : GroupChoiceFeatureEntry {
    @Composable
    override fun EntryPoint(modifier: Modifier, onGroupSelected: (Long) -> Unit) {
        val appComponent = LocalAppComponent.current
        val featureComponent: GroupChoiceFeatureComponent = retain {
            DaggerGroupChoiceFeatureComponent.factory().create(appComponent)
        }
        val viewModel: GroupChoiceViewModelImpl = viewModel(
            factory = featureComponent.getViewModelFactory()
        )
        GroupChoiceScreen(viewModel = viewModel, onGroupSelected = onGroupSelected, modifier = modifier)
    }
}
