package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class HistorySummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String = "",
    val createdOn: Long = System.currentTimeMillis(),
    val length: SummaryLength = SummaryLength.NONE,
    val type: SummaryType,
    val subtype: VideoSubtype? = null,
    val sourceLink: String? = null,
    val sourceText: String? = null,
    val lengthResults: Map<SummaryLength, HistoryLengthResult> = emptyMap(),
) {

    val activeResult: HistoryLengthResult?
        get() = lengthResults[length] ?: lengthResults.values.firstOrNull()

    val summary: String get() = activeResult?.summary.orEmpty()
    val overview: String? get() = activeResult?.overview
    val keyPoints: List<String> get() = activeResult?.keyPoints.orEmpty()
    val tags: List<String> get() = activeResult?.tags.orEmpty()
    val provider: String? get() = activeResult?.provider
    val model: String? get() = activeResult?.model

    val allLengthResults: Map<SummaryLength, HistoryLengthResult>
        get() = lengthResults

    val hasLength: Boolean
        get() = length != SummaryLength.NONE

    val isYoutubeLink: Boolean
        get() = (type == SummaryType.VIDEO && subtype == VideoSubtype.YOUTUBE) ||
                isYouTubeLink(sourceLink)

    val isBiliBiliLink: Boolean
        get() = (type == SummaryType.VIDEO && subtype == VideoSubtype.BILIBILI) ||
                isBiliBiliLink(sourceLink)
}