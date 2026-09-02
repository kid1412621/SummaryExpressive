package me.nanova.summaryexpressive.ui.page.history.section

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType

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
    val dismissState = rememberSwipeToDismissBoxState()

    val aiProvider = remember(summary.provider) {
        summary.provider?.let { providerName ->
            AIProvider.entries.find { it.name.equals(providerName, ignoreCase = true) }
        }
    }

    val domain = remember(summary.sourceLink) {
        summary.sourceLink?.let { link ->
            runCatching {
                val uri = link.toUri()
                uri.host?.removePrefix("www.")
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
    }

    val faviconUrl = remember(domain, summary.type) {
        if (summary.type == SummaryType.ARTICLE && !domain.isNullOrBlank()) {
            "https://www.google.com/s2/favicons?domain=$domain&sz=128"
        } else {
            null
        }
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
                HistoryItemBadge(
                    summary = summary,
                    domain = domain,
                    faviconUrl = faviconUrl
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
private fun HistoryItemBadge(
    summary: HistorySummary,
    domain: String?,
    faviconUrl: String?,
    modifier: Modifier = Modifier,
) {
    val (badgeContainer, badgeContent) = HistoryBadges.badgeColorsFor(
        summary.type,
        summary.subtype
    )
    HistoryIconBadge(
        containerColor = badgeContainer,
        contentColor = badgeContent,
        modifier = modifier
    ) {
        when {
            summary.isYoutubeLink -> {
                Icon(
                    painter = painterResource(id = R.drawable.youtube),
                    contentDescription = "YouTube",
                    modifier = Modifier.size(20.dp)
                )
            }

            summary.isBiliBiliLink -> {
                Icon(
                    painter = painterResource(id = R.drawable.bilibili),
                    contentDescription = "BiliBili",
                    modifier = Modifier.size(20.dp)
                )
            }

            summary.type == SummaryType.ARTICLE && faviconUrl != null -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = domain,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Icon(
                            imageVector = summary.type.icon,
                            contentDescription = summary.type.name,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = summary.type.icon,
                            contentDescription = summary.type.name,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            else -> {
                Icon(
                    imageVector = summary.type.icon,
                    contentDescription = summary.type.name,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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

@Composable
private fun HistoryItemMetadataRow(
    summary: HistorySummary,
    domain: String?,
    aiProvider: AIProvider?,
    modifier: Modifier = Modifier,
) {
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

        if (aiProvider != null || !summary.model.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.widthIn(max = 140.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    if (aiProvider != null) {
                        Icon(
                            painter = painterResource(id = aiProvider.icon),
                            contentDescription = aiProvider.name,
                            modifier = Modifier.size(13.dp),
                            tint = if (aiProvider.isMonochromeIcon) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                    if (!summary.model.isNullOrBlank()) {
                        Text(
                            text = summary.model,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                text = summary.length.name.lowercase()
                    .replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
