package me.nanova.summaryexpressive.ui

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.nanova.summaryexpressive.ui.page.AdvancedSummarySetupScreen
import me.nanova.summaryexpressive.ui.page.HistoryScreen
import me.nanova.summaryexpressive.ui.page.HomeScreen
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.page.SettingsScreen
import me.nanova.summaryexpressive.vm.AppViewModel

@Composable
fun AppNavigation(
    backStack: NavBackStack<NavKey>,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val handleOnboardingDone: (Nav?) -> Unit = remember(backStack, appViewModel) {
        { targetRoute ->
            appViewModel.setIsOnboarded(true)
            backStack.clear()
            backStack.add(Nav.Home)
            if (targetRoute != null && targetRoute != Nav.Home) {
                backStack.add(targetRoute)
            }
        }
    }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Nav.Home> {
                HomeScreen(
                    modifier = Modifier,
                    onNav = { route -> backStack.add(route) },
                    appViewModel = appViewModel
                )
            }

            entry<Nav.Onboarding> {
                OnboardingScreen(
                    onDone = {
                        handleOnboardingDone(null)
                    },
                    onDoneAndNavigate = { destination ->
                        handleOnboardingDone(destination)
                    }
                )
            }

            entry<Nav.Settings> { key ->
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNav = { route -> backStack.add(route) },
                    highlightSection = key.highlight,
                    appViewModel = appViewModel
                )
            }

            entry<Nav.History> {
                HistoryScreen()
            }

            entry<Nav.AdvancedSummarySetup> {
                AdvancedSummarySetupScreen(
                    onBack = { backStack.removeLastOrNull() },
                    appViewModel = appViewModel
                )
            }
        },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = EaseIn)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = EaseOut)
            )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = EaseIn)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = EaseOut)
            )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = EaseIn)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = EaseOut)
            )
        }
    )
}