package me.nanova.summaryexpressive.domain.usecase

import kotlinx.coroutines.flow.first
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.defaultSystemPromptPlaceholder
import javax.inject.Inject

class UpdateUserPreferencesUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend fun setUseOriginalLanguage(value: Boolean) {
        userPreferencesRepository.setUseOriginalLanguage(value)
    }

    suspend fun setDynamicColor(value: Boolean) {
        userPreferencesRepository.setDynamicColor(value)
    }

    suspend fun setTheme(value: Int) {
        userPreferencesRepository.setTheme(value)
    }

    suspend fun setActiveProvider(value: String?) {
        userPreferencesRepository.setActiveProvider(value)
    }

    suspend fun setProviderOrder(value: List<String>) {
        userPreferencesRepository.setProviderOrder(value)
    }

    suspend fun setShowLength(value: Boolean) {
        userPreferencesRepository.setShowLength(value)
    }

    suspend fun setSummaryLength(value: String) {
        userPreferencesRepository.setSummaryLength(value)
    }

    suspend fun setAutoExtractUrl(value: Boolean) {
        userPreferencesRepository.setAutoExtractUrl(value)
    }

    suspend fun setBilibiliSessData(data: String, expires: Long) {
        userPreferencesRepository.setBilibiliSessData(data, expires)
    }

    suspend fun clearBilibiliSessData() {
        userPreferencesRepository.clearBilibiliSessData()
    }

    suspend fun setIsAppendMode(value: Boolean) {
        userPreferencesRepository.setIsAppendMode(value)
        if (!value) {
            val currentPrompt = userPreferencesRepository.preferencesFlow.first().customBasePrompt
            if (currentPrompt.isEmpty()) {
                userPreferencesRepository.setCustomBasePrompt(defaultSystemPromptPlaceholder)
            }
        }
    }

    suspend fun setCustomBasePrompt(value: String) {
        userPreferencesRepository.setCustomBasePrompt(value)
    }

    suspend fun setAdditionalSystemPrompt(value: String) {
        userPreferencesRepository.setAdditionalSystemPrompt(value)
    }
}
