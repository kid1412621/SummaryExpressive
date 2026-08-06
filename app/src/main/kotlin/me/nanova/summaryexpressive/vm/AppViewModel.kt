package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.model.CustomPrompt
import me.nanova.summaryexpressive.ui.Nav
import javax.inject.Inject


@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Nav?>(null)
    val startDestination: StateFlow<Nav?> = _startDestination

    init {
        viewModelScope.launch {
            val isOnboardingCompleted = userPreferencesRepository.preferencesFlow.map { it.isOnboarded}.first()

            if (isOnboardingCompleted) {
                _startDestination.value = Nav.Home
            } else {
                _startDestination.value = Nav.Onboarding
            }
        }
    }

    val settingsUiState: StateFlow<SettingsUiState> =
        userPreferencesRepository.preferencesFlow.map { prefs ->
            SettingsUiState(
                useOriginalLanguage = prefs.useOriginalLanguage,
                dynamicColor = prefs.dynamicColor,
                theme = prefs.theme,
                apiKey = prefs.apiKey.takeIf { it.isNotBlank() },
                baseUrl = prefs.baseUrl.takeIf { it.isNotBlank() },
                aiProvider = AIProvider.valueOf(prefs.aiProvider),
                model = prefs.model.takeIf { it.isNotBlank() },
                showLength = prefs.showLength,
                summaryLength = SummaryLength.valueOf(prefs.summaryLength),
                autoExtractUrl = prefs.autoExtractUrl,
                sessData = prefs.sessData,
                sessDataExpires = prefs.sessDataExpires,
                savedPrompts = prefs.savedPrompts,
                selectedPromptId = prefs.selectedPromptId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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
    fun setApiKeyValue(newValue: String) =
        savePreference(userPreferencesRepository::setApiKey, newValue)

    // API base url
    fun setBaseUrlValue(newValue: String) {
        val baseUrlWithProtocol = if (newValue.isBlank() || newValue.startsWith("http")) newValue
        else "https://$newValue"
        savePreference(userPreferencesRepository::setBaseUrl, baseUrlWithProtocol)
    }

    // AI provider
    fun setAIProviderValue(newValue: String) =
        savePreference(userPreferencesRepository::setAIProvider, newValue)

    // Model
    fun setModel(newValue: String) =
        savePreference(userPreferencesRepository::setModel, newValue)

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

    // Custom Prompts
    fun savePrompt(prompt: CustomPrompt) {
        viewModelScope.launch {
            userPreferencesRepository.savePrompt(prompt)
        }
    }

    fun deletePrompt(promptId: String) {
        viewModelScope.launch {
            userPreferencesRepository.deletePrompt(promptId)
        }
    }

    fun selectPrompt(promptId: String?) {
        viewModelScope.launch {
            userPreferencesRepository.selectPrompt(promptId)
        }
    }
}