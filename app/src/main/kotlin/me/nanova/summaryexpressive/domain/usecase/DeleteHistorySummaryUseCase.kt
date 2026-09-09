package me.nanova.summaryexpressive.domain.usecase

import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import javax.inject.Inject

class DeleteHistorySummaryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(id: String) {
        historyRepository.deleteSummary(id)
    }
}
