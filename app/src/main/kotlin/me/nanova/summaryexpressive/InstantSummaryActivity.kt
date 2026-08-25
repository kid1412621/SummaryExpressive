package me.nanova.summaryexpressive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppViewModel
import me.nanova.summaryexpressive.vm.SummaryViewModel

@AndroidEntryPoint
class InstantSummaryActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val summaryViewModel: SummaryViewModel by viewModels()
    private val textToSummarizeStateFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val settings by appViewModel.settingsUiState.collectAsState()
            val textToSummarize by textToSummarizeStateFlow.collectAsState()


            LaunchedEffect(textToSummarize, settings) {
                textToSummarize?.let {
                    if (it.isNotBlank()) {
                        summaryViewModel.summarize(it, settings)
                    }
                }
            }

            SummaryExpressiveTheme(
                darkTheme = when (settings.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settings.dynamicColor
            ) {
                InstantSummaryDialog(
                    viewModel = summaryViewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val textToSummarize = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                if (type.startsWith("application/") || type.startsWith("image/")) {
                    val contentUri: Uri? =
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    contentUri?.toString()
                } else {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }

            else -> {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            }
        }

        if (textToSummarize.isNullOrBlank()) {
            finish()
            return
        }
        textToSummarizeStateFlow.value = textToSummarize
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstantSummaryDialog(viewModel: SummaryViewModel, onDismiss: () -> Unit) {
    val summarizationState by viewModel.summarizationState.collectAsState()
    val isLoading = summarizationState.isLoading
    val summaryResult = summarizationState.summaryResult
    val error = summarizationState.error
    val clipboard = LocalClipboard.current
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val maxHeight = with(density) { (containerSize.height * 0.4f).toDp() }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp)
                    .heightIn(max = maxHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(modifier = Modifier.size(70.dp))
                            }
                        }

                        error != null -> {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = error.message ?: "An unknown error occurred.")
                        }

                        summaryResult != null -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = summaryResult.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        scope.launch {
                                            clipboard.setClipEntry(
                                                ClipData.newPlainText(
                                                    "User Input",
                                                    summaryResult.summary
                                                ).toClipEntry()
                                            )
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Summary"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = summaryResult.summary)
                            }
                        }
                    }
                }
            }
        }
    }
}