package me.nanova.summaryexpressive.ui.page.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.component.LlmSwitcher
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    settings: SettingsUiState,
    onNav: (dest: Nav) -> Unit,
    onIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediumFlexibleTopAppBar(
        modifier = modifier.height(100.dp),
        colors = TopAppBarDefaults.topAppBarColors(),
        title = { },
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onNav(Nav.Settings()) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
                }
                LlmSwitcher(
                    provider = settings.activeProvider,
                    model = settings.activeModel,
                    onClick = onIndicatorClick
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onNav(Nav.History) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(id = R.string.history),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeTopAppBarPreview() {
    SummaryExpressiveTheme {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        HomeTopAppBar(
            scrollBehavior = scrollBehavior,
            settings = SettingsUiState(
                activeProvider = AIProvider.OPENAI,
                activeModel = "gpt-4o"
            ),
            onNav = {},
            onIndicatorClick = {}
        )
    }
}
