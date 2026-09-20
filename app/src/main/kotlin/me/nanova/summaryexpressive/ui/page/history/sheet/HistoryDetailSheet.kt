package me.nanova.summaryexpressive.ui.page.history.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.ui.component.LengthSelector
import me.nanova.summaryexpressive.ui.component.SummaryCard

/**
 * Bottom sheet to view complete summary details, playback TTS, and share
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailSheet(
    summary: HistorySummary?,
    onDismissRequest: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
) {
    if (summary == null) return

    val availableLengths = remember(summary) {
        summary.allLengthResults.keys.filter { it != SummaryLength.NONE }.sortedBy { it.ordinal }
    }
    var selectedLength by remember(summary) {
        mutableStateOf(
            if (summary.allLengthResults.containsKey(summary.length)) {
                summary.length
            } else {
                availableLengths.firstOrNull() ?: summary.length
            }
        )
    }
    val activeSummary = remember(summary, selectedLength) {
        summary.copy(length = selectedLength)
    }

    var isPlaying by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedLength) {
        scrollState.scrollTo(0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                if (availableLengths.size > 1) {
                    val selectedIndex = availableLengths.indexOf(selectedLength).coerceAtLeast(0)
                    LengthSelector(
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { index ->
                            selectedLength = availableLengths[index]
                            isPlaying = false
                        },
                        options = availableLengths.map { length ->
                            length.name.lowercase().replaceFirstChar { it.titlecase() }
                        },
                        enabled = true,
                        useContainerBackground = false,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                key(selectedLength) {
                    SummaryCard(
                        modifier = Modifier.fillMaxWidth(),
                        summary = SummaryOutput(
                            title = summary.title,
                            summary = activeSummary.summary,
                            author = summary.author,
                            sourceLink = summary.sourceLink,
                            isYoutubeLink = summary.isYoutubeLink,
                            isBiliBiliLink = summary.isBiliBiliLink,
                            length = activeSummary.length,
                            provider = activeSummary.provider ?: summary.provider,
                            model = activeSummary.model ?: summary.model,
                            overview = activeSummary.overview,
                            keyPoints = activeSummary.keyPoints,
                            tags = activeSummary.tags.ifEmpty { summary.tags },
                        ),
                        cardColors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        isExpandedByDefault = true,
                        isPlaying = isPlaying,
                        onPlayRequest = { isPlaying = !isPlaying },
                        onShowSnackbar = onShowSnackbar
                    )
                }
            }
        }
    }
}
