package me.nanova.summaryexpressive.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.nanova.summaryexpressive.data.HistoryDao
import me.nanova.summaryexpressive.data.local.database.mapper.toDomain
import me.nanova.summaryexpressive.data.local.database.mapper.toEntity
import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 20

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {
    override fun getSummaries(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { historyDao.getSummaries(query, type) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun addSummary(summary: HistorySummary) {
        historyDao.insert(summary.toEntity())
    }

    override suspend fun deleteSummary(id: String) {
        historyDao.deleteById(id)
    }
}
