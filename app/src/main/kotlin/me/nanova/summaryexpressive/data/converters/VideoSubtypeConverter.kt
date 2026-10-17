package me.nanova.summaryexpressive.data.converters

import androidx.room3.ColumnTypeConverter
import me.nanova.summaryexpressive.model.VideoSubtype

class VideoSubtypeConverter {
    @ColumnTypeConverter
    fun fromVideoSubtype(value: VideoSubtype?): String? {
        return value?.name
    }

    @ColumnTypeConverter
    fun toVideoSubtype(value: String?): VideoSubtype? {
        return value?.let { VideoSubtype.valueOf(it) }
    }
}