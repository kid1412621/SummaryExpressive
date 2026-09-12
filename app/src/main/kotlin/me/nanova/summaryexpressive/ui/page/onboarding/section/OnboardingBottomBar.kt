package me.nanova.summaryexpressive.ui.page.onboarding.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    onSkipOrPrevious: () -> Unit,
    onNextOrFinish: () -> Unit,
    onSetupAI: () -> Unit,
    onIndicatorClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFirst = currentPage == 0
    val isLast = currentPage == pageCount - 1

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpressivePageIndicator(
                pageCount = pageCount,
                currentPage = currentPage,
                onPageClick = onIndicatorClick,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .semantics {
                        contentDescription = "Page ${currentPage + 1} of $pageCount"
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSkipOrPrevious) {
                    Text(
                        text = if (isFirst) stringResource(R.string.skip)
                        else stringResource(R.string.previous)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = isLast,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(onClick = onSetupAI) {
                            Text(stringResource(R.string.setupAI))
                        }
                    }

                    FilledTonalButton(onClick = onNextOrFinish) {
                        Text(
                            text = if (isLast) stringResource(R.string.finishButton)
                            else stringResource(R.string.next)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressivePageIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                label = "indicator_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "indicator_color"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPageClick(index) }
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingBottomBarFirstPreview() {
    SummaryExpressiveTheme {
        OnboardingBottomBar(
            currentPage = 0,
            pageCount = 4,
            onSkipOrPrevious = {},
            onNextOrFinish = {},
            onSetupAI = {},
            onIndicatorClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingBottomBarLastPreview() {
    SummaryExpressiveTheme {
        OnboardingBottomBar(
            currentPage = 3,
            pageCount = 4,
            onSkipOrPrevious = {},
            onNextOrFinish = {},
            onSetupAI = {},
            onIndicatorClick = {}
        )
    }
}
