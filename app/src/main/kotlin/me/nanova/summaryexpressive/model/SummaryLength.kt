package me.nanova.summaryexpressive.model

enum class SummaryLength {
    SHORT,
    MEDIUM,
    LONG,
    NONE;

    companion object {
        val userSelectable: List<SummaryLength> = listOf(SHORT, MEDIUM, LONG)
    }
}

