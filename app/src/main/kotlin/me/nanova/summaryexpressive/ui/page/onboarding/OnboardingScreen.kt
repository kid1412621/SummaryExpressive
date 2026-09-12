package me.nanova.summaryexpressive.ui.page.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.page.onboarding.section.ONBOARDING_PAGE_COUNT
import me.nanova.summaryexpressive.ui.page.onboarding.section.OnboardingBottomBar
import me.nanova.summaryexpressive.ui.page.onboarding.section.OnboardingStepPage
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onDoneAndNavigate: (destinationRoute: Nav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }

    val isFirst = pagerState.currentPage == 0
    val isLast = pagerState.currentPage == pagerState.pageCount - 1

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OnboardingStepPage(
                        page = page,
                        imageLoader = imageLoader,
                        modifier = Modifier.widthIn(max = 560.dp)
                    )
                }
            }

            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = ONBOARDING_PAGE_COUNT,
                onSkipOrPrevious = {
                    if (isFirst) {
                        onDone()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNextOrFinish = {
                    if (isLast) {
                        onDone()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                onSetupAI = {
                    onDoneAndNavigate(Nav.Settings(highlight = "ai"))
                },
                onIndicatorClick = { targetPage ->
                    scope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    SummaryExpressiveTheme {
        OnboardingScreen(onDone = {}, onDoneAndNavigate = {})
    }
}
