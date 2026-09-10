package me.nanova.summaryexpressive.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.nanova.summaryexpressive.data.HistoryDao
import me.nanova.summaryexpressive.data.local.database.entity.HistoryEntity
import me.nanova.summaryexpressive.data.local.database.mapper.toDomain
import me.nanova.summaryexpressive.data.local.database.mapper.toEntity
import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 20

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {
    private var isDeduplicated = false
    private val deduplicationMutex = Mutex()

    override fun getSummaries(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { historyDao.getSummaries(query, type) }
        ).flow
            .onStart { ensureDeduplicated() }
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
    }

    override suspend fun addSummary(summary: HistorySummary) {
        val existingRecords = findExistingRecords(summary)
        if (existingRecords.isNotEmpty()) {
            val primary = existingRecords.first().toDomain()
            val mergedLengthResults = mutableMapOf<SummaryLength, HistoryLengthResult>()
            for (record in existingRecords) {
                mergedLengthResults.putAll(record.toDomain().allLengthResults)
            }
            val newResult = HistoryLengthResult(
                length = summary.length,
                summary = summary.summary,
                provider = summary.provider,
                model = summary.model,
            )
            mergedLengthResults[summary.length] = newResult

            val updated = primary.copy(
                title = summary.title.ifBlank { primary.title },
                author = summary.author.ifBlank { primary.author },
                summary = summary.summary,
                length = summary.length,
                createdOn = System.currentTimeMillis(),
                provider = summary.provider,
                model = summary.model,
                lengthResults = mergedLengthResults,
            )
            historyDao.insert(updated.toEntity())

            val extraIds = existingRecords.drop(1).map { it.id }
            if (extraIds.isNotEmpty()) {
                historyDao.deleteByIds(extraIds)
            }
        } else {
            val initialResults = summary.allLengthResults
            val newSummary = summary.copy(lengthResults = initialResults)
            historyDao.insert(newSummary.toEntity())
        }
    }

    override suspend fun deleteSummary(id: String) {
        historyDao.deleteById(id)
    }

    private suspend fun findExistingRecords(summary: HistorySummary): List<HistoryEntity> {
        val link = summary.sourceLink?.trim()
        if (!link.isNullOrBlank()) {
            val records = historyDao.getAllBySourceLink(link)
            if (records.isNotEmpty()) return records
        }
        val text = summary.sourceText?.trim()
        if (!text.isNullOrBlank()) {
            val records = historyDao.getAllBySourceText(text)
            if (records.isNotEmpty()) return records
        }
        return emptyList()
    }

    private suspend fun ensureDeduplicated() {
        if (isDeduplicated) return
        deduplicationMutex.withLock {
            if (isDeduplicated) return
            runCatching {
                val all = historyDao.getAll()
                val grouped = all.groupBy { entity ->
                    when {
                        !entity.sourceLink.isNullOrBlank() -> "link:" + entity.sourceLink.trim()
                        !entity.sourceText.isNullOrBlank() -> "text:" + entity.sourceText.trim()
                        else -> "id:" + entity.id
                    }
                }
                for ((_, group) in grouped) {
                    if (group.size > 1) {
                        val latest = group.maxByOrNull { it.createdOn } ?: group.first()
                        val mergedLengths = mutableMapOf<SummaryLength, HistoryLengthResult>()
                        for (item in group) {
                            mergedLengths.putAll(item.toDomain().allLengthResults)
                        }
                        val updated = latest.toDomain().copy(
                            lengthResults = mergedLengths
                        )
                        historyDao.insert(updated.toEntity())
                        val toDelete = group.filter { it.id != latest.id }.map { it.id }
                        if (toDelete.isNotEmpty()) {
                            historyDao.deleteByIds(toDelete)
                        }
                    }
                }
            }
            isDeduplicated = true
        }
    }
}
