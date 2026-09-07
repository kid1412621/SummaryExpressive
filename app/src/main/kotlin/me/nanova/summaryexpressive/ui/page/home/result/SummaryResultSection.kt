package me.nanova.summaryexpressive.ui.page.home.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.SummarizationState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SummaryResultSection(
    summarizationState: SummarizationState,
    isPlaying: Boolean,
    onCopySummary: (String) -> Unit,
    onShowSnackBar: (String) -> Unit,
    onPlaySummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (summarizationState.isLoading) {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        summarizationState.summaryResult?.takeIf { it.summary.isNotEmpty() }?.let { summaryOutput ->
            key(summaryOutput.length) {
                SummaryCard(
                    modifier = Modifier.padding(vertical = 8.dp),
                    isExpandedByDefault = true,
                    summary = summaryOutput,
                    onLongClick = { onCopySummary(summaryOutput.summary) },
                    onShowSnackbar = onShowSnackBar,
                    isPlaying = isPlaying,
                    onPlayRequest = onPlaySummary
                )
            }
        }
    }
}

@Preview
@Composable
private fun SummaryResultSectionPreview() {
    SummaryExpressiveTheme {
        SummaryResultSection(
            summarizationState = SummarizationState(
                summaryResult = SummaryOutput(
                    title = "Sample Summary",
                    summary = "This is a preview summary demonstrating the result container.",
                    author = "Author",
                    sourceLink = "https://example.com",
                    isYoutubeLink = false,
                    isBiliBiliLink = false,
                    length = SummaryLength.MEDIUM,
                    provider = AIProvider.OPENAI.name,
                    model = "gpt-4o"
                )
            ),
            isPlaying = false,
            onCopySummary = {},
            onShowSnackBar = {},
            onPlaySummary = {}
        )
    }
}
