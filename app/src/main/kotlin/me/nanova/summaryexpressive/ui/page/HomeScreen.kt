package me.nanova.summaryexpressive.ui.page

import android.content.ClipData
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool.Companion.isYouTubeLink
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.model.SummaryResult
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.vm.SummarySource
import me.nanova.summaryexpressive.vm.SummaryViewModel


private object MimeTypes {
    const val PDF = "application/pdf"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
    const val JPG = "image/jpg"
    const val WEBP = "image/webp"

    val allSupported = arrayOf(PDF, DOCX, PNG, JPEG, JPG, WEBP)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    initialUrl: String? = null,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    var urlOrText by remember { mutableStateOf(initialUrl ?: "") }
    val scope = rememberCoroutineScope() // Coroutine scope for async calls
    val context = LocalContext.current
    val clipboard = LocalClipboard.current // Clipboard
    val focusManager = LocalFocusManager.current // Hide cursor
    val focusRequester = remember { FocusRequester() } // Show cursor after removing
    val options = listOf(
        stringResource(id = R.string.short_length),
        stringResource(id = R.string.middle_length),
        stringResource(id = R.string.long_length)
    ) // Lengths
    val isLoading by viewModel.isLoading.collectAsState()
    val summaryResult by viewModel.currentSummaryResult.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val apiKey by viewModel.apiKey.collectAsState()
    var documentFilename by remember { mutableStateOf<String?>(null) }
    val inputSource by remember {
        derivedStateOf {
            when {
                documentFilename != null -> SummarySource.Document(documentFilename, urlOrText)
                urlOrText.startsWith("http", ignoreCase = true)
                        || urlOrText.startsWith("https", ignoreCase = true) ->
                    if (isYouTubeLink(urlOrText)) SummarySource.Video(urlOrText)
                    else SummarySource.Article(urlOrText)

                urlOrText.isNotBlank() -> SummarySource.Text(urlOrText)
                else -> SummarySource.None
            }
        }
    }
    var singleLine by remember { mutableStateOf(false) }
    val showLength by viewModel.showLength.collectAsState()
    val summaryLength by viewModel.summaryLength.collectAsState()
    val multiLine by viewModel.multiLine.collectAsState()

    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            result.value = uri
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                viewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    fun summarize() {
        focusManager.clearFocus()
        viewModel.summarize(inputSource)
    }

