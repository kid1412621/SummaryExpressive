package me.nanova.summaryexpressive.vm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.nanova.summaryexpressive.data.AIProviderConfigDao
import me.nanova.summaryexpressive.data.AIProviderConfigEntity
import me.nanova.summaryexpressive.data.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.defaultSystemPromptPlaceholder
import me.nanova.summaryexpressive.model.ProviderConfig
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.UserPreferences
import me.nanova.summaryexpressive.ui.Nav
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeAIProviderConfigRepository : AIProviderConfigRepository(null) {
        val configs = MutableStateFlow<Map<String, ProviderConfig>>(emptyMap())

        override val providerConfigsFlow: Flow<Map<String, ProviderConfig>> = configs

        override fun getConfigFlow(provider: String): Flow<ProviderConfig?> =
            configs.map { it[provider] }

        override suspend fun getConfig(provider: String): ProviderConfig? =
            configs.value[provider]

        override suspend fun saveConfig(provider: String, config: ProviderConfig) {
            configs.value = configs.value + (provider to config)
        }

        override suspend fun updateApiKey(provider: String, apiKey: String) {
            val current = getConfig(provider) ?: ProviderConfig()
            saveConfig(provider, current.copy(apiKey = apiKey))
        }

        override suspend fun updateBaseUrl(provider: String, baseUrl: String) {
            val current = getConfig(provider) ?: ProviderConfig()
            saveConfig(provider, current.copy(baseUrl = baseUrl))
        }

        override suspend fun updateModel(provider: String, model: String) {
            val current = getConfig(provider) ?: ProviderConfig()
            saveConfig(provider, current.copy(activeModel = model))
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository(null) {
        val prefs = MutableStateFlow(UserPreferences())

        override val preferencesFlow: Flow<UserPreferences> = prefs

        override suspend fun setUseOriginalLanguage(value: Boolean) {
            prefs.value = prefs.value.copy(useOriginalLanguage = value)
        }

        override suspend fun setDynamicColor(value: Boolean) {
            prefs.value = prefs.value.copy(dynamicColor = value)
        }

        override suspend fun setTheme(value: Int) {
            prefs.value = prefs.value.copy(theme = value)
        }

        override suspend fun setActiveProvider(value: String?) {
            prefs.value = prefs.value.copy(activeProvider = value)
        }

        override suspend fun setProviderOrder(value: List<String>) {
            prefs.value = prefs.value.copy(providerOrder = value)
        }

        override suspend fun setIsOnboarded(value: Boolean) {
            prefs.value = prefs.value.copy(isOnboarded = value)
        }

        override suspend fun setShowLength(value: Boolean) {
            prefs.value = prefs.value.copy(showLength = value)
        }

        override suspend fun setSummaryLength(value: String) {
            prefs.value = prefs.value.copy(summaryLength = value)
        }

        override suspend fun setAutoExtractUrl(value: Boolean) {
            prefs.value = prefs.value.copy(autoExtractUrl = value)
        }

        override suspend fun setSessData(data: String, expires: Long) {
            prefs.value = prefs.value.copy(sessData = data, sessDataExpires = expires)
        }

        override suspend fun clearSessData() {
            prefs.value = prefs.value.copy(sessData = "", sessDataExpires = 0L)
        }

        override suspend fun setIsAppendMode(value: Boolean) {
            prefs.value = prefs.value.copy(isAppendMode = value)
        }

        override suspend fun setCustomBasePrompt(value: String) {
            prefs.value = prefs.value.copy(customBasePrompt = value)
        }

        override suspend fun setAdditionalSystemPrompt(value: String) {
            prefs.value = prefs.value.copy(additionalSystemPrompt = value)
        }
    }

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeConfigRepo: FakeAIProviderConfigRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeConfigRepo = FakeAIProviderConfigRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test startDestination reflects onboarding state`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)

        assertEquals(Nav.Onboarding, viewModel.startDestination.value)

        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = true)
        assertEquals(Nav.Home, viewModel.startDestination.value)
    }

    @Test
    fun `test settingsUiState combines preferences and provider configs`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(
            activeProvider = "OPENAI",
            useOriginalLanguage = false,
            dynamicColor = true,
            theme = 1,
            summaryLength = "LONG"
        )
        fakeConfigRepo.saveConfig(
            "OPENAI",
            ProviderConfig(
                apiKey = "sk-test-123",
                baseUrl = "https://api.openai.com/v1",
                activeModel = "gpt-4o",
                models = listOf("gpt-4o", "gpt-4o-mini")
            )
        )

        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.settingsUiState.collect {}
        }
        val state = viewModel.settingsUiState.value

        assertEquals(AIProvider.OPENAI, state.activeProvider)
        assertEquals("sk-test-123", state.apiKey)
        assertEquals("https://api.openai.com/v1", state.baseUrl)
        assertEquals("gpt-4o", state.activeModel)
        assertFalse(state.useOriginalLanguage)
        assertTrue(state.dynamicColor)
        assertEquals(1, state.theme)
        assertEquals(SummaryLength.LONG, state.summaryLength)
    }

    @Test
    fun `test setApiKeyValue and setBaseUrlValue with normalization`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(activeProvider = "OPENAI")
        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)

        viewModel.setApiKeyValue("  sk-test-key  ")
        assertEquals("sk-test-key", fakeConfigRepo.getConfig("OPENAI")?.apiKey)

        // URL without protocol
        viewModel.setBaseUrlValue("api.openai.com/v1")
        assertEquals("https://api.openai.com/v1", fakeConfigRepo.getConfig("OPENAI")?.baseUrl)

        // URL with http protocol
        viewModel.setBaseUrlValue("http://localhost:11434")
        assertEquals("http://localhost:11434", fakeConfigRepo.getConfig("OPENAI")?.baseUrl)

        // Empty URL
        viewModel.setBaseUrlValue("   ")
        assertEquals("", fakeConfigRepo.getConfig("OPENAI")?.baseUrl)
    }

    @Test
    fun `test setProviderConfig preserves existing models and updates order`() = runTest(testDispatcher) {
        fakeConfigRepo.saveConfig(
            "DEEPSEEK",
            ProviderConfig(
                apiKey = "old-key",
                baseUrl = "https://api.deepseek.com",
                activeModel = "deepseek-chat",
                models = listOf("deepseek-chat", "deepseek-reasoner")
            )
        )

        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)
        val newOrder = listOf("DEEPSEEK", "OPENAI", "GEMINI")

        viewModel.setProviderConfig(
            provider = "DEEPSEEK",
            baseUrl = "api.deepseek.com/v1",
            apiKey = "new-key",
            providerOrder = newOrder
        )

        val updatedConfig = fakeConfigRepo.getConfig("DEEPSEEK")
        assertNotNull(updatedConfig)
        assertEquals("new-key", updatedConfig?.apiKey)
        assertEquals("https://api.deepseek.com/v1", updatedConfig?.baseUrl)
        // Existing models and active model preserved
        assertEquals("deepseek-chat", updatedConfig?.activeModel)
        assertEquals(listOf("deepseek-chat", "deepseek-reasoner"), updatedConfig?.models)

        // Active provider and provider order updated
        assertEquals("DEEPSEEK", fakePrefsRepo.prefs.value.activeProvider)
        assertEquals(newOrder, fakePrefsRepo.prefs.value.providerOrder)
    }

    @Test
    fun `test setModel and setModelForProvider appends new model and updates activeModel`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(activeProvider = "GEMINI")
        fakeConfigRepo.saveConfig(
            "GEMINI",
            ProviderConfig(
                apiKey = "gemini-key",
                activeModel = "gemini-1.5-flash",
                models = listOf("gemini-1.5-flash", "gemini-1.5-pro")
            )
        )

        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)

        viewModel.setModel("gemini-2.0-flash-exp")

        val config = fakeConfigRepo.getConfig("GEMINI")
        assertEquals("gemini-2.0-flash-exp", config?.activeModel)
        assertEquals(listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp"), config?.models)
    }

    @Test
    fun `test setProviderModels updates model list and auto selects valid activeModel`() = runTest(testDispatcher) {
        fakeConfigRepo.saveConfig(
            "OPENAI",
            ProviderConfig(
                apiKey = "key",
                activeModel = "gpt-4",
                models = listOf("gpt-4")
            )
        )

        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)

        // Explicit selectedModel
        viewModel.setProviderModels("OPENAI", listOf("gpt-4o", "o1"), selectedModel = "o1")
        var config = fakeConfigRepo.getConfig("OPENAI")
        assertEquals("o1", config?.activeModel)
        assertEquals(listOf("gpt-4o", "o1"), config?.models)

        // Fallback to first when current activeModel not in list
        viewModel.setProviderModels("OPENAI", listOf("custom-1", "custom-2"))
        config = fakeConfigRepo.getConfig("OPENAI")
        assertEquals("custom-1", config?.activeModel)
    }

    @Test
    fun `test resetProviderModelsToDefault restores default models`() = runTest(testDispatcher) {
        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)
        val defaultModels = AIProvider.CLAUDE.defaultModelIds

        viewModel.resetProviderModelsToDefault("CLAUDE")

        val config = fakeConfigRepo.getConfig("CLAUDE")
        assertNotNull(config)
        assertEquals(defaultModels, config?.models)
        assertEquals(defaultModels.first(), config?.activeModel)
    }

    @Test
    fun `test setIsAppendMode sets default prompt when customBasePrompt is empty`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isAppendMode = true, customBasePrompt = "")
        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)

        viewModel.setIsAppendMode(false)

        assertFalse(fakePrefsRepo.prefs.value.isAppendMode)
        assertEquals(defaultSystemPromptPlaceholder, fakePrefsRepo.prefs.value.customBasePrompt)
    }

    @Test
    fun `test appStartAction event dispatch and reset`() {
        val viewModel = AppViewModel(fakePrefsRepo, fakeConfigRepo)

        viewModel.onEvent(AppStartAction(content = "https://example.com", autoTrigger = true))
        assertEquals("https://example.com", viewModel.appStartAction.value.content)
        assertTrue(viewModel.appStartAction.value.autoTrigger)

        viewModel.onStartActionHandled()
        assertNull(viewModel.appStartAction.value.content)
        assertFalse(viewModel.appStartAction.value.autoTrigger)
    }
}
