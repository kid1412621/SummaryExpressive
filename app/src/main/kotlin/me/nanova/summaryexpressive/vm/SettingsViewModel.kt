package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.domain.usecase.GetUserSettingsUseCase
import me.nanova.summaryexpressive.domain.usecase.UpdateProviderConfigUseCase
import me.nanova.summaryexpressive.domain.usecase.UpdateUserPreferencesUseCase
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.SummaryLength
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
    private val updateProviderConfigUseCase: UpdateProviderConfigUseCase,
) : ViewModel() {

    private val _summaryLength = MutableStateFlow<SummaryLength?>(null)
    private val _activeProvider = MutableStateFlow<AIProvider?>(null)
    private val _activeModel = MutableStateFlow<String?>(null)

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        getUserSettingsUseCase(),
        _summaryLength,
        _activeProvider,
        _activeModel,
    ) { userSettings, immediateLength, immediateProvider, immediateModel ->
        val prefs = userSettings.preferences
        val providerConfigs = userSettings.providerConfigs
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
        savePreference(updateUserPreferencesUseCase::setUseOriginalLanguage, newValue)

    // Dynamic color
    fun setDynamicColorValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setDynamicColor, newValue)

    // Theme for Dark, Light or System
    fun setTheme(newValue: Int) =
        savePreference(updateUserPreferencesUseCase::setTheme, newValue)

    // API Key
    fun setApiKeyValue(newValue: String, provider: String? = null) {
        viewModelScope.launch {
            val targetProvider = provider
                ?: _activeProvider.value?.name
                ?: settingsUiState.value.activeProvider?.name
            updateProviderConfigUseCase.updateApiKey(targetProvider, newValue)
        }
    }

    // API base url
    fun setBaseUrlValue(newValue: String, provider: String? = null) {
        viewModelScope.launch {
            val targetProvider = provider
                ?: _activeProvider.value?.name
                ?: settingsUiState.value.activeProvider?.name
            updateProviderConfigUseCase.updateBaseUrl(targetProvider, newValue)
        }
    }

    // AI provider
    fun setAIProviderValue(newValue: String) {
        val providerEnum = AIProvider.entries.find { it.name == newValue }
        _activeProvider.value = providerEnum
        _activeModel.value = null
        savePreference(updateUserPreferencesUseCase::setActiveProvider, newValue)
    }

    fun setProviderAndModel(provider: AIProvider, model: String) {
        _activeProvider.value = provider
        _activeModel.value = model
        savePreference(updateUserPreferencesUseCase::setActiveProvider, provider.name)
        viewModelScope.launch {
            updateModelForProvider(provider.name, model)
        }
    }

    fun setProviderOrder(order: List<String>) =
        savePreference(updateUserPreferencesUseCase::setProviderOrder, order)

    fun setProviderConfig(
        provider: String,
        baseUrl: String,
        apiKey: String,
        providerOrder: List<String>? = null,
    ) {
        val providerEnum = AIProvider.entries.find { it.name == provider }
        _activeProvider.value = providerEnum
        _activeModel.value = null
        viewModelScope.launch {
            updateProviderConfigUseCase.setProviderConfig(
                provider = provider,
                baseUrl = baseUrl,
                apiKey = apiKey,
                providerOrder = providerOrder,
            )
        }
    }

    // Model
    fun setModel(newValue: String, provider: AIProvider? = null) {
        _activeModel.value = newValue
        viewModelScope.launch {
            val targetProvider = provider
                ?: _activeProvider.value
                ?: settingsUiState.value.activeProvider
            updateModelForProvider(targetProvider?.name, newValue)
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

    private suspend fun updateModelForProvider(provider: String?, model: String) {
        updateProviderConfigUseCase.updateModelForProvider(provider, model)
    }

    fun setProviderModels(provider: String, models: List<String>, selectedModel: String? = null) {
        viewModelScope.launch {
            updateProviderConfigUseCase.setProviderModels(provider, models, selectedModel)
        }
    }

    fun resetProviderModelsToDefault(provider: String) {
        viewModelScope.launch {
            updateProviderConfigUseCase.resetProviderModelsToDefault(provider)
        }
    }

    // Show length
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setShowLength, newValue)

    // Summary Length
    fun setSummaryLength(newValue: SummaryLength) {
        _summaryLength.value = newValue
        savePreference(updateUserPreferencesUseCase::setSummaryLength, newValue.name)
    }

    // Auto extract url
    fun setAutoExtractUrlValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setAutoExtractUrl, newValue)

    // BiliBili SESSDATA
    fun setBilibiliSessData(data: String, expires: Long) {
        viewModelScope.launch {
            updateUserPreferencesUseCase.setBilibiliSessData(data, expires)
        }
    }

    fun clearBilibiliSessData() {
        viewModelScope.launch {
            updateUserPreferencesUseCase.clearBilibiliSessData()
        }
    }

    // Advanced Setup
    fun setIsAppendMode(newValue: Boolean) {
        viewModelScope.launch {
            updateUserPreferencesUseCase.setIsAppendMode(newValue)
        }
    }

    fun setCustomBasePrompt(newValue: String) =
        savePreference(updateUserPreferencesUseCase::setCustomBasePrompt, newValue)

    fun setAdditionalSystemPrompt(newValue: String) =
        savePreference(updateUserPreferencesUseCase::setAdditionalSystemPrompt, newValue)

    // --- Preference Handling Helpers ---
    private fun <T> savePreference(setter: suspend (T) -> Unit, value: T) {
        viewModelScope.launch {
            setter(value)
        }
    }
}
