package me.nanova.summaryexpressive.ui

import androidx.compose.animation.core.CubicBezierEasing
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
import me.nanova.summaryexpressive.ui.page.HistoryScreen
import me.nanova.summaryexpressive.ui.page.HomeScreen
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.page.settings.AdvancedSummarySetupScreen
import me.nanova.summaryexpressive.ui.page.settings.SettingsScreen
import me.nanova.summaryexpressive.vm.AppViewModel

private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

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
                animationSpec = tween(400, easing = EmphasizedDecelerateEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300, easing = EmphasizedAccelerateEasing)
            )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(400, easing = EmphasizedDecelerateEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = EmphasizedAccelerateEasing)
            )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(400, easing = EmphasizedEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(350, easing = EmphasizedEasing)
            )
        }
    )
}