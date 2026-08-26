package me.nanova.summaryexpressive.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import me.nanova.summaryexpressive.data.AIProviderConfigDao
import me.nanova.summaryexpressive.data.AppDatabase
import me.nanova.summaryexpressive.data.HistoryDao
import me.nanova.summaryexpressive.data.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.data.repository.HistoryRepository
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
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
        val migrationV2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE history ADD COLUMN provider TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE history ADD COLUMN model TEXT DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS `ai_provider_config` (`provider` TEXT NOT NULL, `apiKey` TEXT NOT NULL, `baseUrl` TEXT NOT NULL, `active_model` TEXT NOT NULL, `models` TEXT DEFAULT NULL, PRIMARY KEY(`provider`))")
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "summary_expressive_db"
        )
            .addMigrations(migrationV2)
            .build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }

    @Provides
    @Singleton
    fun provideAIProviderConfigDao(appDatabase: AppDatabase): AIProviderConfigDao {
        return appDatabase.aiProviderConfigDao()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository {
        return HistoryRepository(historyDao)
    }

    @Provides
    @Singleton
    fun provideAIProviderConfigRepository(aiProviderConfigDao: AIProviderConfigDao): AIProviderConfigRepository {
        return AIProviderConfigRepository(aiProviderConfigDao)
    }

    @Provides
    @Singleton
    fun provideLLMHandler(
        @ApplicationContext context: Context,
        httpClient: HttpClient,
        userPreferencesRepository: UserPreferencesRepository,
    ): LLMHandler {
        return LLMHandler(context, httpClient, userPreferencesRepository)
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