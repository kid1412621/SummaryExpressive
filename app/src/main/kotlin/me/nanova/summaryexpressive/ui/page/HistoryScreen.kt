package me.nanova.summaryexpressive.ui.page

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.vm.HistoryViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel<HistoryViewModel>(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val searchBarState = rememberSearchBarState()
    val selectedFilter by viewModel.filterType.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                Modifier.background(
                    color = MaterialTheme.colorScheme.surface,
                )
            ) {
                SearchBar(
                    state = searchBarState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 15.dp),
                    inputField = {
                        SearchBarDefaults.InputField(
                            textFieldState = viewModel.searchState,
                            searchBarState = searchBarState,
                            onSearch = { focusManager.clearFocus() },
                            placeholder = { Text(stringResource(id = R.string.search)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = stringResource(id = R.string.search)
                                )
                            },
                            trailingIcon = {
                                if (viewModel.searchState.text.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onSearchTextChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear"
                                        )
                                    }
                                }
                            },
                        )
                    },
                )

                TypeFilters(
                    selectedFilter = selectedFilter,
                    onFilterChanged = { viewModel.onFilterChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val historySummaries = viewModel.historySummaries.collectAsLazyPagingItems()
        val haptics = LocalHapticFeedback.current

        var playingSummaryId by remember { mutableStateOf<String?>(null) }
        val lazyListState = rememberLazyListState()

        LaunchedEffect(historySummaries.loadState.refresh, historySummaries.itemCount) {
            if (historySummaries.loadState.refresh is LoadState.NotLoading && historySummaries.itemCount > 0) {
                lazyListState.animateScrollToItem(0)
            }
        }

        val deletedMessage = stringResource(id = R.string.deleted)
        val undoMessage = stringResource(id = R.string.undo)
        val scope = rememberCoroutineScope()
        val onShowSnackbar: (String) -> Unit = { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }

        when (historySummaries.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(70.dp))
                }
                return@Scaffold
            }

            is LoadState.Error -> {
                // TODO: error handling
            }

            is LoadState.NotLoading -> {
                if (historySummaries.itemCount == 0) {
                    val message = stringResource(
                        id = if (viewModel.searchState.text.isBlank() && selectedFilter == null) R.string.noHistory
                        else R.string.nothingFound
                    )
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                    }
                    return@Scaffold
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                count = historySummaries.itemCount,
                key = historySummaries.itemKey { it.id }
            ) { index ->
                val summary = historySummaries[index]
                if (summary != null) {
                    SwipeableSummaryCard(
                        summary = summary,
                        isPlaying = playingSummaryId == summary.id,
                        onPlayRequest = {
                            playingSummaryId =
                                if (playingSummaryId == summary.id) null else summary.id
                        },
                        onShowSnackbar = onShowSnackbar,
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
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (historySummaries.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeFilters(
    selectedFilter: SummaryType?,
    onFilterChanged: (SummaryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SummaryType.entries) { filter ->
            val selected = selectedFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterChanged(filter) },
                label = {
                    Text(
                        text = filter.name.lowercase()
                            .replaceFirstChar { it.titlecase() })
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (selected) Icons.Default.Check else filter.icon,
                        contentDescription = filter.name
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableSummaryCard(
    summary: HistorySummary,
    isPlaying: Boolean,
    onPlayRequest: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(CardDefaults.shape),
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color.Transparent
                }, label = "background color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = CardDefaults.shape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }) {
        SummaryCard(
            modifier = Modifier
                .fillMaxWidth(),
            summary = SummaryOutput(
                title = summary.title,
                summary = summary.summary,
                author = summary.author,
                sourceLink = summary.sourceLink,
                isYoutubeLink = summary.isYoutubeLink,
                isBiliBiliLink = summary.isBiliBiliLink,
                length = summary.length,
            ),
            cardColors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            onShowSnackbar = onShowSnackbar,
            isPlaying = isPlaying,
            onPlayRequest = onPlayRequest
        )
    }
}
