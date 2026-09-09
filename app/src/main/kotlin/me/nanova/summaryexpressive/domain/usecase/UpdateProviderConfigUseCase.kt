package me.nanova.summaryexpressive.domain.usecase

import kotlinx.coroutines.flow.first
import me.nanova.summaryexpressive.domain.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.ProviderConfig
import javax.inject.Inject

class UpdateProviderConfigUseCase @Inject constructor(
    private val aiProviderConfigRepository: AIProviderConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.isBlank() -> ""
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith(
                "https://",
                ignoreCase = true
            ) -> trimmed
            else -> "https://$trimmed"
        }
    }

    suspend fun updateApiKey(provider: String? = null, apiKey: String) {
        val targetProvider = provider
            ?: userPreferencesRepository.preferencesFlow.first().activeProvider
            ?: return
        aiProviderConfigRepository.updateApiKey(targetProvider, apiKey.trim())
    }

    suspend fun updateBaseUrl(provider: String? = null, baseUrl: String) {
        val targetProvider = provider
            ?: userPreferencesRepository.preferencesFlow.first().activeProvider
            ?: return
        aiProviderConfigRepository.updateBaseUrl(targetProvider, normalizeBaseUrl(baseUrl))
    }

    suspend fun setProviderConfig(
        provider: String,
        baseUrl: String,
        apiKey: String,
        providerOrder: List<String>? = null,
    ) {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(apiKey = apiKey.trim(), baseUrl = normalizedBaseUrl)
        )
        userPreferencesRepository.setActiveProvider(provider)
        if (providerOrder != null) {
            userPreferencesRepository.setProviderOrder(providerOrder)
        }
    }

    suspend fun updateModelForProvider(provider: String? = null, model: String) {
        val targetProvider = provider
            ?: userPreferencesRepository.preferencesFlow.first().activeProvider
            ?: return
        val currentConfig = aiProviderConfigRepository.getConfig(targetProvider) ?: ProviderConfig()
        val models =
            if (currentConfig.models.isNotEmpty() && !currentConfig.models.contains(model)) {
                currentConfig.models + model
            } else {
                currentConfig.models
            }
        aiProviderConfigRepository.saveConfig(
            targetProvider,
            currentConfig.copy(activeModel = model, models = models)
        )
    }

    suspend fun setProviderModels(provider: String, models: List<String>, selectedModel: String? = null) {
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        val targetModel =
            selectedModel ?: currentConfig.activeModel.takeIf { it in models }
            ?: models.firstOrNull()
            ?: ""
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(models = models, activeModel = targetModel)
        )
    }

    suspend fun resetProviderModelsToDefault(provider: String) {
        val aiProvider = AIProvider.entries.find { it.name == provider } ?: return
        val defaultModels = aiProvider.defaultModelIds
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        val defaultModel = defaultModels.firstOrNull() ?: ""
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(models = defaultModels, activeModel = defaultModel)
        )
    }
}
