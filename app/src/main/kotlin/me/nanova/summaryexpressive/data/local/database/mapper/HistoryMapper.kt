package me.nanova.summaryexpressive.data.local.database.mapper

import me.nanova.summaryexpressive.data.local.database.entity.HistoryEntity
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.HistorySummary

fun HistoryEntity.toDomain(): HistorySummary {
    val results = lengthResults?.takeIf { it.isNotEmpty() } ?: mapOf(
        length to HistoryLengthResult(
            length = length,
            summary = summary,
            provider = provider,
            model = model,
        )
    )
    return HistorySummary(
        id = id,
        title = title,
        author = author,
        createdOn = createdOn,
        length = length,
        type = type,
        subtype = subtype,
        sourceLink = sourceLink,
        sourceText = sourceText,
        lengthResults = results,
    )
}

fun HistorySummary.toEntity(): HistoryEntity = HistoryEntity(
    id = id,
    title = title,
    summary = summary,
    author = author,
    createdOn = createdOn,
    length = length,
    type = type,
    subtype = subtype,
    sourceLink = sourceLink,
    sourceText = sourceText,
    provider = provider,
    model = model,
    lengthResults = lengthResults.takeIf { it.isNotEmpty() },
)
