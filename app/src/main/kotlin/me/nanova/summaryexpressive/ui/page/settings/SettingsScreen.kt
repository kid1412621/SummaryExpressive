package me.nanova.summaryexpressive.ui.page.settings

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.BuildConfig
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.page.settings.dialog.AIProviderSettingsDialog
import me.nanova.summaryexpressive.ui.page.settings.dialog.ModelSettingsDialog
import me.nanova.summaryexpressive.ui.page.settings.dialog.ThemeSettingsDialog
import me.nanova.summaryexpressive.ui.page.settings.search.SettingsEmptySearchResults
import me.nanova.summaryexpressive.ui.page.settings.search.SettingsSearchBar
import me.nanova.summaryexpressive.ui.page.settings.search.rememberSettingsSearchState
import me.nanova.summaryexpressive.ui.page.settings.section.AISettingsGroup
import me.nanova.summaryexpressive.ui.page.settings.section.DisplaySettingsGroup
import me.nanova.summaryexpressive.ui.page.settings.section.GroupPosition
import me.nanova.summaryexpressive.ui.page.settings.section.HelpSettingsGroup
import me.nanova.summaryexpressive.ui.page.settings.section.ServicesSettingsGroup
import me.nanova.summaryexpressive.ui.page.settings.section.SettingItem
import me.nanova.summaryexpressive.ui.page.settings.section.SettingsFooter
import me.nanova.summaryexpressive.ui.page.settings.section.SettingsGroup
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppViewModel
import me.nanova.summaryexpressive.vm.SettingsUiState

private sealed interface DialogState {
    data object None : DialogState
    data object Theme : DialogState
    data object Provider : DialogState
    data class Model(val provider: AIProvider? = null) : DialogState
}

