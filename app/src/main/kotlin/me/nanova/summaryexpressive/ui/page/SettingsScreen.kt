package me.nanova.summaryexpressive.ui.page

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable as CoreAnimatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VpnKey
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.BuildConfig
import me.nanova.summaryexpressive.ProviderConfig
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.component.ClickablePasteIcon
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppViewModel
import me.nanova.summaryexpressive.vm.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Date

private enum class DialogState {
    NONE, THEME, AI_PROVIDER, MODEL
}

data class SettingsActions(
    val onThemeChange: (Int) -> Unit,
    val onApiKeyChange: (String) -> Unit,
    val onProviderChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onProviderConfigChange: (String, String, String) -> Unit,
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

    AnimatedContent(
        targetState = dialogState,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
            } else {
                slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
            }
        },
        label = "dialog animation"
    ) { targetDialog ->
        when (targetDialog) {
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
                    initialProvider = state.aiProvider ?: AIProvider.OPENAI,
                    providerConfigs = state.providerConfigs,
                    onDismissRequest = { dialogState = DialogState.NONE },
                    onConfirm = { provider, baseUrl, apiKey ->
                        actions.onProviderConfigChange(provider.name, baseUrl, apiKey)
                    },
                    onNext = { provider, baseUrl, apiKey ->
                        actions.onProviderConfigChange(provider.name, baseUrl, apiKey)
                        dialogState = DialogState.MODEL
                    }
                )
            }

            DialogState.MODEL -> {
                ModelSettingsDialog(
                    onDismissRequest = { dialogState = DialogState.NONE },
                    provider = state.aiProvider ?: AIProvider.OPENAI,
                    initialModelId = state.model,
                    onConfirm = { modelId ->
                        actions.onModelChange(modelId)
                    },
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
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

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
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
                            val expiryDate = SimpleDateFormat("yyyy-MM-dd", LocalLocale.current.platformLocale)
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
                            onCheckedChange = { newValue ->
                                actions.onUseOriginalLanguageChange(newValue)
                            }
                        )
                    },
                    overlineContent = null,
                    supportingContent = { Text(stringResource(id = R.string.useOriginalLanguageDescription)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    elevation = ListItemDefaults.elevation(),
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
                            onCheckedChange = { actions.onShowLengthChange(it) }
                        )
                    }
                ) { Text(stringResource(id = R.string.useLengthOptions)) }

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

