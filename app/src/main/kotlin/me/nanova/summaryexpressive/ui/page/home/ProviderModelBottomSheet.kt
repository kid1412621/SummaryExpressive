package me.nanova.summaryexpressive.ui.page.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.ProviderConfig
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderModelBottomSheet(
    settings: SettingsUiState,
    onProviderSelect: (AIProvider) -> Unit,
    onModelSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        ProviderModelSheetContent(
            settings = settings,
            onProviderSelect = onProviderSelect,
            onModelSelect = onModelSelect,
            onDismiss = onDismiss,
            onGoToSettings = onGoToSettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProviderModelSheetContent(
    modifier: Modifier = Modifier,
    settings: SettingsUiState,
    onProviderSelect: (AIProvider) -> Unit,
    onModelSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.selectAIProvider),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = {
                    onDismiss()
                    onGoToSettings()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(id = R.string.setupAI))
            }
        }

        // Provider Horizontal Strip
        val sortedProviders = AIProvider.getEffectiveProviders(settings.providerOrder)
            .sortedByDescending { provider ->
                settings.providerConfigs[provider.name]?.let {
                    it.apiKey.isNotBlank() || it.baseUrl.isNotBlank()
                } ?: false
            }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sortedProviders, key = { it.name }) { provider ->
                val isConfigured = settings.providerConfigs[provider.name]?.let {
                    it.apiKey.isNotBlank() || it.baseUrl.isNotBlank()
                } ?: false
                val isSelected = settings.activeProvider == provider
                val unconfiguredPrompt = stringResource(
                    R.string.providerNotConfiguredPrompt,
                    provider.id.display
                )

                FilterChip(
                    selected = isSelected,
                    enabled = true,
                    onClick = {
                        if (isConfigured) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onProviderSelect(provider)
                        } else {
                            Toast.makeText(
                                context,
                                unconfiguredPrompt,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    label = {
                        Text(
                            text = provider.id.display,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = provider.icon),
                            contentDescription = provider.name,
                            modifier = Modifier.size(18.dp),
                            tint = if (!isConfigured) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else if (provider.isMonochromeIcon) {
                                LocalContentColor.current
                            } else {
                                Color.Unspecified
                            }
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Models Section Header
        Text(
            text = stringResource(id = R.string.setModel),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val currentProvider = settings.activeProvider
        val currentProviderConfig = currentProvider?.name?.let { settings.providerConfigs[it] }
        val models = currentProvider?.getEffectiveModels(currentProviderConfig) ?: emptyList()

        if (models.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(models, key = { it }) { modelId ->
                    val isSelected = settings.activeModel == modelId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                onModelSelect(modelId)
                                onDismiss()
                            }
                            .semantics { role = Role.RadioButton },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        } else {
                            null
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = modelId,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.noModelsConfigured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onGoToSettings()
                        }
                    ) {
                        Text(stringResource(id = R.string.configureInSettings))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun ProviderModelSheetContentPreview() {
    SummaryExpressiveTheme {
        ProviderModelSheetContent(
            settings = SettingsUiState(
                activeProvider = AIProvider.OPENAI,
                activeModel = "gpt-4o",
                providerConfigs = mapOf(
                    AIProvider.OPENAI.name to ProviderConfig(apiKey = "mock_key")
                )
            ),
            onProviderSelect = {},
            onModelSelect = {},
            onDismiss = {},
            onGoToSettings = {}
        )
    }
}

@Preview
@Composable
private fun ProviderModelSheetContentEmptyPreview() {
    SummaryExpressiveTheme {
        ProviderModelSheetContent(
            settings = SettingsUiState(
                activeProvider = AIProvider.OPENAI,
                activeModel = "",
                providerConfigs = emptyMap()
            ),
            onProviderSelect = {},
            onModelSelect = {},
            onDismiss = {},
            onGoToSettings = {}
        )
    }
}
