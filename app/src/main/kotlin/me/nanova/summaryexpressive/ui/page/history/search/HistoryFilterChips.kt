package me.nanova.summaryexpressive.ui.page.history.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.model.SummaryType

/**
 * Filter chips for history summary types
 */
@Composable
fun HistoryFilterChips(
    selectedFilter: SummaryType?,
    onFilterChanged: (SummaryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SummaryType.entries) { filter ->
            val selected = selectedFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterChanged(filter) },
                label = {
                    Text(
                        text = filter.name.lowercase()
                            .replaceFirstChar { it.titlecase() }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (selected) Icons.Default.Check else filter.icon,
                        contentDescription = filter.name
                    )
                }
            )
        }
    }
}