private fun <T> inTween(): TweenSpec<T> = tween(durationMillis = 700)
private fun <T> outTween(): TweenSpec<T> = tween(durationMillis = 1000, delayMillis = 500)

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    val animatedColor = remember(surfaceVariantColor) { Animatable(surfaceVariantColor) }
    val animatedBorderWidth = remember { CoreAnimatable(0f) }

    LaunchedEffect(highlighted, surfaceVariantColor, secondaryContainerColor) {
        if (highlighted) {
            launch {
                animatedColor.animateTo(secondaryContainerColor, animationSpec = inTween())
                animatedColor.animateTo(surfaceVariantColor, animationSpec = outTween())
            }
            launch {
                animatedBorderWidth.animateTo(3f, animationSpec = inTween())
                animatedBorderWidth.animateTo(0f, animationSpec = outTween())
            }
        } else {
            animatedColor.snapTo(surfaceVariantColor)
            animatedBorderWidth.snapTo(0f)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun ThemeSettingsDialog(
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
private fun AIProviderSettingsDialog(
    onDismissRequest: () -> Unit,
    initialProvider: AIProvider,
    providerConfigs: Map<String, ProviderConfig>,
    onConfirm: (provider: AIProvider, baseUrl: String, apiKey: String) -> Unit,
    onNext: (provider: AIProvider, baseUrl: String, apiKey: String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val apiKeyFocusRequester = remember { FocusRequester() }
    var selectedProvider by remember { mutableStateOf(initialProvider) }
    
    var baseUrlTextFieldValue by remember(selectedProvider) { 
        mutableStateOf(providerConfigs[selectedProvider.name]?.baseUrl ?: "") 
    }
    var apiKeyTextFieldValue by remember(selectedProvider) { 
        mutableStateOf(providerConfigs[selectedProvider.name]?.apiKey ?: "") 
    }

    val formValid = (selectedProvider.isMandatoryBaseUrl && baseUrlTextFieldValue.isNotBlank())
            || (selectedProvider.isRequiredApiKey && apiKeyTextFieldValue.isNotBlank())
    val providerChanged = selectedProvider != initialProvider

    val submit = {
        if (formValid) {
            if (providerChanged) {
                onNext(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue)
            } else {
                onConfirm(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue)
                onDismissRequest()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.setAIProvider)) },
        text = {
            Column {
                AIProvider.entries.forEach {
                    AIProviderItem(it, selected = (selectedProvider == it)) {
                        selectedProvider = it
                    }
                }

                Spacer(modifier = Modifier.height(9.dp))

                OutlinedTextField(
                    value = baseUrlTextFieldValue,
                    onValueChange = { baseUrlTextFieldValue = it },
                    label = { Text(if (selectedProvider.isMandatoryBaseUrl) "* Base Url" else "Custom URL") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = "Base Url") },
                    trailingIcon = {
                        ClickablePasteIcon(
                            text = baseUrlTextFieldValue,
                            onPaste = { baseUrlTextFieldValue = it.trim() },
                            onClear = { baseUrlTextFieldValue = "" }
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { apiKeyFocusRequester.requestFocus() }
                    ),
                )

                Spacer(modifier = Modifier.height(9.dp))

                if (selectedProvider.isRequiredApiKey) {
                    OutlinedTextField(
                        modifier = Modifier.focusRequester(apiKeyFocusRequester),
                        value = apiKeyTextFieldValue,
                        onValueChange = { apiKeyTextFieldValue = it },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.VpnKey,
                                contentDescription = "API Key"
                            )
                        },
                        label = { Text("* " + stringResource(R.string.setApiKey)) },
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            ClickablePasteIcon(
                                text = apiKeyTextFieldValue,
                                onPaste = { apiKeyTextFieldValue = it.trim() },
                                onClear = { apiKeyTextFieldValue = "" }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                submit()
                            }
                        ),
                    )
                }
            }
        },
        confirmButton = {
                TextButton(
                    enabled = formValid,
                    onClick = { submit() }
                ) {
                    Text(stringResource(id = if (providerChanged) R.string.next else R.string.ok))
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
private fun ModelSettingsDialog(
    onDismissRequest: () -> Unit,
    provider: AIProvider,
    initialModelId: String?,
    onConfirm: (modelId: String) -> Unit,
) {
    val customModelKey = "##CUSTOM_MODEL##"
    val isInitialModelCustom =
        initialModelId?.let { id -> provider.models.none { it.id == id } } == true

    var selectedKey by remember {
        mutableStateOf(if (isInitialModelCustom) customModelKey else initialModelId)
    }
    var customModelName by remember {
        mutableStateOf(if (isInitialModelCustom) initialModelId else "")
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.setModel) + " (${provider.id.display})") },
        text = {
            Column(Modifier.heightIn(max = 390.dp)) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(provider.models.size) { index ->
                        val model = provider.models[index]
                        RadioButtonItem(
                            selected = selectedKey == model.id,
                            onSelectionChange = { selectedKey = model.id }
                        ) {
                            Text(text = model.id)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                RadioButtonItem(
                    selected = selectedKey == customModelKey,
                    onSelectionChange = { selectedKey = customModelKey }
                ) {
                    OutlinedTextField(
                        value = customModelName,
                        onValueChange = {
                            customModelName = it
                            selectedKey = customModelKey
                        },
                        label = { Text("Custom Model") },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        trailingIcon = {
                            ClickablePasteIcon(
                                text = customModelName,
                                onPaste = {
                                    customModelName = it.trim()
                                    selectedKey = customModelKey
                                },
                                onClear = { customModelName = "" }
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resultModelId = if (selectedKey == customModelKey) {
                        customModelName
                    } else {
                        selectedKey ?: ""
                    }
                    onConfirm(resultModelId)
                    onDismissRequest()
                },
                enabled = ((selectedKey != customModelKey && selectedKey != null) || (selectedKey == customModelKey && customModelName.isNotBlank()))
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
private fun RadioButtonItem(
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

@Composable
private fun AIProviderItem(
    llm: AIProvider,
    selected: Boolean,
    onSelectionChange: () -> Unit,
) {
    RadioButtonItem(selected, onSelectionChange) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(id = llm.icon),
                tint = if (selected && !llm.isMonochromeIcon) Color.Unspecified else LocalContentColor.current,
                contentDescription = "${llm.id.display} icon",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = llm.id.display, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


@Preview
@Composable
private fun AIProviderSettingsDialogPreview() {
    SummaryExpressiveTheme {
        AIProviderSettingsDialog(
            onDismissRequest = {},
            initialProvider = AIProvider.OPENAI,
            providerConfigs = emptyMap(),
            onConfirm = { _, _, _ -> },
            onNext = { _, _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ModelSettingsDialogPreview() {
    SummaryExpressiveTheme {
        ModelSettingsDialog(
            onDismissRequest = {},
            provider = AIProvider.OPENAI,
            onConfirm = { _ -> },
            initialModelId = AIProvider.OPENAI.models.first().id,
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

@Preview
@Composable
private fun AIProviderItemPreview() {
    SummaryExpressiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AIProvider.entries
                .forEach { AIProviderItem(it, selected = it == AIProvider.OPENAI) {} }
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
            aiProvider = AIProvider.OPENAI,
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
            onProviderConfigChange = { _, _, _ -> },
            onUseOriginalLanguageChange = {},
            onDynamicColorChange = {},
            onShowLengthChange = {},
            onAutoExtractUrlChange = {},
            onSessDataChange = { _, _ -> },
            onSessDataClear = {}
        )
        Scaffold { innerPadding ->
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
