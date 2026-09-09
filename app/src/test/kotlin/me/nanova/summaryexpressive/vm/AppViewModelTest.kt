package me.nanova.summaryexpressive.vm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.domain.usecase.GetOnboardingStatusUseCase
import me.nanova.summaryexpressive.domain.usecase.SetOnboardingStatusUseCase
import me.nanova.summaryexpressive.model.UserPreferences
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val prefs = MutableStateFlow(UserPreferences())

        override val preferencesFlow: Flow<UserPreferences> = prefs

        override suspend fun setUseOriginalLanguage(value: Boolean) {}
        override suspend fun setDynamicColor(value: Boolean) {}
        override suspend fun setTheme(value: Int) {}
        override suspend fun setActiveProvider(value: String?) {}
        override suspend fun setProviderOrder(value: List<String>) {}
        override suspend fun setIsOnboarded(value: Boolean) {
            prefs.value = prefs.value.copy(isOnboarded = value)
        }
        override suspend fun setShowLength(value: Boolean) {}
        override suspend fun setSummaryLength(value: String) {}
        override suspend fun setAutoExtractUrl(value: Boolean) {}
        override suspend fun setBilibiliSessData(data: String, expires: Long) {}
        override suspend fun clearBilibiliSessData() {}
        override suspend fun setIsAppendMode(value: Boolean) {}
        override suspend fun setCustomBasePrompt(value: String) {}
        override suspend fun setAdditionalSystemPrompt(value: String) {}
    }

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var getOnboardingStatusUseCase: GetOnboardingStatusUseCase
    private lateinit var setOnboardingStatusUseCase: SetOnboardingStatusUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
        getOnboardingStatusUseCase = GetOnboardingStatusUseCase(fakePrefsRepo)
        setOnboardingStatusUseCase = SetOnboardingStatusUseCase(fakePrefsRepo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test isOnboarded reflects onboarding state`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isOnboarded.collect {}
        }

        assertEquals(false, viewModel.isOnboarded.value)

        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = true)
        assertEquals(true, viewModel.isOnboarded.value)
    }

    @Test
    fun `test setIsOnboarded updates repository`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)

        viewModel.setIsOnboarded(true)
        assertTrue(fakePrefsRepo.prefs.value.isOnboarded)
    }

    @Test
    fun `test appStartAction event dispatch and reset`() {
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)

        viewModel.onEvent(AppStartAction(content = "https://example.com", autoTrigger = true))
        assertEquals("https://example.com", viewModel.appStartAction.value.content)
        assertTrue(viewModel.appStartAction.value.autoTrigger)

        viewModel.onStartActionHandled()
        assertNull(viewModel.appStartAction.value.content)
        assertFalse(viewModel.appStartAction.value.autoTrigger)
    }
}
