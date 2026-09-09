package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.domain.usecase.DeleteHistorySummaryUseCase
import me.nanova.summaryexpressive.domain.usecase.GetHistorySummariesUseCase
import me.nanova.summaryexpressive.domain.usecase.RestoreHistorySummaryUseCase
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistorySummariesUseCase: GetHistorySummariesUseCase,
    private val deleteHistorySummaryUseCase: DeleteHistorySummaryUseCase,
    private val restoreHistorySummaryUseCase: RestoreHistorySummaryUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<SummaryType?>(null)
    val filterType: StateFlow<SummaryType?> = _filterType.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedSearchText = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySummaries: Flow<PagingData<HistorySummary>> =
        combine(debouncedSearchText, _filterType) { text, type ->
            Pair(text, type)
        }.flatMapLatest { (text, type) ->
            getHistorySummariesUseCase(text, type)
        }.cachedIn(viewModelScope)

    fun onSearchTextChanged(text: String) {
        _searchQuery.value = text
    }

    fun onFilterChanged(type: SummaryType) {
        _filterType.value = if (_filterType.value == type) null else type
    }

    fun deleteSummary(id: String) {
        viewModelScope.launch {
            deleteHistorySummaryUseCase(id)
        }
    }

    fun restoreSummary(summary: HistorySummary) {
        viewModelScope.launch {
            restoreHistorySummaryUseCase(summary)
        }
    }
}