package me.nanova.summaryexpressive.data.converters

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class StringListConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString<List<String>>(value)
        }.getOrElse {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
