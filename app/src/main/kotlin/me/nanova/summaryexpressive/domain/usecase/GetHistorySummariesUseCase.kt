package me.nanova.summaryexpressive.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import javax.inject.Inject

class GetHistorySummariesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>> {
        return historyRepository.getSummaries(query, type)
    }
}
