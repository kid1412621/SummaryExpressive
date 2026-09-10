package me.nanova.summaryexpressive.data.converters

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.SummaryLength

class HistoryLengthResultsConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromLengthResults(value: Map<SummaryLength, HistoryLengthResult>?): String? {
        return value?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toLengthResults(value: String?): Map<SummaryLength, HistoryLengthResult>? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString<Map<SummaryLength, HistoryLengthResult>>(value)
        }.getOrNull()
    }
}
