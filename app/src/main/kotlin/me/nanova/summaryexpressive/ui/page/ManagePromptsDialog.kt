package me.nanova.summaryexpressive.ui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.model.CustomPrompt
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePromptsSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    prompts: List<CustomPrompt>,
    onAddPrompt: (CustomPrompt) -> Unit,
    onEditPrompt: (CustomPrompt) -> Unit,
    onDeletePrompt: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<CustomPrompt?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Prompt")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Manage Custom Prompts",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                if (prompts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No custom prompts yet.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(prompts) { prompt ->
                            PromptItem(
                                prompt = prompt,
                                onEdit = { showEditDialog = prompt },
                                onDelete = { onDeletePrompt(prompt.id) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showAddDialog) {
        PromptEditorDialog(
            title = "Add New Prompt",
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content ->
                onAddPrompt(CustomPrompt(id = UUID.randomUUID().toString(), title = title, content = content))
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { prompt ->
        PromptEditorDialog(
            title = "Edit Prompt",
            initialTitle = prompt.title,
            initialContent = prompt.content,
            onDismiss = { showEditDialog = null },
            onConfirm = { title, content ->
                onEditPrompt(prompt.copy(title = title, content = content))
                showEditDialog = null
            }
        )
    }
}

@Composable
fun PromptItem(
    prompt: CustomPrompt,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = prompt.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = prompt.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun PromptEditorDialog(
    title: String,
    initialTitle: String = "",
    initialContent: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var promptTitle by remember { mutableStateOf(initialTitle) }
    var promptContent by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = promptTitle,
                    onValueChange = { promptTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = promptContent,
                    onValueChange = { promptContent = it },
                    label = { Text("Prompt Content") },
                    minLines = 3,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(promptTitle, promptContent) },
                enabled = promptTitle.isNotBlank() && promptContent.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
