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
) : SummaryData
