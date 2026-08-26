package me.nanova.summaryexpressive.ui.page.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeFloatingActionButtons(
    fabVisible: Boolean,
    onPaste: () -> Unit,
    onSummarize: () -> Unit,
    isLoading: Boolean,
    hasResult: Boolean,
    isDirty: Boolean,
    onShowSnackBar: (String) -> Unit,
    onLaunchFilePicker: () -> Unit,
    onLaunchImagePicker: () -> Unit,
    onLaunchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stillLoading = stringResource(id = R.string.stillLoading)
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(menuExpanded) { menuExpanded = false }

    val attachmentItems = listOf(
        Triple(Icons.Rounded.Image, "Image", onLaunchImagePicker),
        Triple(Icons.Rounded.CameraAlt, "Camera", onLaunchCamera),
        Triple(Icons.Rounded.Description, "Document", onLaunchFilePicker)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButtonMenu(
            expanded = menuExpanded,
            button = {
                ToggleFloatingActionButton(
                    checked = menuExpanded,
                    onCheckedChange = { if (!isLoading) menuExpanded = !menuExpanded },
                    modifier = Modifier
                        .semantics {
                            stateDescription = if (menuExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Toggle attachments menu"
                        }
                        .animateFloatingActionButton(
                            visible = fabVisible,
                            alignment = Alignment.BottomEnd
                        )
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = "More actions",
                        modifier = Modifier.animateIcon({ checkedProgress }),
                    )
                }
            },
        ) {
            attachmentItems.forEach { (icon, text, onClick) ->
                FloatingActionButtonMenuItem(
                    icon = { Icon(icon, contentDescription = null) },
                    text = { Text(text) },
                    onClick = {
                        onClick()
                        menuExpanded = false
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { if (!isLoading) onPaste() },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .animateFloatingActionButton(
                    visible = fabVisible && !menuExpanded,
                    alignment = Alignment.BottomEnd
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentPaste,
                contentDescription = "Paste from clipboard",
            )
        }

        FloatingActionButton(
            onClick = {
                if (isLoading) onShowSnackBar(stillLoading)
                else onSummarize()
            },
            modifier = Modifier.animateFloatingActionButton(
                visible = fabVisible && !menuExpanded,
                alignment = Alignment.TopStart
            )
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else if (hasResult && !isDirty) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.regenerate)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Summarize"
                )
            }
        }
    }
}

@Preview
@Composable
private fun HomeFloatingActionButtonsPreview() {
    SummaryExpressiveTheme {
        HomeFloatingActionButtons(
            fabVisible = true,
            onPaste = {},
            onSummarize = {},
            isLoading = false,
            hasResult = false,
            isDirty = false,
            onShowSnackBar = {},
            onLaunchFilePicker = {},
            onLaunchImagePicker = {},
            onLaunchCamera = {}
        )
    }
}
