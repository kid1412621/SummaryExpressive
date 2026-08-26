package me.nanova.summaryexpressive.data

import androidx.room.Database
import androidx.room.RoomDatabase
import me.nanova.summaryexpressive.model.HistorySummary

@Database(
    entities = [HistorySummary::class, AIProviderConfigEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun aiProviderConfigDao(): AIProviderConfigDao
}