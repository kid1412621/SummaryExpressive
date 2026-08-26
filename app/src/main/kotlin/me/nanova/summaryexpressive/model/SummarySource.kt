package me.nanova.summaryexpressive.model

sealed class SummarySource {
    data class Video(val url: String) : SummarySource()
    data class Article(val url: String) : SummarySource()
    data class Text(val content: String) : SummarySource()
    data class Document(val filename: String, val uri: String) : SummarySource()
    data object None : SummarySource()
}
