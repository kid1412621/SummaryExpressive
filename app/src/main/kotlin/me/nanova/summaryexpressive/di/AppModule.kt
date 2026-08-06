package me.nanova.summaryexpressive.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.data.AppDatabase
import me.nanova.summaryexpressive.data.HistoryDao
import me.nanova.summaryexpressive.data.HistoryRepository
import me.nanova.summaryexpressive.llm.LLMHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE history ADD COLUMN provider TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE history ADD COLUMN model TEXT DEFAULT NULL")
                database.execSQL("CREATE TABLE IF NOT EXISTS `ai_provider_config` (`provider` TEXT NOT NULL, `apiKey` TEXT NOT NULL, `baseUrl` TEXT NOT NULL, `model` TEXT NOT NULL, PRIMARY KEY(`provider`))")
            }
        }
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "summary_expressive_db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }

    @Provides
    @Singleton
    fun provideAIProviderConfigDao(appDatabase: AppDatabase): me.nanova.summaryexpressive.data.AIProviderConfigDao {
        return appDatabase.aiProviderConfigDao()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository {
        return HistoryRepository(historyDao)
    }

    @Provides
    @Singleton
    fun provideLLMHandler(
        @ApplicationContext context: Context,
        httpClient: HttpClient
    ): LLMHandler {
        return LLMHandler(context, httpClient)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) { json() }
        }
    }
}