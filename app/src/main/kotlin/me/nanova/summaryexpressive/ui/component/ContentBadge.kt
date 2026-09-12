package me.nanova.summaryexpressive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import me.nanova.summaryexpressive.model.isBiliBiliLink
import me.nanova.summaryexpressive.model.isYouTubeLink
import java.util.Locale

/**
 * Curated Android 16 / Material 3 Expressive Icon Badge Palettes for all content types
 */
object ContentBadgeColors {
    @Composable
    fun youtubeColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF482024) to Color(0xFFF87171)
    } else {
        Color(0xFFFEF2F2) to Color(0xFFDC2626)
    }

    @Composable
    fun bilibiliColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF452233) to Color(0xFFFB7185)
    } else {
        Color(0xFFFFF1F2) to Color(0xFFE11D48)
    }

    @Composable
    fun genericVideoColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF2D325A) to Color(0xFFA5B4FC)
    } else {
        Color(0xFFEEF2FF) to Color(0xFF4F46E5)
    }

    @Composable
    fun articleColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF1A3828) to Color(0xFF86EFAC)
    } else {
        Color(0xFFF0FDF4) to Color(0xFF16A34A)
    }

    @Composable
    fun documentColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF1E3448) to Color(0xFF7DD3FC)
    } else {
        Color(0xFFF0F9FF) to Color(0xFF0284C7)
    }

    @Composable
    fun textColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF382E48) to Color(0xFFD8B4FE)
    } else {
        Color(0xFFF5F3FF) to Color(0xFF9333EA)
    }

    @Composable
    fun neutralColors(): Pair<Color, Color> = if (isSystemInDarkTheme()) {
        Color(0xFF2E3138) to Color(0xFF94A3B8)
    } else {
        Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    @Composable
    fun badgeColorsFor(summary: HistorySummary): Pair<Color, Color> {
        return when {
            summary.isYoutubeLink -> youtubeColors()
            summary.isBiliBiliLink -> bilibiliColors()
            summary.type == SummaryType.VIDEO -> when (summary.subtype) {
                VideoSubtype.YOUTUBE -> youtubeColors()
                VideoSubtype.BILIBILI -> bilibiliColors()
                else -> genericVideoColors()
            }

            summary.type == SummaryType.ARTICLE -> articleColors()
            summary.type == SummaryType.DOCUMENT -> documentColors()
            summary.type == SummaryType.TEXT -> textColors()
            else -> genericVideoColors()
        }
    }

    @Composable
    fun badgeColorsFor(
        urlOrText: String,
        documentFilename: String? = null,
    ): Pair<Color, Color> {
        if (documentFilename != null) {
            return documentColors()
        }
        val trimmed = urlOrText.trim()
        if (trimmed.isEmpty()) {
            return neutralColors()
        }
        return when {
            isYouTubeLink(trimmed) -> youtubeColors()
            isBiliBiliLink(trimmed) -> bilibiliColors()
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith(
                "https://",
                ignoreCase = true
            ) -> articleColors()

            else -> textColors()
        }
    }
}

/**
 * Circular Icon Badge Container with Tonal Tinting (Material 3 Expressive)
 */
@Composable
fun IconBadge(
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
    }
}

/**
 * Shared ContentBadge for History Summary items.
 */
