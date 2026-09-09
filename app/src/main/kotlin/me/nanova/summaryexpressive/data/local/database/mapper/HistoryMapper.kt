package me.nanova.summaryexpressive.data.local.database.mapper

import me.nanova.summaryexpressive.data.local.database.entity.HistoryEntity
import me.nanova.summaryexpressive.model.HistorySummary

fun HistoryEntity.toDomain(): HistorySummary = HistorySummary(
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
)

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
)
