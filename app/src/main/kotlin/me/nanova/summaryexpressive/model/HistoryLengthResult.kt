package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryLengthResult(
    val length: SummaryLength,
    val summary: String,
    val provider: String? = null,
    val model: String? = null,
    val overview: String? = null,
    val keyPoints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)
