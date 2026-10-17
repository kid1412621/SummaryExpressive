package me.nanova.summaryexpressive.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import me.nanova.summaryexpressive.data.local.database.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class, AIProviderConfigEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun aiProviderConfigDao(): AIProviderConfigDao
}