    fun clearInput() {
        urlOrText = ""
        viewModel.clearCurrentSummary()
        focusRequester.requestFocus()
        documentFilename = null
        singleLine = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { HomeTopAppBar(navController, scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            HomeFloatingActionButtons(
                onPaste = {
                    clearInput()
                    scope.launch {
                        clipboard.getClipEntry()?.let {
                            urlOrText = it.clipData.getItemAt(0).text.toString()
                        }
                    }
                },
                onSummarize = { summarize() },
                isLoading = isLoading,
                onShowSnackbar = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
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
                        onUrlChange = { newText ->
                            urlOrText = newText
                        },
                        onSummarize = { summarize() },
                        error = error,
                        apiKey = apiKey,
                        onClear = { clearInput() },
                        focusRequester = focusRequester,
                        onLaunchFilePicker = {
                            launcher.launch(MimeTypes.allSupported)
                        },
                        inputSource = inputSource,
                        isLoading = isLoading,
                        multiLine = multiLine,
                        singleLine = singleLine,
                        onSingleLineChange = { singleLine = it }
                    )

                    val currentResult = summaryResult
                    if (showLength) {
                        SummaryLengthSelector(
                            selectedIndex = summaryLength.ordinal,
                            onSelectedIndexChange = { viewModel.setSummaryLength(SummaryLength.entries[it]) },
                            options = options,
                            enabled = !isLoading,
                        )
                    }

                    // Loading-Animation
                    if (isLoading) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }

                    if (currentResult != null && !currentResult.summary.isNullOrEmpty()) {
                        SummaryResultSection(
                            summaryResult = currentResult,
                            onRegenerate = { summarize() },
                            onShowSnackbar = { message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTopAppBar(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    MediumFlexibleTopAppBar(
        modifier = Modifier.height(100.dp),
        colors = TopAppBarDefaults.topAppBarColors(),
        title = { },
        navigationIcon = {
            IconButton(
                onClick = { navController.navigate("settings") }
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        actions = {
            IconButton(
                onClick = { navController.navigate("history") }
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = "History",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InputSection(
    urlOrText: String,
    onUrlChange: (String) -> Unit,
    onSummarize: () -> Unit,
    inputSource: SummarySource,
    error: Throwable?,
    apiKey: String,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    onLaunchFilePicker: () -> Unit,
    isLoading: Boolean,
    multiLine: Boolean,
    singleLine: Boolean,
    onSingleLineChange: (Boolean) -> Unit,
) {
    val showCancelIcon = remember(urlOrText) { urlOrText.isNotBlank() }
    val textToShow =
        if (inputSource is SummarySource.Document) inputSource.filename ?: urlOrText else urlOrText

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = textToShow,
                onValueChange = onUrlChange,
                label = { Text("URL/Text") },
                enabled = !isLoading,
                readOnly = inputSource is SummarySource.Document,
                isError = error != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSummarize() }),
                supportingText = {
                    if (error != null) {
                        ErrorMessage(error.message, apiKey)
                    } else {
                        Column {
                            if (inputSource is SummarySource.Text && urlOrText.isNotBlank()) {
                                // Based on the rule of thumb that 100 tokens is about 75 words.
                                // ref: https://platform.openai.com/tokenizer
                                val wordCount = urlOrText.trim().split(Regex("\\s+")).size
                                val tokenCount = (wordCount * 4) / 3
                                Text(
                                    text = "Approximate tokens: $tokenCount",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.tertiaryFixedDim
                                )
                            }
                        }
                    }
                },
                trailingIcon = {
                    if (showCancelIcon) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Outlined.Cancel,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                maxLines = 15,
                singleLine = if (multiLine) singleLine else true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 20.dp)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                OutlinedButton(
                    onClick = onLaunchFilePicker,
                    enabled = !isLoading,
                    modifier = Modifier
                        .padding(top = 27.dp)
                        .height(58.dp)
                ) {
                    Icon(
                        if (inputSource is SummarySource.Document) Icons.Filled.CheckCircle
                        else Icons.Filled.AddCircle,
                        contentDescription = "Floating action button",
                    )
                }
                if (multiLine) {
                    val textLength = urlOrText.length
                    val lineBreaks = urlOrText.count { it == '\n' }
                    if (textLength >= 100 || lineBreaks >= 1) {
                        Button(
                            onClick = { onSingleLineChange(!singleLine) },
                            enabled = !isLoading,
                            modifier = Modifier
                                .height(72.dp)
                                .padding(top = 15.dp)
                        ) {
                            Icon(
                                imageVector = if (singleLine) Icons.Outlined.KeyboardArrowDown
                                else Icons.Outlined.KeyboardArrowUp,
                                contentDescription = if (singleLine) "Minimize" else "Expand",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryLengthSelector(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    options: List<String>,
    enabled: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onSelectedIndexChange(index) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun SummaryResultSection(
    summaryResult: SummaryResult,
    onRegenerate: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val isYoutube = summaryResult.isYoutubeLink

    SummaryCard(
        modifier = Modifier.padding(top = 15.dp, bottom = 15.dp),
        title = summaryResult.title,
        author = summaryResult.author,
        summary = summaryResult.summary,
        isYouTube = isYoutube,
        onLongClick = {
            scope.launch {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboard.setClipEntry(
                    ClipData.newPlainText(
                        "User Input",
                        summaryResult.summary
                    ).toClipEntry()
                )
            }
        },
        onShowSnackbar = onShowSnackbar
    )

    Column(
        modifier = Modifier
            .padding(top = 15.dp, bottom = 90.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(
                onClick = onRegenerate,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(id = R.string.regenerate))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeFloatingActionButtons(
    onPaste: () -> Unit,
    onSummarize: () -> Unit,
    isLoading: Boolean,
    onShowSnackbar: (String) -> Unit,
) {
    val stillLoading = stringResource(id = R.string.stillLoading)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButton(
            onClick = { if (!isLoading) onPaste() }
        ) {
            Icon(
                Icons.Rounded.ContentPaste,
                contentDescription = "Paste",
                modifier = Modifier.size(24.dp)
            )
        }
        FloatingActionButton(
            onClick = {
                if (isLoading) {
                    onShowSnackbar(stillLoading)
                } else {
                    onSummarize()
                }
            }
        ) {
            if (isLoading) LoadingIndicator()
            else Icon(Icons.Filled.Check, "Check")
        }
    }
}

@Composable
private fun ErrorMessage(errorMessage: String?, apiKey: String) {
    val errMsg = when (errorMessage) {
        SummaryException.NoInternetException.message -> stringResource(id = R.string.noInternet)
        SummaryException.InvalidLinkException.message -> stringResource(id = R.string.invalidURL)
        SummaryException.NoTranscriptException.message -> stringResource(id = R.string.noTranscript)
        SummaryException.NoContentException.message -> stringResource(id = R.string.noContent)
        SummaryException.TooShortException.message -> stringResource(id = R.string.tooShort)
        SummaryException.PaywallException.message -> stringResource(id = R.string.paywallDetected)
        SummaryException.TooLongException.message -> stringResource(id = R.string.tooLong)
        SummaryException.IncorrectKeyException.message -> {
            if (apiKey.isBlank()) {
                stringResource(id = R.string.incorrectKeyOpenSource)
            } else {
                stringResource(id = R.string.incorrectKey)
            }
        }

        SummaryException.RateLimitException.message -> stringResource(id = R.string.rateLimit)
        SummaryException.NoKeyException.message -> stringResource(id = R.string.noKey)
        else -> errorMessage ?: "unknown error"
    }
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = errMsg,
        color = MaterialTheme.colorScheme.error
    )
}


@Preview
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UrlInputSectionPreview() {
    val url by remember { mutableStateOf("https://example.com") }
    val focusRequester = remember { FocusRequester() }

    Column {
        InputSection(
            urlOrText = url,
            onUrlChange = {},
            onSummarize = {},
            error = null,
            apiKey = "test_api_key",
            onClear = {},
            focusRequester = focusRequester,
            onLaunchFilePicker = {},
            inputSource = SummarySource.Article(url),
            isLoading = false,
            multiLine = true,
            singleLine = false,
            onSingleLineChange = {}
        )

        HorizontalDivider()

        InputSection(
            urlOrText = "",
            onUrlChange = {},
            onSummarize = {},
            error = SummaryException.InvalidLinkException,
            apiKey = "test_api_key",
            onClear = {},
            focusRequester = focusRequester,
            onLaunchFilePicker = {},
            inputSource = SummarySource.Document(
                filename = "some image or documentations",
                uri = ""
            ),
            isLoading = false,
            multiLine = true,
            singleLine = false,
            onSingleLineChange = {}
        )
    }
}


@Preview
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryLengthSelectorPreview() {
    val selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Short", "Medium", "Long")

    SummaryLengthSelector(
        selectedIndex = selectedIndex,
        onSelectedIndexChange = {},
        options = options,
        enabled = true
    )
}
