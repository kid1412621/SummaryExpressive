package me.nanova.summaryexpressive.vm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.model.UserPreferences
import me.nanova.summaryexpressive.ui.Nav
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

    private class FakeUserPreferencesRepository : UserPreferencesRepository(null) {
        val prefs = MutableStateFlow(UserPreferences())

        override val preferencesFlow: Flow<UserPreferences> = prefs

        override suspend fun setIsOnboarded(value: Boolean) {
            prefs.value = prefs.value.copy(isOnboarded = value)
        }
    }

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test startDestination reflects onboarding state`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(fakePrefsRepo)

        assertEquals(Nav.Onboarding, viewModel.startDestination.value)

        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = true)
        assertEquals(Nav.Home, viewModel.startDestination.value)
    }

    @Test
    fun `test setIsOnboarded updates repository`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(fakePrefsRepo)

        viewModel.setIsOnboarded(true)
        assertTrue(fakePrefsRepo.prefs.value.isOnboarded)
    }

    @Test
    fun `test appStartAction event dispatch and reset`() {
        val viewModel = AppViewModel(fakePrefsRepo)

        viewModel.onEvent(AppStartAction(content = "https://example.com", autoTrigger = true))
        assertEquals("https://example.com", viewModel.appStartAction.value.content)
        assertTrue(viewModel.appStartAction.value.autoTrigger)

        viewModel.onStartActionHandled()
        assertNull(viewModel.appStartAction.value.content)
        assertFalse(viewModel.appStartAction.value.autoTrigger)
    }
}
