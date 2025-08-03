package me.nanova.summaryexpressive.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import me.nanova.summaryexpressive.UserPreferencesRepository
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
        }
    }
}