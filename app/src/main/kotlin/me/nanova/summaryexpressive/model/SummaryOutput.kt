package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable

@Serializable
data class SummaryOutput(
    override val title: String,
    override val author: String,
    override val summary: String,
    val sourceLink: String? = null,
    val isYoutubeLink: Boolean,
    val isBiliBiliLink: Boolean,
    val length: SummaryLength,
    val provider: String? = null,
    val model: String? = null,
    val overview: String? = null,
    val keyPoints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val detectedLanguage: String? = null,
    val errorReason: String? = null,
) : SummaryData
