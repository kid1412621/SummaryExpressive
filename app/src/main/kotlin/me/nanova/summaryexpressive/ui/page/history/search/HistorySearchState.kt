package me.nanova.summaryexpressive.ui.page.history.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.vm.HistoryViewModel

/**
 * State holder for History search and filter state
 */
class HistorySearchState(
    val query: String,
    val selectedFilter: SummaryType?,
    val onQueryChange: (String) -> Unit,
    val onClear: () -> Unit,
    val onFilterChanged: (SummaryType) -> Unit,
) {
    val isSearching: Boolean
        get() = query.isNotBlank() || selectedFilter != null
}

@Composable
fun rememberHistorySearchState(viewModel: HistoryViewModel): HistorySearchState {
    val selectedFilter by viewModel.filterType.collectAsState()
    val query = viewModel.searchState.text.toString()

    return remember(query, selectedFilter, viewModel) {
        HistorySearchState(
            query = query,
            selectedFilter = selectedFilter,
            onQueryChange = viewModel::onSearchTextChanged,
            onClear = { viewModel.onSearchTextChanged("") },
            onFilterChanged = viewModel::onFilterChanged
        )
    }
}
