package com.example.day.core.ui.uikit.components.ltm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/**
 * Отображает список LTM-фактов, сгруппированных по категориям.
 *
 * @param facts список фактов для отображения
 * @param modifier модификатор
 * @param showPromptPreview показывать ли блок "как агент видит данные"
 * @param onEdit коллбэк редактирования; null — иконка не отображается
 * @param onDelete коллбэк удаления; null — иконка не отображается
 * @param onAdd коллбэк добавления нового факта; null — кнопка не отображается
 */
@Composable
fun LongTermFactsListView(
    facts: ImmutableList<LongTermFactUiItem>,
    modifier: Modifier = Modifier,
    showPromptPreview: Boolean = false,
    onEdit: ((LongTermFactUiItem) -> Unit)? = null,
    onDelete: ((LongTermFactUiItem) -> Unit)? = null,
    onAdd: (() -> Unit)? = null
) {
    if (facts.isEmpty() && onAdd == null) {
        EmptyFactsPlaceholder(modifier = modifier)
        return
    }

    val grouped = facts.groupBy { it.category }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showPromptPreview && facts.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            LtmPromptPreview(facts)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
        }

        grouped.forEach { (category, items) ->
            Text(
                text = category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
            )
            items.forEach { fact ->
                FactItem(fact = fact, onEdit = onEdit, onDelete = onDelete)
            }
        }

        if (facts.isEmpty()) {
            Text(
                text = "Фактов пока нет",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        if (onAdd != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text("Добавить факт")
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FactItem(
    fact: LongTermFactUiItem,
    onEdit: ((LongTermFactUiItem) -> Unit)?,
    onDelete: ((LongTermFactUiItem) -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fact.memoryKey,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = fact.fact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            if (onEdit != null) {
                IconButton(onClick = { onEdit(fact) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { onDelete(fact) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun LtmPromptPreview(facts: ImmutableList<LongTermFactUiItem>) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Как агент видит эти данные в промпте:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            facts.forEach { fact ->
                Text(
                    text = "- [${fact.category}] ${fact.memoryKey}: ${fact.fact}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun EmptyFactsPlaceholder(modifier: Modifier = Modifier) {
    Text(
        text = "Фактов пока нет.\nАгент сохранит их сам — например:\nSAVE_FACT[primary_language:skills:Kotlin]",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}
