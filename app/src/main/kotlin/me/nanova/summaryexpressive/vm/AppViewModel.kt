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
    private val aiProviderConfigRepository: AIProviderConfigRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Nav?>(null)
    val startDestination: StateFlow<Nav?> = _startDestination

    init {
        viewModelScope.launch {
            val isOnboardingCompleted = userPreferencesRepository.preferencesFlow.map { it.isOnboarded }.first()
            if (isOnboardingCompleted) {
                _startDestination.value = Nav.Home
            } else {
                _startDestination.value = Nav.Onboarding
            }
        }
    }

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferencesFlow,
        aiProviderConfigRepository.providerConfigsFlow
    ) { prefs, providerConfigs ->
        val providerConfig = prefs.aiProvider?.let { providerConfigs[it] }
        SettingsUiState(
            useOriginalLanguage = prefs.useOriginalLanguage,
            dynamicColor = prefs.dynamicColor,
            theme = prefs.theme,
            apiKey = providerConfig?.apiKey?.takeIf { it.isNotBlank() },
            baseUrl = providerConfig?.baseUrl?.takeIf { it.isNotBlank() },
            aiProvider = prefs.aiProvider?.let { runCatching { AIProvider.valueOf(it) }.getOrNull() },
            providerConfigs = providerConfigs,
            model = providerConfig?.model?.takeIf { it.isNotBlank() },
            showLength = prefs.showLength,
            summaryLength = SummaryLength.valueOf(prefs.summaryLength),
            autoExtractUrl = prefs.autoExtractUrl,
            sessData = prefs.sessData,
            sessDataExpires = prefs.sessDataExpires,
            isAppendMode = prefs.isAppendMode,
            customBasePrompt = prefs.customBasePrompt,
            additionalSystemPrompt = prefs.additionalSystemPrompt
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
            val provider = userPreferencesRepository.preferencesFlow.first().aiProvider ?: return@launch
            aiProviderConfigRepository.updateApiKey(provider, newValue)
        }
    }

    // API base url
    fun setBaseUrlValue(newValue: String) {
        val baseUrlWithProtocol = if (newValue.isBlank() || newValue.startsWith("http")) newValue else "https://$newValue"
        viewModelScope.launch {
            val provider = userPreferencesRepository.preferencesFlow.first().aiProvider ?: return@launch
            aiProviderConfigRepository.updateBaseUrl(provider, baseUrlWithProtocol)
        }
    }

    // AI provider
    fun setAIProviderValue(newValue: String) =
        savePreference(userPreferencesRepository::setAIProvider, newValue)

    fun setProviderConfig(provider: String, baseUrl: String, apiKey: String) {
        val baseUrlWithProtocol = if (baseUrl.isBlank() || baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
        viewModelScope.launch {
            aiProviderConfigRepository.saveConfig(
                provider,
                ProviderConfig(apiKey = apiKey, baseUrl = baseUrlWithProtocol)
            )
            userPreferencesRepository.setAIProvider(provider)
        }
    }

    // Model
    fun setModel(newValue: String) {
        viewModelScope.launch {
            val provider = userPreferencesRepository.preferencesFlow.first().aiProvider ?: return@launch
            aiProviderConfigRepository.updateModel(provider, newValue)
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
        savePreference(userPreferencesRepository::setIsAppendMode, newValue)
        if (!newValue && settingsUiState.value.customBasePrompt.isEmpty()) {
            setCustomBasePrompt(defaultSystemPromptPlaceholder)
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
}