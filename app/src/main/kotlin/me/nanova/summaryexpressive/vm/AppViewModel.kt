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
import me.nanova.summaryexpressive.ProviderConfig
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.data.AIProviderConfigDao
import me.nanova.summaryexpressive.data.AIProviderConfigEntity
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.ui.Nav
import javax.inject.Inject


@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigDao: AIProviderConfigDao
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
        aiProviderConfigDao.getAllConfigsFlow()
    ) { prefs, configEntities ->
        val providerConfigs = configEntities.associate { 
            it.provider to it.toProviderConfig() 
        }
        val provider =
            prefs.activeProvider?.let { runCatching { AIProvider.valueOf(it) }.getOrNull() }
        val providerConfig = provider?.name?.let { providerConfigs[it] }
        val effectiveModel = provider?.getEffectiveModel(providerConfig)?.takeIf { it.isNotBlank() }
        SettingsUiState(
            useOriginalLanguage = prefs.useOriginalLanguage,
            dynamicColor = prefs.dynamicColor,
            theme = prefs.theme,
            apiKey = providerConfig?.apiKey?.takeIf { it.isNotBlank() },
            baseUrl = providerConfig?.baseUrl?.takeIf { it.isNotBlank() },
            activeProvider = provider,
            providerConfigs = providerConfigs,
            activeModel = effectiveModel,
            showLength = prefs.showLength,
            summaryLength = SummaryLength.valueOf(prefs.summaryLength),
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
            val provider =
                userPreferencesRepository.preferencesFlow.first().activeProvider ?: return@launch
            val currentConfig = aiProviderConfigDao.getConfig(provider)?.toProviderConfig() ?: ProviderConfig()
            aiProviderConfigDao.insertConfig(
                AIProviderConfigEntity.fromProviderConfig(provider, currentConfig.copy(apiKey = newValue))
            )
        }
    }

    // API base url
    fun setBaseUrlValue(newValue: String) {
        val baseUrlWithProtocol = if (newValue.isBlank() || newValue.startsWith("http")) newValue else "https://$newValue"
        viewModelScope.launch {
            val provider =
                userPreferencesRepository.preferencesFlow.first().activeProvider ?: return@launch
            val currentConfig = aiProviderConfigDao.getConfig(provider)?.toProviderConfig() ?: ProviderConfig()
            aiProviderConfigDao.insertConfig(
                AIProviderConfigEntity.fromProviderConfig(provider, currentConfig.copy(baseUrl = baseUrlWithProtocol))
            )
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
        val baseUrlWithProtocol = if (baseUrl.isBlank() || baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
        viewModelScope.launch {
            val currentConfig = aiProviderConfigDao.getConfig(provider)?.toProviderConfig() ?: ProviderConfig()
            aiProviderConfigDao.insertConfig(
                AIProviderConfigEntity.fromProviderConfig(provider, currentConfig.copy(baseUrl = baseUrlWithProtocol, apiKey = apiKey))
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
            val provider =
                userPreferencesRepository.preferencesFlow.first().activeProvider ?: return@launch
            setModelForProvider(provider, newValue)
        }
    }

    fun setModelForProvider(provider: String, model: String) {
        viewModelScope.launch {
            val currentConfig =
                aiProviderConfigDao.getConfig(provider)?.toProviderConfig() ?: ProviderConfig()
            val models =
                if (currentConfig.models.isNotEmpty() && !currentConfig.models.contains(model)) {
                    currentConfig.models + model
                } else {
                    currentConfig.models
                }
            aiProviderConfigDao.insertConfig(
                AIProviderConfigEntity.fromProviderConfig(
                    provider,
                    currentConfig.copy(activeModel = model, models = models)
                )
            )
        }
    }

    fun setProviderModels(provider: String, models: List<String>, selectedModel: String? = null) {
        viewModelScope.launch {
            val currentConfig = aiProviderConfigDao.getConfig(provider)?.toProviderConfig() ?: ProviderConfig()
            val targetModel =
                selectedModel ?: currentConfig.activeModel.takeIf { it in models }
                ?: models.firstOrNull()
                ?: ""
            aiProviderConfigDao.insertConfig(
                AIProviderConfigEntity.fromProviderConfig(
                    provider,
                    currentConfig.copy(models = models, activeModel = targetModel)
                )
            )
        }
    }

    fun resetProviderModelsToDefault(provider: String) {
        viewModelScope.launch {
            val aiProvider =
                runCatching { AIProvider.valueOf(provider) }.getOrNull() ?: return@launch
            val defaultModels = aiProvider.defaultModelIds
            val currentConfig =
                aiProviderConfigDao.getConfig(provider)?.toProviderConfig() ?: ProviderConfig()
            val defaultModel = defaultModels.firstOrNull() ?: ""
            aiProviderConfigDao.insertConfig(
                AIProviderConfigEntity.fromProviderConfig(
                    provider,
                    currentConfig.copy(models = defaultModels, activeModel = defaultModel)
                )
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
        savePreference(userPreferencesRepository::setIsAppendMode, newValue)
        if (!newValue && settingsUiState.value.customBasePrompt.isEmpty()) {
            setCustomBasePrompt(me.nanova.summaryexpressive.llm.defaultSystemPromptPlaceholder)
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