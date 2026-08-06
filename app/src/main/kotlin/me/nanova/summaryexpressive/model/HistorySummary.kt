package me.nanova.summaryexpressive.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.data.converters.SummaryLengthConverter
import me.nanova.summaryexpressive.data.converters.SummaryTypeConverter
import me.nanova.summaryexpressive.data.converters.VideoSubtypeConverter
import me.nanova.summaryexpressive.llm.SummaryLength
import java.util.UUID

@Serializable
@Entity(tableName = "history")
@TypeConverters(
    SummaryLengthConverter::class,
    SummaryTypeConverter::class,
    VideoSubtypeConverter::class
)
data class HistorySummary(
    @PrimaryKey
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
    @ColumnInfo(defaultValue = "NULL")
    val provider: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val model: String? = null,
) {
    val isYoutubeLink: Boolean
        get() = type == SummaryType.VIDEO && subtype == VideoSubtype.YOUTUBE

    val isBiliBiliLink: Boolean
        get() = type == SummaryType.VIDEO && subtype == VideoSubtype.BILIBILI
}