data class SettingsActions(
    val onThemeChange: (Int) -> Unit,
    val onApiKeyChange: (String) -> Unit,
    val onProviderChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onProviderConfigChange: (String, String, String, List<String>?) -> Unit,
    val onSaveProviderModels: (String, List<String>, String) -> Unit,
    val onResetProviderModels: (String) -> Unit,
    val onUseOriginalLanguageChange: (Boolean) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onShowLengthChange: (Boolean) -> Unit,
    val onAutoExtractUrlChange: (Boolean) -> Unit,
    val onSessDataChange: (String, Long) -> Unit,
    val onSessDataClear: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNav: (Nav) -> Unit = {},
    appViewModel: AppViewModel,
    highlightSection: String?,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val state by appViewModel.settingsUiState.collectAsState()
    val actions = SettingsActions(
        onThemeChange = appViewModel::setTheme,
        onApiKeyChange = appViewModel::setApiKeyValue,
        onProviderChange = appViewModel::setAIProviderValue,
        onModelChange = appViewModel::setModel,
        onProviderConfigChange = appViewModel::setProviderConfig,
        onSaveProviderModels = appViewModel::setProviderModels,
        onResetProviderModels = appViewModel::resetProviderModelsToDefault,
        onUseOriginalLanguageChange = appViewModel::setUseOriginalLanguageValue,
        onDynamicColorChange = appViewModel::setDynamicColorValue,
        onShowLengthChange = appViewModel::setShowLengthValue,
        onAutoExtractUrlChange = appViewModel::setAutoExtractUrlValue,
        onSessDataChange = appViewModel::setSessData,
        onSessDataClear = appViewModel::clearSessData
    )

    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    var showBiliBiliLoginSheet by remember { mutableStateOf(value = false) }
    var showClearSessDataDialog by remember { mutableStateOf(value = false) }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()

    if (showClearSessDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessDataDialog = false },
            title = { Text("Clear BiliBili Login") },
            text = { Text("Are you sure you want to clear your BiliBili login information?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onSessDataClear()
                        showClearSessDataDialog = false
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessDataDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBiliBiliLoginSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBiliBiliLoginSheet = false },
            sheetState = sheetState
        ) {
            fun hide() {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showBiliBiliLoginSheet = false
                    }
                }
            }
            BiliBiliLoginSheetContent(
                onDismiss = { hide() }
            ) { sessData, expires ->
                actions.onSessDataChange(sessData, expires)
                hide()
            }
        }
    }

    when (val currentDialogState = dialogState) {
        DialogState.None -> {}
        DialogState.Theme -> {
            ThemeSettingsDialog(
                onDismissRequest = { dialogState = DialogState.None },
                currentTheme = state.theme,
                onThemeChange = actions.onThemeChange,
            )
        }

        DialogState.Provider -> {
            AIProviderSettingsDialog(
                initialProvider = state.activeProvider ?: AIProvider.OPENAI,
                providerConfigs = state.providerConfigs,
                providerOrder = state.providerOrder,
                onDismissRequest = { dialogState = DialogState.None },
                onConfirm = { provider, baseUrl, apiKey, order ->
                    actions.onProviderConfigChange(provider.name, baseUrl, apiKey, order)
                },
                onNext = { provider, baseUrl, apiKey, order ->
                    actions.onProviderConfigChange(provider.name, baseUrl, apiKey, order)
                    dialogState = DialogState.Model(provider)
                }
            )
        }

        is DialogState.Model -> {
            val targetProvider =
                currentDialogState.provider ?: state.activeProvider ?: AIProvider.OPENAI
            val targetModelId = state.providerConfigs[targetProvider.name]?.activeModel
                ?: if (targetProvider == state.activeProvider) state.activeModel else null
            ModelSettingsDialog(
                onDismissRequest = { dialogState = DialogState.None },
                provider = targetProvider,
                initialModelId = targetModelId,
                providerConfigs = state.providerConfigs,
                onConfirm = { models, selectedModel ->
                    actions.onSaveProviderModels(targetProvider.name, models, selectedModel)
                    dialogState = DialogState.None
                },
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        SettingsContent(
            innerPadding = innerPadding,
            state = state,
            actions = actions,
            onNav = { onNav(it) },
            onShowThemeDialog = { dialogState = DialogState.Theme },
            onShowAIProviderDialog = { dialogState = DialogState.Provider },
            onShowModelDialog = { dialogState = DialogState.Model() },
            onShowBiliBiliLoginSheet = { showBiliBiliLoginSheet = true },
            onShowClearSessDataDialog = { showClearSessDataDialog = true },
            highlightSection = highlightSection
        )
    }
}

@Composable
private fun SettingsContent(
    innerPadding: PaddingValues,
    state: SettingsUiState,
    actions: SettingsActions,
    onNav: (Nav) -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowAIProviderDialog: () -> Unit,
    onShowModelDialog: () -> Unit,
    onShowBiliBiliLoginSheet: () -> Unit,
    onShowClearSessDataDialog: () -> Unit,
    highlightSection: String?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchState = rememberSettingsSearchState(
        state = state,
        actions = actions,
        onNav = onNav,
        onShowThemeDialog = onShowThemeDialog,
        onShowAIProviderDialog = onShowAIProviderDialog,
        onShowModelDialog = onShowModelDialog,
        onShowBiliBiliLoginSheet = onShowBiliBiliLoginSheet,
        onShowClearSessDataDialog = onShowClearSessDataDialog
    )

    LaunchedEffect(highlightSection) {
        if (highlightSection == "ai") {
            lazyListState.animateScrollToItem(index = 2)
        } else if (highlightSection == "3rd-party-service") {
            lazyListState.animateScrollToItem(index = 3)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Android 16 Search Bar
            item(key = "search_bar") {
                SettingsSearchBar(
                    query = searchState.query,
                    onQueryChange = searchState::onQueryChange,
                    onClear = searchState::onClear
                )
            }

            if (searchState.isSearching) {
                if (searchState.filteredItems.isEmpty()) {
                    item(key = "empty_search") {
                        SettingsEmptySearchResults(query = searchState.query)
                    }
                } else {
                    item(key = "search_results") {
                        SettingsGroup {
                            searchState.filteredItems.forEachIndexed { index, item ->
                                val position = when {
                                    searchState.filteredItems.size == 1 -> GroupPosition.SINGLE
                                    index == 0 -> GroupPosition.TOP
                                    index == searchState.filteredItems.lastIndex -> GroupPosition.BOTTOM
                                    else -> GroupPosition.MIDDLE
                                }
                                SettingItem(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    iconBadge = item.iconBadge,
                                    position = position,
                                    trailingContent = item.trailingContent,
                                    onClick = item.onClick,
                                    onLongClick = item.onLongClick,
                                    enabled = item.enabled
                                )
                            }
                        }
                    }
                }
            } else {
                // Group 1: Display & Interface
                item(key = "group_display") {
                    DisplaySettingsGroup(
                        state = state,
                        actions = actions,
                        onShowThemeDialog = onShowThemeDialog
                    )
                }

                // Group 2: AI & Models
                item(key = "group_ai") {
                    AISettingsGroup(
                        state = state,
                        highlighted = highlightSection == "ai",
                        onShowAIProviderDialog = onShowAIProviderDialog,
                        onShowModelDialog = onShowModelDialog,
                        onNav = onNav
                    )
                }

                // Group 3: Services & Tools
                item(key = "group_services") {
                    ServicesSettingsGroup(
                        state = state,
                        actions = actions,
                        highlighted = highlightSection == "3rd-party-service",
                        onShowBiliBiliLoginSheet = onShowBiliBiliLoginSheet,
                        onShowClearSessDataDialog = onShowClearSessDataDialog
                    )
                }

                // Group 4: Help & Community
                item(key = "group_help") {
                    HelpSettingsGroup(
                        onNav = onNav
                    )
                }

                // Footer (App Version & Info)
                item(key = "footer") {
                    val appInfo = "${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.FLAVOR})"
                    val copiedText = stringResource(id = R.string.copied)

                    SettingsFooter(
                        appInfo = appInfo,
                        onCopyAppInfo = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("App info", appInfo).toClipEntry()
                                )
                                Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenWebsite = {
                            val url = "https://nanova.me"
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScrollContentPreview() {
    SummaryExpressiveTheme {
        val state = SettingsUiState(
            theme = 0,
            apiKey = "test_key",
            baseUrl = "",
            activeProvider = AIProvider.OPENAI,
            useOriginalLanguage = false,
            dynamicColor = true,
            showLength = true,
            autoExtractUrl = true
        )
        val actions = SettingsActions(
            onThemeChange = {},
            onApiKeyChange = {},
            onProviderChange = {},
            onModelChange = {},
            onProviderConfigChange = { _, _, _, _ -> },
            onSaveProviderModels = { _, _, _ -> },
            onResetProviderModels = {},
            onUseOriginalLanguageChange = {},
            onDynamicColorChange = {},
            onShowLengthChange = {},
            onAutoExtractUrlChange = {},
            onSessDataChange = { _, _ -> },
            onSessDataClear = {}
        )
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            SettingsContent(
                innerPadding = innerPadding,
                state = state,
                actions = actions,
                onShowThemeDialog = {},
                onShowAIProviderDialog = {},
                onShowModelDialog = {},
                highlightSection = null,
                onNav = {},
                onShowBiliBiliLoginSheet = {},
                onShowClearSessDataDialog = {}
            )
        }
    }
}
