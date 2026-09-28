package me.nanova.summaryexpressive

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.rememberNavBackStack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.ui.AppNavigation
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppStartAction
import me.nanova.summaryexpressive.vm.AppViewModel
import me.nanova.summaryexpressive.vm.SettingsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Check if a link has been shared
        handleIntent(intent)

        setContent {
            val settingsState by settingsViewModel.settingsUiState.collectAsState()
            val isOnboarded by appViewModel.isOnboarded.collectAsState()

            SummaryExpressiveTheme(
                darkTheme = when (settingsState.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settingsState.dynamicColor
            ) {
                if (isOnboarded == null) {
                    return@SummaryExpressiveTheme
                }
                val startDestination = if (isOnboarded == true) Nav.Home else Nav.Onboarding
                val backStack = rememberNavBackStack(startDestination)
                AppNavigation(
                    backStack = backStack,
                    appViewModel = appViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                val autoTrigger = intent.getBooleanExtra("auto_trigger", false)
                val summaryJson = intent.getStringExtra("summary_result_json")
                val lengthResultsJson = intent.getStringExtra("length_results_json")

                val json = Json { ignoreUnknownKeys = true }
                val initialSummary = summaryJson?.let {
                    runCatching { json.decodeFromString<SummaryOutput>(it) }
                        .onFailure { e ->
                            android.util.Log.e(
                                "MainActivity",
                                "Failed to decode summaryJson: $summaryJson",
                                e
                            )
                        }
                        .getOrNull()
                }
                val lengthResults = lengthResultsJson?.let {
                    runCatching {
                        json.decodeFromString<Map<String, SummaryOutput>>(it)
                            .mapNotNull { (k, v) ->
                                runCatching { SummaryLength.valueOf(k) to v }.getOrNull()
                            }.toMap()
                    }.getOrDefault(emptyMap())
                } ?: emptyMap()

                val shouldAutoTrigger = if (initialSummary != null) false else autoTrigger

                if (type.startsWith("application/") || type.startsWith("image/")) {
                    val contentUri: Uri? =
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    contentUri?.let {
                        appViewModel.onEvent(
                            AppStartAction(
                                content = it.toString(),
                                autoTrigger = shouldAutoTrigger,
                                initialSummary = initialSummary,
                                lengthResults = lengthResults,
                            )
                        )
                    }
                } else {
                    val content = intent.getStringExtra(Intent.EXTRA_TEXT)
                    appViewModel.onEvent(
                        AppStartAction(
                            content = content,
                            autoTrigger = shouldAutoTrigger,
                            initialSummary = initialSummary,
                            lengthResults = lengthResults,
                        )
                    )
                }

                // Clear intent action so recreating activity won't re-trigger
                intent.action = null
            }

            Intent.ACTION_VIEW -> {
                if (intent.data?.host != "clipboard") return
                // To avoid re-triggering on configuration change, we clear the data.
                intent.data = null
                // Postpone clipboard access until the window has focus.
                window.decorView.post {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.getItemAt(0)?.text?.let {
                        appViewModel.onEvent(
                            AppStartAction(
                                content = it.toString(),
                                autoTrigger = true
                            )
                        )
                    }
                }
            }
        }
    }
}