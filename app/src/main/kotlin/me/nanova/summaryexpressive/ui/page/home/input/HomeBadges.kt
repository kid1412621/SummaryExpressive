package me.nanova.summaryexpressive.ui.page.home.input

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 40dp Circular Icon Badge Container with Tonal Tinting (Material 3 Expressive)
 */
@Composable
fun HomeIconBadge(
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
    }
}

/**
 * Curated Android 16 / Material 3 Expressive Icon Badge Palettes for Home Content Types
 */
object HomeBadges {
    @Composable
    fun youtubeColors() = if (isSystemInDarkTheme()) {
        Color(0xFF482024) to Color(0xFFF87171)
    } else {
        Color(0xFFFEF2F2) to Color(0xFFDC2626)
    }

    @Composable
    fun bilibiliColors() = if (isSystemInDarkTheme()) {
        Color(0xFF452233) to Color(0xFFFB7185)
    } else {
        Color(0xFFFFF1F2) to Color(0xFFE11D48)
    }

    @Composable
    fun articleColors() = if (isSystemInDarkTheme()) {
        Color(0xFF1A3828) to Color(0xFF86EFAC)
    } else {
        Color(0xFFF0FDF4) to Color(0xFF16A34A)
    }

    @Composable
    fun documentColors() = if (isSystemInDarkTheme()) {
        Color(0xFF1E3448) to Color(0xFF7DD3FC)
    } else {
        Color(0xFFF0F9FF) to Color(0xFF0284C7)
    }

    @Composable
    fun textColors() = if (isSystemInDarkTheme()) {
        Color(0xFF382E48) to Color(0xFFD8B4FE)
    } else {
        Color(0xFFF5F3FF) to Color(0xFF9333EA)
    }

    @Composable
    fun emptyOrNeutralColors() = if (isSystemInDarkTheme()) {
        Color(0xFF2E3138) to Color(0xFF94A3B8)
    } else {
        Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    @Composable
    fun badgeColorsFor(
        urlOrText: String,
        documentFilename: String?,
    ): Pair<Color, Color> {
        if (documentFilename != null) {
            return documentColors()
        }
        val trimmed = urlOrText.trim()
        if (trimmed.isEmpty()) {
            return emptyOrNeutralColors()
        }
        val lower = trimmed.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> youtubeColors()
            lower.contains("bilibili.com") || lower.contains("b23.tv") -> bilibiliColors()
            lower.startsWith("http://") || lower.startsWith("https://") -> articleColors()
            else -> textColors()
        }
    }
}
