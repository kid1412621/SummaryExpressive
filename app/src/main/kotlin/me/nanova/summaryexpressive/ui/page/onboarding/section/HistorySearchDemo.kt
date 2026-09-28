package me.nanova.summaryexpressive.ui.page.onboarding.section

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import me.nanova.summaryexpressive.ui.component.ContentBadge
import me.nanova.summaryexpressive.ui.page.history.section.GroupPosition
import me.nanova.summaryexpressive.ui.page.history.section.groupShape
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

private data class HistoryDemoItem(
    val id: Int,
    val title: String,
    val meta: String,
    val type: SummaryType,
    val subtype: VideoSubtype?,
)

private val ALL_HISTORY_ITEMS = listOf(
    HistoryDemoItem(
        id = 1,
        title = "Attention Is All You Need",
        meta = "10:42 AM • claude-5-sonnet",
        type = SummaryType.VIDEO,
        subtype = VideoSubtype.YOUTUBE
    ),
    HistoryDemoItem(
        id = 2,
        title = "Kotlin 2.0 Compiler Architecture",
        meta = "09:15 AM • gpt-6-astra",
        type = SummaryType.VIDEO,
        subtype = VideoSubtype.BILIBILI
    ),
    HistoryDemoItem(
        id = 3,
        title = "DeepSeek-V3_Technical_Report.pdf",
        meta = "Yesterday • deepseek-chat",
        type = SummaryType.DOCUMENT,
        subtype = null
    ),
)

/**
 * Step 4 Onboarding Demo:
 * Highlights Offline History, Instant Search Bar, Category Filter Chips,
 * and Connected Shapes list items matching Android 16 expressive guidelines.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistorySearchDemo(modifier: Modifier = Modifier) {
    // Dynamic Filter & Search auto-cycle loop:
    // 0: "All" active -> 3 items visible with connected shapes (2.4s)
    // 1: "Videos" filter chip selected -> filtered to YouTube & BiliBili (2.4s)
    // 2: Search bar types "Attention" -> filters to single YouTube item (2.8s)
    var filterCycle by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            filterCycle = 0
            delay(2400) // All items
            filterCycle = 1
            delay(2400) // Videos filter
            filterCycle = 2
            delay(2800) // Search query "Attention"
        }
    }

    val selectedFilter = when (filterCycle) {
        1 -> SummaryType.VIDEO
        else -> null
    }

    val searchQuery = when (filterCycle) {
        2 -> "Attention"
        else -> ""
    }

    val displayedItems = remember(filterCycle) {
        when (filterCycle) {
            1 -> ALL_HISTORY_ITEMS.filter { it.type == SummaryType.VIDEO }
            2 -> ALL_HISTORY_ITEMS.filter { it.title.contains("Attention", ignoreCase = true) }
            else -> ALL_HISTORY_ITEMS
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Real History Search Bar (mirroring HistorySearchBar.kt)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = searchQuery.ifEmpty { stringResource(R.string.search) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Filter Chips Row (mirroring HistoryFilterChips.kt)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chipTypes = listOf(
                        null to "All",
                        SummaryType.VIDEO to "Videos",
                        SummaryType.ARTICLE to "Articles",
                        SummaryType.DOCUMENT to "Docs"
                    )

                    items(chipTypes) { (type, label) ->
                        val isSelected = selectedFilter == type
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary
                            ) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Connected Shape History List (using groupShape from HistoryItemRow.kt)
                AnimatedContent(
                    targetState = displayedItems,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "history_list_anim"
                ) { items ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            val position = when {
                                items.size == 1 -> GroupPosition.SINGLE
                                index == 0 -> GroupPosition.TOP
                                index == items.size - 1 -> GroupPosition.BOTTOM
                                else -> GroupPosition.MIDDLE
                            }

                            Surface(
                                shape = groupShape(
                                    position,
                                    outerRadius = 18.dp,
                                    innerRadius = 4.dp
                                ),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 1.dp,
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 13.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContentBadge(
                                        summary = HistorySummary(
                                            title = "",
                                            length = SummaryLength.SHORT,
                                            type = item.type,
                                            subtype = item.subtype
                                        ),
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.meta,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HistorySearchDemoPreview() {
    SummaryExpressiveTheme {
        HistorySearchDemo()
    }
}
