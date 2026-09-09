package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class HistorySummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val author: String = "",
    val createdOn: Long = System.currentTimeMillis(),
    val length: SummaryLength,
    val type: SummaryType,
    val subtype: VideoSubtype? = null,
    val sourceLink: String? = null,
    val sourceText: String? = null,
    val provider: String? = null,
    val model: String? = null,
) {
    val isYoutubeLink: Boolean
        get() = type == SummaryType.VIDEO && subtype == VideoSubtype.YOUTUBE

    val isBiliBiliLink: Boolean
        get() = type == SummaryType.VIDEO && subtype == VideoSubtype.BILIBILI
}