package me.nanova.summaryexpressive.ui.page.home.action

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    hasInput: Boolean,
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

    LaunchedEffect(fabVisible) {
        if (!fabVisible) {
            menuExpanded = false
        }
    }

    BackHandler(menuExpanded) { menuExpanded = false }

    val attachmentItems = listOf(
        Triple(Icons.Rounded.Image, stringResource(id = R.string.image), onLaunchImagePicker),
        Triple(Icons.Rounded.CameraAlt, stringResource(id = R.string.camera), onLaunchCamera),
        Triple(
            Icons.Rounded.Description,
            stringResource(id = R.string.document),
            onLaunchFilePicker
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attachment FAB Menu
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
                        contentDescription = stringResource(id = R.string.more_actions),
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

        // Main Action Large FAB: Toggles dynamically between Paste (empty) and Submit/Summarize (content)
        LargeFloatingActionButton(
            onClick = {
                when {
                    isLoading -> onShowSnackBar(stillLoading)
                    !hasInput -> onPaste()
                    else -> onSummarize()
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = if (!hasInput) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (!hasInput) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier.animateFloatingActionButton(
                visible = fabVisible && !menuExpanded,
                alignment = Alignment.BottomEnd
            )
        ) {
            AnimatedContent(
                targetState = Triple(hasInput, isLoading, hasResult && !isDirty),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "fab-icon-swap"
            ) { (inputPresent, loading, canRegenerate) ->
                when {
                    loading -> {
                        LoadingIndicator(modifier = Modifier.size(56.dp))
                    }

                    !inputPresent -> {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = stringResource(id = R.string.paste_from_clipboard),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    canRegenerate -> {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(id = R.string.regenerate),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(id = R.string.summarize),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeFloatingActionButtonsEmptyPreview() {
    SummaryExpressiveTheme {
        HomeFloatingActionButtons(
            fabVisible = true,
            hasInput = false,
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

@Preview
@Composable
private fun HomeFloatingActionButtonsWithInputPreview() {
    SummaryExpressiveTheme {
        HomeFloatingActionButtons(
            fabVisible = true,
            hasInput = true,
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
