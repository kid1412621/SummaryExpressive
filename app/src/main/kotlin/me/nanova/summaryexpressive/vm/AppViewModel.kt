package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.data.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.defaultSystemPromptPlaceholder
import me.nanova.summaryexpressive.model.ProviderConfig
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.ui.Nav
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
) : ViewModel() {

    val startDestination: StateFlow<Nav?> = userPreferencesRepository.preferencesFlow
        .map { if (it.isOnboarded) Nav.Home else Nav.Onboarding }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferencesFlow,
        aiProviderConfigRepository.providerConfigsFlow
    ) { prefs, providerConfigs ->
        val providerConfig = prefs.activeProvider?.let { providerConfigs[it] }
        SettingsUiState(
            useOriginalLanguage = prefs.useOriginalLanguage,
            dynamicColor = prefs.dynamicColor,
            theme = prefs.theme,
            apiKey = providerConfig?.apiKey?.takeIf { it.isNotBlank() },
            baseUrl = providerConfig?.baseUrl?.takeIf { it.isNotBlank() },
            activeProvider = prefs.activeProvider?.let { AIProvider.entries.find { p -> p.name == it } },
            providerConfigs = providerConfigs,
            activeModel = providerConfig?.activeModel?.takeIf { it.isNotBlank() },
            showLength = prefs.showLength,
            summaryLength = SummaryLength.entries.find { it.name == prefs.summaryLength } ?: SummaryLength.MEDIUM,
            autoExtractUrl = prefs.autoExtractUrl,
            sessData = prefs.sessData,
            sessDataExpires = prefs.sessDataExpires,
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
    fun setApiKeyValue(newValue: String) {
        viewModelScope.launch {
            val provider = userPreferencesRepository.preferencesFlow.first().activeProvider ?: return@launch
            aiProviderConfigRepository.updateApiKey(provider, newValue.trim())
        }
    }

    // API base url
    fun setBaseUrlValue(newValue: String) {
        val baseUrl = normalizeBaseUrl(newValue)
        viewModelScope.launch {
            val provider = userPreferencesRepository.preferencesFlow.first().activeProvider ?: return@launch
            aiProviderConfigRepository.updateBaseUrl(provider, baseUrl)
        }
    }

    // AI provider
    fun setAIProviderValue(newValue: String) =
        savePreference(userPreferencesRepository::setActiveProvider, newValue)

    fun setProviderOrder(order: List<String>) =
        savePreference(userPreferencesRepository::setProviderOrder, order)

    fun setProviderConfig(
        provider: String,
        baseUrl: String,
        apiKey: String,
        providerOrder: List<String>? = null,
    ) {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
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
    fun setModel(newValue: String) {
        viewModelScope.launch {
            val provider = userPreferencesRepository.preferencesFlow.first().activeProvider ?: return@launch
            updateModelForProvider(provider, newValue)
        }
    }

    fun setModelForProvider(provider: String, model: String) {
        viewModelScope.launch {
            updateModelForProvider(provider, model)
        }
    }

    private suspend fun updateModelForProvider(provider: String, model: String) {
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        val models = if (currentConfig.models.isNotEmpty() && !currentConfig.models.contains(model)) {
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
    fun setSummaryLength(newValue: SummaryLength) =
        savePreference(userPreferencesRepository::setSummaryLength, newValue.name)

    // Auto extract url
    fun setAutoExtractUrlValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setAutoExtractUrl, newValue)

    // OnboardingScreen
    fun setIsOnboarded(newValue: Boolean) =
        savePreference(userPreferencesRepository::setIsOnboarded, newValue)

    // BiliBili SESSDATA
    fun setSessData(data: String, expires: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setSessData(data, expires)
        }
    }

    fun clearSessData() {
        viewModelScope.launch {
            userPreferencesRepository.clearSessData()
        }
    }

    // Advanced Setup
    fun setIsAppendMode(newValue: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIsAppendMode(newValue)
            if (!newValue) {
                val currentPrompt = userPreferencesRepository.preferencesFlow.first().customBasePrompt
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

    // --- App Start Action ---
    private val _appStartAction = MutableStateFlow(AppStartAction())
    val appStartAction: StateFlow<AppStartAction> = _appStartAction.asStateFlow()

    fun onEvent(action: AppStartAction) {
        _appStartAction.value = action
    }

    fun onStartActionHandled() {
        _appStartAction.value = AppStartAction()
    }

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
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }
}