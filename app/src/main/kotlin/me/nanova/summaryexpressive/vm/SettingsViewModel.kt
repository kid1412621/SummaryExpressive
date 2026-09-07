package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.data.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.defaultSystemPromptPlaceholder
import me.nanova.summaryexpressive.model.ProviderConfig
import me.nanova.summaryexpressive.model.SummaryLength
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
) : ViewModel() {

    private val _summaryLength = MutableStateFlow<SummaryLength?>(null)
    private val _activeProvider = MutableStateFlow<AIProvider?>(null)
    private val _activeModel = MutableStateFlow<String?>(null)

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferencesFlow,
        aiProviderConfigRepository.providerConfigsFlow,
        _summaryLength,
        _activeProvider,
        _activeModel,
    ) { prefs, providerConfigs, immediateLength, immediateProvider, immediateModel ->
        val effectiveProvider = immediateProvider
            ?: prefs.activeProvider?.let { AIProvider.entries.find { p -> p.name == it } }
        val providerConfig = effectiveProvider?.name?.let { providerConfigs[it] }
        val storedLength = SummaryLength.entries.find { it.name == prefs.summaryLength }
            ?: SummaryLength.MEDIUM
        val effectiveLength = immediateLength ?: storedLength
        val effectiveModel = immediateModel
            ?: providerConfig?.activeModel?.takeIf { it.isNotBlank() }

        SettingsUiState(
            useOriginalLanguage = prefs.useOriginalLanguage,
            dynamicColor = prefs.dynamicColor,
            theme = prefs.theme,
            apiKey = providerConfig?.apiKey?.takeIf { it.isNotBlank() },
            baseUrl = providerConfig?.baseUrl?.takeIf { it.isNotBlank() },
            activeProvider = effectiveProvider,
            providerConfigs = providerConfigs,
            activeModel = effectiveModel,
            showLength = prefs.showLength,
            summaryLength = effectiveLength,
            autoExtractUrl = prefs.autoExtractUrl,
            bilibiliSessData = prefs.bilibiliSessData,
            bilibiliSessDataExpires = prefs.bilibiliSessDataExpires,
            isAppendMode = prefs.isAppendMode,
            customBasePrompt = prefs.customBasePrompt,
            additionalSystemPrompt = prefs.additionalSystemPrompt,
            providerOrder = prefs.providerOrder,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    // Original Language in summary
    fun setUseOriginalLanguageValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setUseOriginalLanguage, newValue)

    // Dynamic color
    fun setDynamicColorValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setDynamicColor, newValue)

    // Theme for Dark, Light or System
    fun setTheme(newValue: Int) =
        savePreference(userPreferencesRepository::setTheme, newValue)

    // API Key
    fun setApiKeyValue(newValue: String, provider: String? = null) {
        viewModelScope.launch {
            val targetProvider = provider
                ?: _activeProvider.value?.name
                ?: settingsUiState.value.activeProvider?.name
                ?: userPreferencesRepository.preferencesFlow.first().activeProvider
                ?: return@launch
            aiProviderConfigRepository.updateApiKey(targetProvider, newValue.trim())
        }
    }

    // API base url
    fun setBaseUrlValue(newValue: String, provider: String? = null) {
        val baseUrl = normalizeBaseUrl(newValue)
        viewModelScope.launch {
            val targetProvider = provider
                ?: _activeProvider.value?.name
                ?: settingsUiState.value.activeProvider?.name
                ?: userPreferencesRepository.preferencesFlow.first().activeProvider
                ?: return@launch
            aiProviderConfigRepository.updateBaseUrl(targetProvider, baseUrl)
        }
    }

    // AI provider
    fun setAIProviderValue(newValue: String) {
        val providerEnum = AIProvider.entries.find { it.name == newValue }
        _activeProvider.value = providerEnum
        _activeModel.value = null
        savePreference(userPreferencesRepository::setActiveProvider, newValue)
    }

    fun setProviderOrder(order: List<String>) =
        savePreference(userPreferencesRepository::setProviderOrder, order)

    fun setProviderConfig(
        provider: String,
        baseUrl: String,
        apiKey: String,
        providerOrder: List<String>? = null,
    ) {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val providerEnum = AIProvider.entries.find { it.name == provider }
        _activeProvider.value = providerEnum
        _activeModel.value = null
        viewModelScope.launch {
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
    }

    // Model
    fun setModel(newValue: String, provider: AIProvider? = null) {
        _activeModel.value = newValue
        viewModelScope.launch {
            val targetProvider = provider
                ?: _activeProvider.value
                ?: settingsUiState.value.activeProvider
                ?: userPreferencesRepository.preferencesFlow.first().activeProvider?.let { name ->
                    AIProvider.entries.find { it.name == name }
                }
                ?: return@launch
            updateModelForProvider(targetProvider.name, newValue)
        }
    }

    fun setModelForProvider(provider: String, model: String) {
        val currentActive =
            _activeProvider.value?.name ?: settingsUiState.value.activeProvider?.name
        if (currentActive == provider) {
            _activeModel.value = model
        }
        viewModelScope.launch {
            updateModelForProvider(provider, model)
        }
    }

    private suspend fun updateModelForProvider(provider: String, model: String) {
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        val models =
            if (currentConfig.models.isNotEmpty() && !currentConfig.models.contains(model)) {
                currentConfig.models + model
            } else {
                currentConfig.models
            }
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(activeModel = model, models = models)
        )
    }

    fun setProviderModels(provider: String, models: List<String>, selectedModel: String? = null) {
        viewModelScope.launch {
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
    }

    fun resetProviderModelsToDefault(provider: String) {
        viewModelScope.launch {
            val aiProvider =
                AIProvider.entries.find { it.name == provider } ?: return@launch
            val defaultModels = aiProvider.defaultModelIds
            val currentConfig =
                aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
            val defaultModel = defaultModels.firstOrNull() ?: ""
            aiProviderConfigRepository.saveConfig(
                provider,
                currentConfig.copy(models = defaultModels, activeModel = defaultModel)
            )
        }
    }

    // Show length
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowLength, newValue)

    // Summary Length
    fun setSummaryLength(newValue: SummaryLength) {
        _summaryLength.value = newValue
        savePreference(userPreferencesRepository::setSummaryLength, newValue.name)
    }

    // Auto extract url
    fun setAutoExtractUrlValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setAutoExtractUrl, newValue)

    // BiliBili SESSDATA
    fun setBilibiliSessData(data: String, expires: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setBilibiliSessData(data, expires)
        }
    }

    fun clearBilibiliSessData() {
        viewModelScope.launch {
            userPreferencesRepository.clearBilibiliSessData()
        }
    }

    // Advanced Setup
    fun setIsAppendMode(newValue: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIsAppendMode(newValue)
            if (!newValue) {
                val currentPrompt =
                    userPreferencesRepository.preferencesFlow.first().customBasePrompt
                if (currentPrompt.isEmpty()) {
                    userPreferencesRepository.setCustomBasePrompt(defaultSystemPromptPlaceholder)
                }
            }
        }
    }

    fun setCustomBasePrompt(newValue: String) =
        savePreference(userPreferencesRepository::setCustomBasePrompt, newValue)

    fun setAdditionalSystemPrompt(newValue: String) =
        savePreference(userPreferencesRepository::setAdditionalSystemPrompt, newValue)

    // --- Preference Handling Helpers ---
    private fun <T> savePreference(setter: suspend (T) -> Unit, value: T) {
        viewModelScope.launch {
            setter(value)
        }
    }

    private fun normalizeBaseUrl(url: String): String {
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
}
