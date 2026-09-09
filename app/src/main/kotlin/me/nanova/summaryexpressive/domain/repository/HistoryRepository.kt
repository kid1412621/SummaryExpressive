package me.nanova.summaryexpressive.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType

interface HistoryRepository {
    fun getSummaries(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>>
    suspend fun addSummary(summary: HistorySummary)
    suspend fun deleteSummary(id: String)
}
