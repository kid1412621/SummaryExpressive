package me.nanova.summaryexpressive.ui.page.home.appbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.component.LlmSwitcher
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    settings: SettingsUiState,
    onNav: (dest: Nav) -> Unit,
    onIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier,
    onConfirmSwitch: ((AIProvider, String) -> Unit)? = null,
) {
    val effectiveProviders = remember(settings.providerOrder, settings.providerConfigs) {
        val providers = AIProvider.getEffectiveProviders(settings.providerOrder)
        val configured = providers.filter { provider ->
            settings.providerConfigs[provider.name]?.let {
                it.apiKey.isNotBlank() || it.baseUrl.isNotBlank()
            } ?: false
        }
        configured.ifEmpty { providers }
    }

    LargeTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.app_name),
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                IconButton(
                    onClick = { onNav(Nav.Settings()) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(id = R.string.settings),
                        modifier = Modifier.size(24.dp)
                    )
                }
                LlmSwitcher(
                    provider = settings.activeProvider,
                    model = settings.activeModel,
                    onClick = onIndicatorClick,
                    availableProviders = effectiveProviders,
                    getModelsForProvider = { provider ->
                        provider.getEffectiveModels(settings.providerConfigs[provider.name])
                    },
                    onConfirmSwitch = onConfirmSwitch
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onNav(Nav.History) },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(id = R.string.history),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeTopAppBarPreview() {
    SummaryExpressiveTheme {
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        HomeTopAppBar(
            scrollBehavior = scrollBehavior,
            settings = SettingsUiState(
                activeProvider = AIProvider.OPENAI,
                activeModel = "gpt-4o"
            ),
            onNav = {},
            onIndicatorClick = {},
            onConfirmSwitch = { _, _ -> }
        )
    }
}
