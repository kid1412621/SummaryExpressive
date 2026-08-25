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
import me.nanova.summaryexpressive.data.repository.HistoryRepository
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import javax.inject.Inject
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val searchState = TextFieldState()

    private val _filterType = MutableStateFlow<SummaryType?>(null)
    val filterType: StateFlow<SummaryType?> = _filterType.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val debouncedSearchText = snapshotFlow { searchState.text }
        .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySummaries: Flow<PagingData<HistorySummary>> =
        combine(debouncedSearchText, _filterType) { text, type ->
            Pair(text.toString(), type)
        }.flatMapLatest { (text, type) ->
            historyRepository.getSummaries(text, type)
        }.cachedIn(viewModelScope)


    fun onSearchTextChanged(text: String) {
        searchState.edit {
            replace(0, length, text)
        }
    }

    fun onFilterChanged(type: SummaryType) {
        _filterType.value = if (_filterType.value == type) null else type
    }

    suspend fun addHistorySummary(summary: HistorySummary) {
        historyRepository.addSummary(summary)
    }

    suspend fun removeHistorySummary(id: String) {
        historyRepository.deleteSummary(id)
    }
}