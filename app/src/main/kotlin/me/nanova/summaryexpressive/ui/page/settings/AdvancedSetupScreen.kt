package me.nanova.summaryexpressive.ui.page.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.defaultSystemPromptPlaceholder
import me.nanova.summaryexpressive.llm.generateFinalPromptString
import me.nanova.summaryexpressive.vm.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSummarySetupScreen(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val state by appViewModel.settingsUiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.advancedSummarySetup),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.basePrompt),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = if (state.isAppendMode) defaultSystemPromptPlaceholder else state.customBasePrompt,
                    onValueChange = { appViewModel.setCustomBasePrompt(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    shape = MaterialTheme.shapes.large,
                    readOnly = state.isAppendMode,
                    enabled = !state.isAppendMode
                )

                AnimatedVisibility(visible = !state.isAppendMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        FilledTonalButton(
                            onClick = { appViewModel.setCustomBasePrompt("") },
                            enabled = state.customBasePrompt.isNotEmpty()
                        ) {
                            Text("Clear")
                        }
                        FilledTonalButton(
                            onClick = {
                                appViewModel.setCustomBasePrompt(
                                    defaultSystemPromptPlaceholder
                                )
                            },
                            enabled = state.customBasePrompt != defaultSystemPromptPlaceholder
                        ) {
                            Text("Reset to Default")
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = stringResource(id = R.string.additionalConfigurations),
                    style = MaterialTheme.typography.titleMedium
                )

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            Icons.Rounded.PostAdd,
                            contentDescription = "Append Custom Prompt",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = { Text(stringResource(id = R.string.appendCustomPromptDescription)) },
                    trailingContent = {
                        Switch(
                            checked = state.isAppendMode,
                            onCheckedChange = { appViewModel.setIsAppendMode(it) }
                        )
                    }
                ) { Text(stringResource(id = R.string.appendCustomPrompt)) }

                AnimatedVisibility(visible = state.isAppendMode) {
                    OutlinedTextField(
                        value = state.additionalSystemPrompt,
                        onValueChange = { appViewModel.setAdditionalSystemPrompt(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 160.dp),
                        shape = MaterialTheme.shapes.large,
                        placeholder = {
                            Text(
                                text = "Enter additional instructions...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Translate,
                            contentDescription = "Language Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.useOriginalLanguage,
                            onCheckedChange = { appViewModel.setUseOriginalLanguageValue(it) }
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.useOriginalLanguageDescription)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { Text(stringResource(id = R.string.useOriginalLanguage)) }

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = { Text(stringResource(id = R.string.useLengthOptionsDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Rounded.ShortText,
                            contentDescription = "Length Options",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.showLength,
                            onCheckedChange = { appViewModel.setShowLengthValue(it) }
                        )
                    }
                ) { Text(stringResource(id = R.string.useLengthOptions)) }

                HorizontalDivider()

                Text(
                    text = stringResource(id = R.string.finalPromptPreview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val appLanguage =
                    LocalLocale.current.platformLocale.getDisplayLanguage(Locale.ENGLISH)
                val finalPromptPreview = generateFinalPromptString(
                    length = state.summaryLength,
                    showLength = state.showLength,
                    useContentLanguage = state.useOriginalLanguage,
                    appLanguage = appLanguage,
                    isAppendMode = state.isAppendMode,
                    customBasePrompt = state.customBasePrompt,
                    additionalSystemPrompt = state.additionalSystemPrompt
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = finalPromptPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
