package me.nanova.summaryexpressive.ui.page.onboarding.section

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.component.LogoIcon
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

const val ONBOARDING_PAGE_COUNT = 5

@Composable
fun OnboardingStepPage(
    page: Int,
    modifier: Modifier = Modifier,
) {
    when (page) {
        0 -> OnboardingStepContent(
            modifier = modifier,
            titleRes = R.string.welcome,
            descriptionRes = R.string.welcomeDescription,
            image = {
                LogoIcon(
                    size = 200.dp,
                    isRotating = true,
                )
            },
        )

        1 -> OnboardingStepContent(
            modifier = modifier,
            titleRes = R.string.onboardingLlmTitle,
            descriptionRes = R.string.onboardingLlmDescription,
            image = { LlmQuickSwitchDemo() },
        )

        2 -> OnboardingStepContent(
            modifier = modifier,
            titleRes = R.string.onboardingSummarizeTitle,
            descriptionRes = R.string.instructionSummary,
            image = { SummarizeContentDemo() },
        )

        3 -> OnboardingStepContent(
            modifier = modifier,
            titleRes = R.string.onboardingShareTitle,
            descriptionRes = R.string.instructionsViaShare,
            image = { InstantSummaryOverlayDemo() },
        )

        4 -> OnboardingStepContent(
            modifier = modifier,
            titleRes = R.string.onboardingHistoryTitle,
            descriptionRes = R.string.instructionsHistory,
            image = { HistorySearchDemo() },
        )
    }
}

@Composable
fun OnboardingStepContent(
    modifier: Modifier = Modifier,
    image: @Composable (() -> Unit)? = null,
    @StringRes titleRes: Int? = null,
    @StringRes descriptionRes: Int? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (image != null) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                image()
            }
        }

        titleRes?.let {
            Text(
                text = stringResource(id = it),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        descriptionRes?.let {
            Text(
                text = stringResource(id = it),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (titleRes != null) 8.dp else 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingStepContentPreview() {
    SummaryExpressiveTheme {
        OnboardingStepPage(page = 1)
    }
}

