package me.nanova.summaryexpressive.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.nanova.summaryexpressive.data.provider.AndroidAppLocaleProvider
import me.nanova.summaryexpressive.data.provider.AndroidDocumentMetadataProvider
import me.nanova.summaryexpressive.data.repository.AIProviderConfigRepositoryImpl
import me.nanova.summaryexpressive.data.repository.HistoryRepositoryImpl
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepositoryImpl
import me.nanova.summaryexpressive.domain.provider.AppLocaleProvider
import me.nanova.summaryexpressive.domain.provider.DocumentMetadataProvider
import me.nanova.summaryexpressive.domain.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.domain.usecase.SummarizeContentUseCase
import me.nanova.summaryexpressive.domain.usecase.SummarizeContentUseCaseImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAIProviderConfigRepository(impl: AIProviderConfigRepositoryImpl): AIProviderConfigRepository

    @Binds
    @Singleton
    abstract fun bindDocumentMetadataProvider(impl: AndroidDocumentMetadataProvider): DocumentMetadataProvider

    @Binds
    @Singleton
    abstract fun bindAppLocaleProvider(impl: AndroidAppLocaleProvider): AppLocaleProvider

    @Binds
    @Singleton
    abstract fun bindSummarizeContentUseCase(impl: SummarizeContentUseCaseImpl): SummarizeContentUseCase
}
