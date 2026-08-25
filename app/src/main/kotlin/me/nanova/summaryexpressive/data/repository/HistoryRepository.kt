package me.nanova.summaryexpressive.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.nanova.summaryexpressive.data.HistoryDao
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 20

@Singleton
class HistoryRepository @Inject constructor(private val historyDao: HistoryDao) {
    fun getSummaries(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { historyDao.getSummaries(query, type) }
        ).flow
    }

    suspend fun addSummary(summary: HistorySummary) {
        historyDao.insert(summary)
    }

    suspend fun deleteSummary(id: String) {
        historyDao.deleteById(id)
    }
}