@Composable
fun ContentBadge(
    summary: HistorySummary,
    modifier: Modifier = Modifier,
    domain: String? = null,
    faviconUrl: String? = null,
    rootFaviconUrl: String? = null,
    size: Dp = 40.dp,
) {
    val (badgeContainer, badgeContent) = ContentBadgeColors.badgeColorsFor(summary)
    val resolvedDomain = remember(summary.sourceLink, domain) {
        domain ?: extractDomain(summary.sourceLink)
    }
    val resolvedFaviconUrl = remember(summary.type, resolvedDomain, faviconUrl) {
        faviconUrl ?: if (summary.type == SummaryType.ARTICLE && !resolvedDomain.isNullOrBlank()) {
            "https://www.google.com/s2/favicons?domain_url=https://$resolvedDomain&sz=128"
        } else {
            null
        }
    }
    val resolvedRootFaviconUrl = remember(summary.type, resolvedDomain, rootFaviconUrl) {
        rootFaviconUrl
            ?: if (summary.type == SummaryType.ARTICLE && !resolvedDomain.isNullOrBlank()) {
                val root = extractRootDomain(resolvedDomain)
                if (root != resolvedDomain) {
                    "https://www.google.com/s2/favicons?domain_url=https://$root&sz=128"
                } else {
                    null
                }
            } else {
                null
            }
    }

    IconBadge(
        containerColor = badgeContainer,
        contentColor = badgeContent,
        modifier = modifier,
        size = size,
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

            summary.type == SummaryType.ARTICLE && resolvedFaviconUrl != null -> {
                FaviconImage(
                    domain = resolvedDomain,
                    faviconUrl = resolvedFaviconUrl,
                    rootFaviconUrl = resolvedRootFaviconUrl,
                    fallbackIcon = {
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

/**
 * Shared ContentBadge for Home Screen input field or dynamic URL/text inputs.
 */
@Composable
fun ContentBadge(
    urlOrText: String,
    modifier: Modifier = Modifier,
    documentFilename: String? = null,
    size: Dp = 40.dp,
) {
    val (badgeContainer, badgeContent) = ContentBadgeColors.badgeColorsFor(
        urlOrText = urlOrText,
        documentFilename = documentFilename,
    )
    val trimmed = urlOrText.trim()
    val isDocument = documentFilename != null
    val isYoutube = isYouTubeLink(trimmed)
    val isBiliBili = isBiliBiliLink(trimmed)
    val isUrl = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith(
        "https://",
        ignoreCase = true
    )

    val domain = remember(trimmed, isUrl) {
        if (isUrl) extractDomain(trimmed) else null
    }
    val faviconUrl = remember(domain) {
        if (!domain.isNullOrBlank()) "https://www.google.com/s2/favicons?domain_url=https://$domain&sz=128" else null
    }
    val rootFaviconUrl = remember(domain) {
        if (!domain.isNullOrBlank()) {
            val root = extractRootDomain(domain)
            if (root != domain) "https://www.google.com/s2/favicons?domain_url=https://$root&sz=128" else null
        } else {
            null
        }
    }

    IconBadge(
        containerColor = badgeContainer,
        contentColor = badgeContent,
        modifier = modifier,
        size = size,
    ) {
        when {
            isDocument -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = stringResource(id = R.string.document),
                    modifier = Modifier.size(20.dp)
                )
            }

            isYoutube -> {
                Icon(
                    painter = painterResource(id = R.drawable.youtube),
                    contentDescription = "YouTube",
                    modifier = Modifier.size(20.dp)
                )
            }

            isBiliBili -> {
                Icon(
                    painter = painterResource(id = R.drawable.bilibili),
                    contentDescription = "BiliBili",
                    modifier = Modifier.size(20.dp)
                )
            }

            isUrl && faviconUrl != null -> {
                FaviconImage(
                    domain = domain,
                    faviconUrl = faviconUrl,
                    rootFaviconUrl = rootFaviconUrl,
                    fallbackIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = "Web Article",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            isUrl -> {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = "Web Article",
                    modifier = Modifier.size(20.dp)
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Text",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Async favicon image loader with root domain fallback and fallback icon.
 */
@Composable
private fun FaviconImage(
    domain: String?,
    faviconUrl: String,
    modifier: Modifier = Modifier,
    rootFaviconUrl: String? = null,
    fallbackIcon: @Composable () -> Unit,
) {
    var currentFaviconUrl by remember(faviconUrl) { mutableStateOf(faviconUrl) }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(currentFaviconUrl)
            .crossfade(true)
            .build(),
        contentDescription = domain,
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Fit,
        loading = { fallbackIcon() },
        error = {
            LaunchedEffect(currentFaviconUrl) {
                if (currentFaviconUrl != rootFaviconUrl && rootFaviconUrl != null) {
                    currentFaviconUrl = rootFaviconUrl
                }
            }
            fallbackIcon()
        }
    )
}

internal fun extractDomain(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return runCatching {
        val uri = url.toUri()
        uri.host?.removePrefix("www.")
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

internal fun extractRootDomain(domain: String): String {
    val clean = domain.removePrefix("www.").trim().lowercase(Locale.ROOT)
    val parts = clean.split('.')
    if (parts.size <= 2) return clean

    val secondToLast = parts[parts.size - 2]
    val commonSecondLevelDomains = setOf(
        "co", "com", "org", "net", "edu", "gov", "ac", "ne", "or"
    )
    return if (secondToLast in commonSecondLevelDomains && parts.size >= 3) {
        parts.takeLast(3).joinToString(".")
    } else {
        parts.takeLast(2).joinToString(".")
    }
}
