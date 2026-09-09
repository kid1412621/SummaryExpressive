package me.nanova.summaryexpressive.domain.usecase

import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.model.HistorySummary
import javax.inject.Inject

class RestoreHistorySummaryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(summary: HistorySummary) {
        historyRepository.addSummary(summary)
    }
}
