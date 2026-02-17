package com.example.day.features.console.impl.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel.State

@Composable
internal fun ConsoleScreen(
    viewModel: ConsoleViewModel,
    modifier: Modifier
) {
    val state = viewModel.getStateAsFlow().collectAsStateWithLifecycle().value
    var inputText by remember { mutableStateOf(state.inputInitialValue) }

    ConsoleScreenInternal(
        state = state,
        inputText = inputText,
        onInputChanged = { newText ->
            inputText = newText
            viewModel.onEvent(ConsoleViewModel.Event.InputChanged(newText))
        },
        onSubmitClick = {
            viewModel.onEvent(ConsoleViewModel.Event.SubmitButtonClick(inputText))
        },
        modifier = modifier
    )
}

@Composable
private fun ConsoleScreenInternal(
    state: State,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Text Field с иконкой очистки
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Введите запрос") },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { onInputChanged("") }) {
                            Icon(
                                painter = painterResource(
                                    id = android.R.drawable.ic_menu_close_clear_cancel
                                ),
                                contentDescription = "Очистить"
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSubmitClick() }
                )
            )

            // Кнопка отправки
            Button(
                onClick = onSubmitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = state.type != State.Type.Loading && inputText.isNotBlank()
            ) {
                if (state.type == State.Type.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Отправить")
                }
            }

            // Текст результата
            val context = LocalContext.current
            Text(
                text = state.response,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState()),
                color = when (state.type) {
                    is State.Type.Error -> Color(0xFFEE6666)
                    is State.Type.Data -> Color(0xFF90EE90)
                    State.Type.Loading -> Color(0xFF666666)
                },
                style = MaterialTheme.typography.bodyLarge
            )

            if (state.type != State.Type.Loading && state.response.isNotBlank()) {
                Button(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("LLM Response", state.response)
                        clipboard.setPrimaryClip(clip)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text("Копировать результат в буфер")
                }
            }
        }
    }
}