package me.nanova.summaryexpressive.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.nanova.summaryexpressive.domain.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.model.UserSettings
import javax.inject.Inject

class GetUserSettingsUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
) {
    operator fun invoke(): Flow<UserSettings> = combine(
        userPreferencesRepository.preferencesFlow,
        aiProviderConfigRepository.providerConfigsFlow,
    ) { preferences, providerConfigs ->
        UserSettings(
            preferences = preferences,
            providerConfigs = providerConfigs,
        )
    }
}
