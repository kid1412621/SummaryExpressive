package me.nanova.summaryexpressive.ui.page.settings

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpCenter
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.BuildConfig
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppViewModel
import me.nanova.summaryexpressive.vm.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.animation.core.Animatable as CoreAnimatable

private enum class DialogState {
    NONE, THEME, AI_PROVIDER, MODEL
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    var dialogState by remember { mutableStateOf(DialogState.NONE) }
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

    when (dialogState) {
        DialogState.NONE -> {}
        DialogState.THEME -> {
            ThemeSettingsDialog(
                onDismissRequest = { dialogState = DialogState.NONE },
                currentTheme = state.theme,
                onThemeChange = actions.onThemeChange,
            )
        }

        DialogState.AI_PROVIDER -> {
            AIProviderSettingsDialog(
                initialProvider = state.activeProvider ?: AIProvider.OPENAI,
                providerConfigs = state.providerConfigs,
                providerOrder = state.providerOrder,
                onDismissRequest = { dialogState = DialogState.NONE },
                onConfirm = { provider, baseUrl, apiKey, order ->
                    actions.onProviderConfigChange(provider.name, baseUrl, apiKey, order)
                },
                onNext = { provider, baseUrl, apiKey, order ->
                    actions.onProviderConfigChange(provider.name, baseUrl, apiKey, order)
                    dialogState = DialogState.MODEL
                }
            )
        }

        DialogState.MODEL -> {
            val currentProvider = state.activeProvider ?: AIProvider.OPENAI
            ModelSettingsDialog(
                onDismissRequest = { dialogState = DialogState.NONE },
                provider = currentProvider,
                initialModelId = state.activeModel,
                providerConfigs = state.providerConfigs,
                onConfirm = { models, selectedModel ->
                    actions.onSaveProviderModels(currentProvider.name, models, selectedModel)
                    dialogState = DialogState.NONE
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            innerPadding,
            state,
            actions,
            onNav = { onNav(it) },
            onShowThemeDialog = { dialogState = DialogState.THEME },
            onShowAIProviderDialog = { dialogState = DialogState.AI_PROVIDER },
            onShowModelDialog = { dialogState = DialogState.MODEL },
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

    LaunchedEffect(highlightSection) {
        if (highlightSection == "ai") {
            lazyListState.animateScrollToItem(index = 1)
        } else if (highlightSection == "3rd-party-service") {
            lazyListState.animateScrollToItem(index = 2)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsGroup {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                val uri = Uri.fromParts("package", context.packageName, null)
                                intent.data = uri
                                context.startActivity(intent)
                            }
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = "Localized description",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        supportingContent = { Text(stringResource(id = R.string.chooseLanguageDescription)) },
                    ) { Text(stringResource(id = R.string.chooseLanguage)) }

                    ListItem(
                        modifier = Modifier
                            .clickable(onClick = onShowThemeDialog)
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                Icons.Rounded.DarkMode,
                                contentDescription = "Dark mode",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        supportingContent = {
                            Text(
                                when (state.theme) {
                                    1 -> stringResource(id = R.string.darkTheme)
                                    2 -> stringResource(id = R.string.lightTheme)
                                    else -> stringResource(id = R.string.systemTheme)
                                }
                            )
                        }
                    ) { Text(stringResource(id = R.string.theme)) }

                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = { Text(stringResource(id = R.string.useDynamicColorDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Palette,
                                contentDescription = "Dynamic Color",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.dynamicColor,
                                onCheckedChange = {
                                    actions.onDynamicColorChange(it)
                                }
                            )
                        }
                    ) { Text(stringResource(id = R.string.useDynamicColor)) }
                }
            }

            item {
                SettingsGroup(highlighted = highlightSection == "ai") {
                    ListItem(
                        modifier = Modifier
                            .clickable(onClick = onShowAIProviderDialog)
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = { Text(stringResource(id = R.string.setAIProviderDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = "AI Provider",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) { Text(stringResource(id = R.string.setAIProvider)) }

                    ListItem(
                        modifier = Modifier
                            .clickable(onClick = onShowModelDialog)
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = { Text(stringResource(id = R.string.setModelDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "LLM Model",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) { Text(stringResource(id = R.string.setModel)) }

                    ListItem(
                        modifier = Modifier
                            .clickable { onNav(Nav.AdvancedSummarySetup) }
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = { Text(stringResource(id = R.string.advancedSummarySetupDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Rounded.ShortText,
                                contentDescription = "Advanced Summary Setup",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) { Text(stringResource(id = R.string.advancedSummarySetup)) }
                }
            }

            item {
                SettingsGroup(highlighted = highlightSection == "3rd-party-service") {
                    val sessDataValid =
                        (state.sessData.isNotBlank() && state.sessDataExpires > System.currentTimeMillis())
                    val itemColor =
                        if (sessDataValid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else LocalContentColor.current

                    ListItem(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (!sessDataValid) {
                                    onShowBiliBiliLoginSheet()
                                }
                            },
                            onLongClick = {
                                if (sessDataValid) {
                                    onShowClearSessDataDialog()
                                }
                            }
                        ),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = {
                            if (sessDataValid) {
                                val expiryDate = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    LocalLocale.current.platformLocale
                                )
                                    .format(Date(state.sessDataExpires))
                                Text(
                                    "Logged in, expires on $expiryDate. Long press to clear.",
                                    color = itemColor
                                )
                            } else {
                                Text("BiliBili required login to get transcripts which used for video summary")
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.bilibili),
                                contentDescription = "BiliBili",
                                modifier = Modifier.size(24.dp),
                                tint = itemColor
                            )
                        }
                    ) { Text("BiliBili Account", color = itemColor) }
                }
            }

            item {
                SettingsGroup {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        supportingContent = { Text(stringResource(R.string.useAutoExtractLinkDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Link,
                                contentDescription = stringResource(R.string.useAutoExtractLink),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.autoExtractUrl,
                                onCheckedChange = { actions.onAutoExtractUrlChange(it) }
                            )
                        }
                    ) { Text(stringResource(R.string.useAutoExtractLink)) }
                }
            }

            item {
                SettingsGroup {
                    ListItem(
                        modifier = Modifier
                            .clickable(onClick = { onNav(Nav.Onboarding) })
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Rounded.HelpCenter,
                                contentDescription = "Tutorial",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        supportingContent = { Text(stringResource(id = R.string.tutorialDescription)) },
                    ) { Text(stringResource(id = R.string.tutorial)) }

                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val url =
                                    "https://play.google.com/store/apps/details?id=${context.packageName}"
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                            }
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                Icons.Rounded.StarRate,
                                contentDescription = "Rate app",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        supportingContent = { Text(stringResource(id = R.string.googlePlayDescription)) },
                    ) { Text(stringResource(id = R.string.googlePlay)) }

                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val url = "https://discord.gg/WjN73wKTqd"
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                            }
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.discord),
                                contentDescription = "Discord",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        supportingContent = { Text(stringResource(id = R.string.discordDescription)) },
                    ) { Text(stringResource(id = R.string.discord)) }

                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val url = "https://github.com/kid1412621/SummaryExpressive"
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                            }
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.github),
                                contentDescription = "Codebase",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        supportingContent = { Text(stringResource(id = R.string.githubDescription)) },
                    ) { Text(stringResource(id = R.string.repository)) }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = CenterHorizontally
                ) {
                    val appInfo =
                        "${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.FLAVOR})"
                    Text(
                        text = "Version $appInfo",
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipData.newPlainText("App info", appInfo).toClipEntry()
                                    )
                                }
                            })
                    Text(
                        text = stringResource(id = R.string.madeBy),
                        modifier = Modifier
                            .clickable {
                                val url = "https://nanova.me"
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                            }
                    )
                }
            }
        }
    }
}

private fun <T> inTween(): TweenSpec<T> = tween(durationMillis = 700)
private fun <T> outTween(): TweenSpec<T> = tween(durationMillis = 1000, delayMillis = 500)

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    val animatedColor = remember(containerColor) { Animatable(containerColor) }
    val animatedBorderWidth = remember { CoreAnimatable(0f) }

    LaunchedEffect(highlighted, containerColor, secondaryContainerColor) {
        if (highlighted) {
            launch {
                animatedColor.animateTo(secondaryContainerColor, animationSpec = inTween())
                animatedColor.animateTo(containerColor, animationSpec = outTween())
            }
            launch {
                animatedBorderWidth.animateTo(3f, animationSpec = inTween())
                animatedBorderWidth.animateTo(0f, animationSpec = outTween())
            }
        } else {
            animatedColor.snapTo(containerColor)
            animatedBorderWidth.snapTo(0f)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = animatedColor.value,
        ),
        border = if (animatedBorderWidth.value > 0) BorderStroke(
            animatedBorderWidth.value.dp,
            MaterialTheme.colorScheme.secondary
        ) else null
    ) {
        content()
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
