package me.nanova.summaryexpressive.ui.page.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.ProviderConfig
import me.nanova.summaryexpressive.ui.component.ClickablePasteIcon
import me.nanova.summaryexpressive.ui.component.ReorderDragHandle
import me.nanova.summaryexpressive.ui.component.ReorderableItem
import me.nanova.summaryexpressive.ui.component.ReorderableListState
import me.nanova.summaryexpressive.ui.component.rememberReorderableListState
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

@Composable
fun ModelSettingsDialog(
    onDismissRequest: () -> Unit,
    provider: AIProvider,
    initialModelId: String?,
    providerConfigs: Map<String, ProviderConfig>,
    onConfirm: (models: List<String>, selectedModel: String) -> Unit,
) {
    val initialEffectiveModels = remember(provider, providerConfigs) {
        val config = providerConfigs[provider.name]
        provider.getEffectiveModels(config)
    }
    val savedModelsList = remember(initialEffectiveModels) { initialEffectiveModels }
    var modelsList by remember(initialEffectiveModels) { mutableStateOf(initialEffectiveModels) }

    val savedSelectedModel = remember(initialModelId, initialEffectiveModels) {
        initialModelId?.takeIf { it in initialEffectiveModels }
            ?: initialEffectiveModels.firstOrNull()
            ?: ""
    }
    var selectedModel by remember(savedSelectedModel) { mutableStateOf(savedSelectedModel) }

    var newModelText by remember { mutableStateOf("") }
    var editingModelIndex by remember { mutableStateOf<Int?>(null) }
    var editingModelText by remember { mutableStateOf("") }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isChanged = modelsList != savedModelsList || selectedModel != savedSelectedModel

    val reorderState = rememberReorderableListState(lazyListState) { fromIndex, toIndex ->
        val updatedList = modelsList.toMutableList()
        val item = updatedList.removeAt(fromIndex)
        updatedList.add(toIndex, item)
        modelsList = updatedList
    }

    val addModel = {
        val trimmed = newModelText.trim()
        if (trimmed.isNotBlank() && !modelsList.contains(trimmed)) {
            modelsList = listOf(trimmed) + modelsList
            selectedModel = trimmed
            newModelText = ""
            scope.launch {
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(id = R.string.manageModels) + " (${provider.id.display})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp)) {
                // Add model input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newModelText,
                        onValueChange = { newModelText = it },
                        label = { Text(stringResource(R.string.addModel)) },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            ClickablePasteIcon(
                                text = newModelText,
                                onPaste = { newModelText = it.trim() },
                                onClear = { newModelText = "" }
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addModel() })
                    )
                    FilledTonalIconButton(
                        onClick = addModel,
                        enabled = newModelText.isNotBlank(),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.addModel)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (modelsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No models configured",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(modelsList, key = { _, item -> item }) { index, model ->
                            ModelListItem(
                                model = model,
                                isSelected = selectedModel == model,
                                isEditing = editingModelIndex == index,
                                editingText = editingModelText,
                                reorderState = reorderState,
                                onSelect = { selectedModel = model },
                                onStartEdit = {
                                    editingModelIndex = index
                                    editingModelText = model
                                },
                                onEditingTextChange = { editingModelText = it },
                                onSaveEdit = { text ->
                                    val trimmed = text.trim()
                                    if (trimmed.isNotBlank()) {
                                        val updatedList = modelsList.toMutableList()
                                        val oldModel = updatedList[index]
                                        updatedList[index] = trimmed
                                        modelsList = updatedList
                                        if (selectedModel == oldModel) {
                                            selectedModel = trimmed
                                        }
                                    }
                                    editingModelIndex = null
                                },
                                onCancelEdit = { editingModelIndex = null },
                                onDelete = {
                                    val updatedList = modelsList.toMutableList()
                                    val removed = updatedList.removeAt(index)
                                    modelsList = updatedList
                                    if (selectedModel == removed) {
                                        selectedModel = updatedList.getOrNull(
                                            index.coerceAtMost(updatedList.size - 1)
                                        ) ?: ""
                                    }
                                    if (editingModelIndex == index) {
                                        editingModelIndex = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(modelsList, selectedModel)
                },
                enabled = modelsList.isNotEmpty() && selectedModel.isNotBlank() && selectedModel in modelsList
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        modelsList = savedModelsList
                        selectedModel = savedSelectedModel
                        editingModelIndex = null
                    },
                    enabled = isChanged
                ) {
                    Text(stringResource(id = R.string.reset))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun LazyItemScope.ModelListItem(
    model: String,
    isSelected: Boolean,
    isEditing: Boolean,
    editingText: String,
    reorderState: ReorderableListState,
    onSelect: () -> Unit,
    onStartEdit: () -> Unit,
    onEditingTextChange: (String) -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ReorderableItem(
        reorderState = reorderState,
        key = model,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            ),
            border = if (isSelected) {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isEditing) {
                ModelEditRow(
                    text = editingText,
                    onTextChange = onEditingTextChange,
                    onSave = { onSaveEdit(editingText) },
                    onCancel = onCancelEdit
                )
            } else {
                ModelDisplayRow(
                    model = model,
                    isSelected = isSelected,
                    reorderState = reorderState,
                    onSelect = onSelect,
                    onStartEdit = onStartEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun ModelEditRow(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            shape = MaterialTheme.shapes.medium
        )
        IconButton(
            onClick = onSave,
            enabled = text.isNotBlank(),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Save edit",
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Cancel edit",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModelDisplayRow(
    model: String,
    isSelected: Boolean,
    reorderState: ReorderableListState,
    onSelect: () -> Unit,
    onStartEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clickable(onClick = onSelect)
                .padding(
                    start = 8.dp,
                    end = 4.dp,
                    top = 6.dp,
                    bottom = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = model,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            )
            IconButton(
                onClick = onStartEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.editModel),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.deleteModel),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            ReorderDragHandle(
                reorderState = reorderState,
                key = model,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ModelSettingsDialogPreview() {
    SummaryExpressiveTheme {
        ModelSettingsDialog(
            onDismissRequest = {},
            provider = AIProvider.OPENAI,
            initialModelId = AIProvider.OPENAI.models.first().id,
            providerConfigs = emptyMap(),
            onConfirm = { _, _ -> },
        )
    }
}
