package me.nanova.summaryexpressive.ui.page.settings.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

@Composable
fun ThemeSettingsDialog(
    onDismissRequest: () -> Unit,
    currentTheme: Int,
    onThemeChange: (Int) -> Unit,
) {
    var theme by remember { mutableIntStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.theme)) },
        text = {
            Column {
                RadioButtonItem(
                    selected = theme == 0,
                    onSelectionChange = { theme = 0 },
                ) {
                    Text(
                        text = stringResource(id = R.string.systemTheme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                RadioButtonItem(
                    selected = theme == 2,
                    onSelectionChange = { theme = 2 }
                ) {
                    Text(
                        text = stringResource(id = R.string.lightTheme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                RadioButtonItem(
                    selected = theme == 1,
                    onSelectionChange = { theme = 1 }
                ) {
                    Text(
                        text = stringResource(id = R.string.darkTheme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onThemeChange(theme)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
fun RadioButtonItem(
    selected: Boolean,
    onSelectionChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelectionChange,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        content()
    }
}

@Preview
@Composable
private fun ThemeSettingsDialogPreview() {
    SummaryExpressiveTheme {
        ThemeSettingsDialog(
            onDismissRequest = {},
            currentTheme = 0,
            onThemeChange = {}
        )
    }
}

@Preview
@Composable
private fun RadioButtonItemPreview() {
    SummaryExpressiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RadioButtonItem(selected = true, onSelectionChange = {}) {
                Text("Option 1")
            }
            RadioButtonItem(selected = false, onSelectionChange = {}) {
                Text("Option 2")
            }
        }
    }
}
