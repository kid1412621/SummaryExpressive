package me.nanova.summaryexpressive.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import me.nanova.summaryexpressive.data.converters.HistoryLengthResultsConverter
import me.nanova.summaryexpressive.data.converters.SummaryLengthConverter
import me.nanova.summaryexpressive.data.converters.SummaryTypeConverter
import me.nanova.summaryexpressive.data.converters.VideoSubtypeConverter
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype

@Entity(tableName = "history")
@TypeConverters(
    SummaryLengthConverter::class,
    SummaryTypeConverter::class,
    VideoSubtypeConverter::class,
    HistoryLengthResultsConverter::class,
)
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val author: String = "",
    val createdOn: Long,
    val length: SummaryLength,
    val type: SummaryType,
    val subtype: VideoSubtype? = null,
    val sourceLink: String? = null,
    val sourceText: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val provider: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val model: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val lengthResults: Map<SummaryLength, HistoryLengthResult>? = null,
)
