package me.nanova.summaryexpressive.ui.page.history

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.ui.page.history.search.HistoryEmptySearchResults
import me.nanova.summaryexpressive.ui.page.history.search.HistoryFilterChips
import me.nanova.summaryexpressive.ui.page.history.search.HistorySearchBar
import me.nanova.summaryexpressive.ui.page.history.search.rememberHistorySearchState
import me.nanova.summaryexpressive.ui.page.history.section.GroupPosition
import me.nanova.summaryexpressive.ui.page.history.section.HistoryItemRow
import me.nanova.summaryexpressive.ui.page.history.section.HistorySectionHeader
import me.nanova.summaryexpressive.ui.page.history.section.getDateSectionTitle
import me.nanova.summaryexpressive.ui.page.history.sheet.HistoryDetailSheet
import me.nanova.summaryexpressive.vm.HistoryViewModel

/**
 * Material 3 Expressive History Screen with Android 16 search bar, connected shapes, and tonal badges
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel<HistoryViewModel>(),
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val searchState = rememberHistorySearchState(viewModel = viewModel)
    val historySummaries = viewModel.historySummaries.collectAsLazyPagingItems()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var selectedSummary by remember { mutableStateOf<HistorySummary?>(null) }

    val deletedMessage = stringResource(id = R.string.deleted)
    val undoMessage = stringResource(id = R.string.undo)

    LaunchedEffect(historySummaries.loadState.refresh, historySummaries.itemCount) {
        if (historySummaries.loadState.refresh is LoadState.NotLoading && historySummaries.itemCount > 0) {
            lazyListState.animateScrollToItem(0)
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
                    Text(text = stringResource(id = R.string.history))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        HistoryDetailSheet(
            summary = selectedSummary,
            onDismissRequest = { selectedSummary = null },
            onShowSnackbar = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )

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
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Search Bar
                item(key = "search_bar") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        HistorySearchBar(
                            query = searchState.query,
                            onQueryChange = searchState.onQueryChange,
                            onClear = searchState.onClear
                        )

                        HistoryFilterChips(
                            selectedFilter = searchState.selectedFilter,
                            onFilterChanged = searchState.onFilterChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }

                // Initial loading state
                if (historySummaries.loadState.refresh is LoadState.Loading) {
                    item(key = "loading_initial") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(56.dp))
                        }
                    }
                }

                // Empty state
                if (historySummaries.loadState.refresh is LoadState.NotLoading && historySummaries.itemCount == 0) {
                    item(key = "empty_state") {
                        HistoryEmptySearchResults(
                            query = searchState.query,
                            isSearching = searchState.isSearching
                        )
                    }
                }

                // Grouped History Items with connected shapes
                items(
                    count = historySummaries.itemCount,
                    key = historySummaries.itemKey { it.id }
                ) { index ->
                    val summary = historySummaries[index]
                    if (summary != null) {
                        val currentSection = getDateSectionTitle(summary.createdOn)
                        val prevSection = if (index > 0) {
                            historySummaries.peek(index - 1)?.let { getDateSectionTitle(it.createdOn) }
                        } else null
                        val nextSection = if (index < historySummaries.itemCount - 1) {
                            historySummaries.peek(index + 1)?.let { getDateSectionTitle(it.createdOn) }
                        } else null

                        val isFirstInSection = prevSection != currentSection
                        val isLastInSection = nextSection != currentSection

                        val position = when {
                            isFirstInSection && isLastInSection -> GroupPosition.SINGLE
                            isFirstInSection -> GroupPosition.TOP
                            isLastInSection -> GroupPosition.BOTTOM
                            else -> GroupPosition.MIDDLE
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    fadeOutSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                        ) {
                            if (isFirstInSection) {
                                HistorySectionHeader(
                                    title = currentSection,
                                    modifier = Modifier.padding(
                                        top = if (index > 0) 14.dp else 4.dp,
                                        bottom = 6.dp
                                    )
                                )
                            }

                            HistoryItemRow(
                                summary = summary,
                                position = position,
                                onClick = { selectedSummary = summary },
                                onDismiss = {
                                    scope.launch {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.removeHistorySummary(summary.id)

                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedMessage,
                                            actionLabel = undoMessage,
                                            duration = SnackbarDuration.Long
                                        )
                                        when (result) {
                                            SnackbarResult.ActionPerformed -> {
                                                viewModel.addHistorySummary(summary)
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                            }

                                            SnackbarResult.Dismissed -> {}
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Pagination append loading
                if (historySummaries.loadState.append is LoadState.Loading) {
                    item(key = "loading_append") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
