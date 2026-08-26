package me.nanova.summaryexpressive.data.converters

import androidx.room.TypeConverter
import me.nanova.summaryexpressive.model.SummaryLength

class SummaryLengthConverter {
    @TypeConverter
    fun fromSummaryLength(value: SummaryLength): String {
        return value.name
    }

    @TypeConverter
    fun toSummaryLength(value: String): SummaryLength {
        return SummaryLength.valueOf(value)
    }
}