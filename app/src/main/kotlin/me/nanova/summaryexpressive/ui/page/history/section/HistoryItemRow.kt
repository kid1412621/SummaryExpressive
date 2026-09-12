package me.nanova.summaryexpressive.ui.page.history.section

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.ui.component.ContentBadge
import me.nanova.summaryexpressive.ui.component.extractDomain

/**
 * Item position in an Android 16 expressive grouped list
 */
enum class GroupPosition {
    SINGLE,
    TOP,
    MIDDLE,
    BOTTOM
}

/**
 * Returns an expressive rounded corner shape matching Android 16 System style
 */
fun groupShape(
    position: GroupPosition,
    outerRadius: Dp = 24.dp,
    innerRadius: Dp = 4.dp,
): RoundedCornerShape {
    return when (position) {
        GroupPosition.SINGLE -> RoundedCornerShape(outerRadius)
        GroupPosition.TOP -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        GroupPosition.MIDDLE -> RoundedCornerShape(innerRadius)
        GroupPosition.BOTTOM -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
    }
}

/**
 * Android 16 History Item Row with connected shape surface, tonal icon badge, and swipe-to-dismiss
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryItemRow(
    summary: HistorySummary,
    position: GroupPosition,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = groupShape(position)
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val dismissState = remember(summary.id, positionalThreshold) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = positionalThreshold
        )
    }

    LaunchedEffect(summary.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    val aiProvider = remember(summary.provider) {
        summary.provider?.let { providerName ->
            AIProvider.entries.find { it.name.equals(providerName, ignoreCase = true) }
        }
    }

    val domain = remember(summary.sourceLink) {
        extractDomain(summary.sourceLink)
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(shape),
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false,
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
            }
        },
        backgroundContent = {
            DismissBackground(dismissState = dismissState, shape = shape)
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(onClick = onClick),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContentBadge(
                    summary = summary,
                    domain = domain,
                )

                Spacer(modifier = Modifier.width(14.dp))

                HistoryItemContent(
                    summary = summary,
                    domain = domain,
                    aiProvider = aiProvider,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(
    dismissState: SwipeToDismissBoxState,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            Color.Transparent
        },
        label = "delete_background_color"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = color, shape = shape)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun HistoryItemContent(
    summary: HistorySummary,
    domain: String?,
    aiProvider: AIProvider?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary.title.ifBlank { summary.summary.take(40) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                summary.createdOn,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()

            Text(
                text = relativeTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = summary.summary.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        HistoryItemMetadataRow(
            summary = summary,
            domain = domain,
            aiProvider = aiProvider
        )
    }
}

private data class ModelBadgeItem(
    val provider: AIProvider?,
    val model: String?,
)

@Composable
private fun OverlappingRow(
    modifier: Modifier = Modifier,
    overlapOffset: Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(0, 0) {}
        }
        val placeables = measurables.map { it.measure(constraints) }
        val overlapPx = overlapOffset.roundToPx()
        val totalWidth = placeables.first().width + (placeables.size - 1) * (placeables.first().width - overlapPx).coerceAtLeast(0)
        val maxHeight = placeables.maxOfOrNull { it.height } ?: 0

        layout(totalWidth, maxHeight) {
            var xPosition = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = xPosition,
                    y = (maxHeight - placeable.height) / 2,
                    zIndex = index.toFloat()
                )
                xPosition += (placeable.width - overlapPx).coerceAtLeast(0)
            }
        }
    }
}

@Composable
private fun HistoryItemMetadataRow(
    summary: HistorySummary,
    domain: String?,
    aiProvider: AIProvider?,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun RowScope.Models() {
        val distinctModels =
            remember(summary.allLengthResults, summary.provider, summary.model, aiProvider) {
                val fromLengths = summary.allLengthResults.values.mapNotNull { res ->
                    val prov = res.provider?.let { name ->
                        AIProvider.entries.find { it.name.equals(name, ignoreCase = true) }
                    } ?: aiProvider
                    val mdl = res.model?.takeIf { it.isNotBlank() } ?: summary.model
                    if (prov != null || mdl != null) {
                        ModelBadgeItem(prov, mdl)
                    } else null
                }

                if (fromLengths.isNotEmpty()) {
                    fromLengths.distinct()
                } else {
                    val prov = aiProvider ?: summary.provider?.let { name ->
                        AIProvider.entries.find { it.name.equals(name, ignoreCase = true) }
                    }
                    val mdl = summary.model?.takeIf { it.isNotBlank() }
                    if (prov != null || mdl != null) {
                        listOf(ModelBadgeItem(prov, mdl))
                    } else {
                        emptyList()
                    }
                }
            }

        if (distinctModels.size == 1) {
            val single = distinctModels.first()
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 130.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    if (single.provider != null) {
                        Icon(
                            painter = painterResource(id = single.provider.icon),
                            contentDescription = single.provider.name,
                            modifier = Modifier.size(13.dp),
                            tint = if (single.provider.isMonochromeIcon) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                    if (!single.model.isNullOrBlank()) {
                        Text(
                            text = single.model,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        } else if (distinctModels.size > 1) {
            OverlappingRow(
                overlapOffset = 6.dp,
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                distinctModels.forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.provider != null) {
                            Icon(
                                painter = painterResource(id = item.provider.icon),
                                contentDescription = item.provider.name,
                                modifier = Modifier.size(12.dp),
                                tint = if (item.provider.isMonochromeIcon) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    Color.Unspecified
                                }
                            )
                        } else if (!item.model.isNullOrBlank()) {
                            Text(
                                text = item.model.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Length() {
        val lengths = remember(summary.allLengthResults, summary.length) {
            val fromAll = summary.allLengthResults.keys.sortedBy { it.ordinal }
            fromAll.ifEmpty { listOf(summary.length) }
        }

        if (lengths.size > 1) {
            OverlappingRow(
                overlapOffset = 6.dp,
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                lengths.forEach { length ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (length) {
                                SummaryLength.SHORT -> "S"
                                SummaryLength.MEDIUM -> "M"
                                SummaryLength.LONG -> "L"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            val single = lengths.first()
            val text = when (single) {
                SummaryLength.SHORT -> "Short"
                SummaryLength.MEDIUM -> "Mid"
                SummaryLength.LONG -> "Long"
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val authorOrDomain = summary.author.ifBlank { domain ?: "" }
        if (authorOrDomain.isNotBlank()) {
            Text(
                text = authorOrDomain,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Models()

        Length()
    }
}
