package me.nanova.summaryexpressive.di

import android.content.Context
import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
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
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.LLMHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val migrationV2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE history ADD COLUMN provider TEXT DEFAULT NULL")
                connection.execSQL("ALTER TABLE history ADD COLUMN model TEXT DEFAULT NULL")
                connection.execSQL("ALTER TABLE history ADD COLUMN lengthResults TEXT DEFAULT NULL")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `ai_provider_config` (`provider` TEXT NOT NULL, `apiKey` TEXT NOT NULL, `baseUrl` TEXT NOT NULL, `active_model` TEXT NOT NULL, `models` TEXT DEFAULT NULL, PRIMARY KEY(`provider`))")
            }
        }

        return Room.databaseBuilder<AppDatabase>(
            context,
            "summary_expressive_db"
        )
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(migrationV2)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
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