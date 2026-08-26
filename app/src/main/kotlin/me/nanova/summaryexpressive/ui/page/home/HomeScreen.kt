package me.nanova.summaryexpressive.ui.page.home

import android.content.ClipData
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppViewModel
import me.nanova.summaryexpressive.vm.SettingsUiState
import me.nanova.summaryexpressive.vm.SummarizationState
import me.nanova.summaryexpressive.vm.SummaryViewModel

object MimeTypes {
    const val PDF = "application/pdf"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
    const val JPG = "image/jpg"
    const val WEBP = "image/webp"

    val allSupported = arrayOf(PDF, DOCX, PNG, JPEG, JPG, WEBP)
}

data class HomeActions(
    val onSummarize: () -> Unit,
    val onClearInput: () -> Unit,
    val onUrlChange: (String) -> Unit,
    val onLengthSelect: (SummaryLength) -> Unit,
    val onPasteFromClipboard: () -> Unit,
    val onPlaySummary: () -> Unit,
    val onCopySummary: (String) -> Unit,
    val onShowSnackBar: (String) -> Unit,
    val onLaunchFilePicker: () -> Unit,
    val onLaunchImagePicker: () -> Unit,
    val onLaunchCamera: () -> Unit,
    val onSelectProvider: (AIProvider) -> Unit,
    val onSelectModel: (String) -> Unit,
    val onShowProviderModelSheet: () -> Unit,
    val onDismissProviderModelSheet: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNav: (dest: Nav) -> Unit = {},
    appViewModel: AppViewModel,
    summaryViewModel: SummaryViewModel = hiltViewModel<SummaryViewModel>(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    var urlOrText by rememberSaveable { mutableStateOf("") }
    var documentFilename by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showProviderModelSheet by rememberSaveable { mutableStateOf(false) }

    val settings by appViewModel.settingsUiState.collectAsState()
    val summarizationState by summaryViewModel.summarizationState.collectAsState()
    val appStartAction by appViewModel.appStartAction.collectAsState()

    fun summarize() {
        focusManager.clearFocus()
        summaryViewModel.summarize(urlOrText, settings)
    }

    fun clearInput() {
        urlOrText = ""
        summaryViewModel.clearCurrentSummary()
        focusRequester.requestFocus()
        documentFilename = null
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                summaryViewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                summaryViewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    scope.launch {
                        summaryViewModel.clearCurrentSummary()
                        documentFilename = getFileName(context, uri)
                        urlOrText = uri.toString()
                    }
                }
            }
        }

    LaunchedEffect(appStartAction) {
        appStartAction.content?.let { content ->
            summaryViewModel.clearCurrentSummary()
            documentFilename = null
            urlOrText = content
            if (appStartAction.autoTrigger) {
                summarize()
            }
            appViewModel.onStartActionHandled()
        }
    }

    LaunchedEffect(summarizationState.summaryResult) {
        isPlaying = false
    }

    LaunchedEffect(summarizationState.error) {
        isPlaying = false
        summarizationState.error?.let { error ->
            if (error is SummaryException.BiliBiliLoginRequiredException) {
                onNav(Nav.Settings(highlight = "3rd-party-service"))
                summaryViewModel.clearCurrentSummary()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackBarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val fabVisible by remember { derivedStateOf { !listState.canScrollBackward } }

    val hasResult = summarizationState.summaryResult?.summary?.isNotEmpty() == true
    val isDirty = settings.showLength && (summarizationState.summaryResult?.let { it.length != settings.summaryLength } ?: false)

    val actions = HomeActions(
        onSummarize = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            summarize()
        },
        onClearInput = { clearInput() },
        onUrlChange = { urlOrText = it },
        onLengthSelect = { appViewModel.setSummaryLength(it) },
        onPasteFromClipboard = {
            clearInput()
            scope.launch {
                clipboard.getClipEntry()?.let { clipEntry ->
                    urlOrText = clipEntry.clipData.getItemAt(0).text.toString()
                }
            }
        },
        onPlaySummary = { isPlaying = !isPlaying },
        onCopySummary = { text ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText("User Input", text).toClipEntry()
                )
            }
        },
        onShowSnackBar = { message ->
            scope.launch { snackBarHostState.showSnackbar(message) }
        },
        onLaunchFilePicker = { filePickerLauncher.launch(MimeTypes.allSupported) },
        onLaunchImagePicker = { imagePickerLauncher.launch("image/*") },
        onLaunchCamera = {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "new_image.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            uri?.let {
                cameraImageUri = it
                cameraLauncher.launch(it)
            }
        },
        onSelectProvider = { appViewModel.setAIProviderValue(it.name) },
        onSelectModel = { appViewModel.setModel(it) },
        onShowProviderModelSheet = { showProviderModelSheet = true },
        onDismissProviderModelSheet = { showProviderModelSheet = false },
    )

    if (showProviderModelSheet) {
        ProviderModelBottomSheet(
            settings = settings,
            onProviderSelect = actions.onSelectProvider,
            onModelSelect = actions.onSelectModel,
            onDismiss = actions.onDismissProviderModelSheet,
            onGoToSettings = { onNav(Nav.Settings(highlight = "ai")) }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            HomeTopAppBar(
                scrollBehavior = scrollBehavior,
                settings = settings,
                onNav = onNav,
                onIndicatorClick = actions.onShowProviderModelSheet
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            HomeFloatingActionButtons(
                fabVisible = fabVisible,
                onPaste = actions.onPasteFromClipboard,
                onSummarize = actions.onSummarize,
                isLoading = summarizationState.isLoading,
                hasResult = hasResult,
                isDirty = isDirty,
                onShowSnackBar = actions.onShowSnackBar,
                onLaunchFilePicker = actions.onLaunchFilePicker,
                onLaunchImagePicker = actions.onLaunchImagePicker,
                onLaunchCamera = actions.onLaunchCamera
            )
        }
    ) { innerPadding ->
        HomeContent(
            innerPadding = innerPadding,
            listState = listState,
            urlOrText = urlOrText,
            documentFilename = documentFilename,
            focusRequester = focusRequester,
            settings = settings,
            summarizationState = summarizationState,
            isPlaying = isPlaying,
            actions = actions
        )
    }
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    listState: LazyListState,
    urlOrText: String,
    documentFilename: String?,
    focusRequester: FocusRequester,
    settings: SettingsUiState,
    summarizationState: SummarizationState,
    isPlaying: Boolean,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val lengthOptions = listOf(
        stringResource(id = R.string.short_length),
        stringResource(id = R.string.middle_length),
        stringResource(id = R.string.long_length)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    InputSection(
                        urlOrText = urlOrText,
                        onUrlChange = actions.onUrlChange,
                        onSummarize = actions.onSummarize,
                        documentFilename = documentFilename,
                        error = summarizationState.error,
                        apiKey = settings.apiKey,
                        onClear = actions.onClearInput,
                        focusRequester = focusRequester,
                        isLoading = summarizationState.isLoading,
                    )

                    if (settings.showLength) {
                        LengthSelector(
                            selectedIndex = settings.summaryLength.ordinal,
                            onSelectedIndexChange = { actions.onLengthSelect(SummaryLength.entries[it]) },
                            options = lengthOptions,
                            enabled = !summarizationState.isLoading,
                        )
                    }

                    if (summarizationState.isLoading) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    summarizationState.summaryResult?.takeIf { it.summary.isNotEmpty() }?.let { summaryOutput ->
                        SummaryCard(
                            modifier = Modifier.padding(vertical = 15.dp),
                            isExpandedByDefault = true,
                            summary = summaryOutput,
                            onLongClick = { actions.onCopySummary(summaryOutput.summary) },
                            onShowSnackbar = actions.onShowSnackBar,
                            isPlaying = isPlaying,
                            onPlayRequest = actions.onPlaySummary
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeContentPreview() {
    SummaryExpressiveTheme {
        HomeContent(
            innerPadding = PaddingValues(0.dp),
            listState = rememberLazyListState(),
            urlOrText = "https://example.com/article",
            documentFilename = null,
            focusRequester = remember { FocusRequester() },
            settings = SettingsUiState(
                activeProvider = AIProvider.OPENAI,
                activeModel = "gpt-4o",
                showLength = true,
                summaryLength = SummaryLength.MEDIUM
            ),
            summarizationState = SummarizationState(
                summaryResult = SummaryOutput(
                    title = "Sample Summary",
                    summary = "This is a sample summary content for testing preview.",
                    author = "Author Name",
                    sourceLink = "https://example.com",
                    isYoutubeLink = false,
                    isBiliBiliLink = false,
                    length = SummaryLength.MEDIUM,
                    provider = AIProvider.OPENAI.name,
                    model = "gpt-4o"
                )
            ),
            isPlaying = false,
            actions = HomeActions(
                onSummarize = {},
                onClearInput = {},
                onUrlChange = {},
                onLengthSelect = {},
                onPasteFromClipboard = {},
                onPlaySummary = {},
                onCopySummary = {},
                onShowSnackBar = {},
                onLaunchFilePicker = {},
                onLaunchImagePicker = {},
                onLaunchCamera = {},
                onSelectProvider = {},
                onSelectModel = {},
                onShowProviderModelSheet = {},
                onDismissProviderModelSheet = {}
            )
        )
    }
}
