package me.nanova.summaryexpressive.data.converters

import androidx.room3.ColumnTypeConverter
import me.nanova.summaryexpressive.model.SummaryLength

class SummaryLengthConverter {
    @ColumnTypeConverter
    fun fromSummaryLength(value: SummaryLength): String {
        return value.name
    }

    @ColumnTypeConverter
    fun toSummaryLength(value: String): SummaryLength {
        return SummaryLength.valueOf(value)
    }
}