package me.nanova.summaryexpressive.ui.page.history.section

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
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype

/**
 * 40dp Circular Icon Badge Container with Tonal Tinting
 */
@Composable
fun HistoryIconBadge(
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
 * Curated Android 16 Icon Badge Palettes for History Summary Content Types
 */
object HistoryBadges {
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
    fun genericVideoColors() = if (isSystemInDarkTheme()) {
        Color(0xFF2D325A) to Color(0xFFA5B4FC)
    } else {
        Color(0xFFEEF2FF) to Color(0xFF4F46E5)
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
        Color(0xFF4A3319) to Color(0xFFFDBA74)
    } else {
        Color(0xFFFFF7ED) to Color(0xFFEA580C)
    }

    @Composable
    fun badgeColorsFor(type: SummaryType, subtype: VideoSubtype?): Pair<Color, Color> {
        return when (type) {
            SummaryType.VIDEO -> when (subtype) {
                VideoSubtype.YOUTUBE -> youtubeColors()
                VideoSubtype.BILIBILI -> bilibiliColors()
                else -> genericVideoColors()
            }
            SummaryType.ARTICLE -> articleColors()
            SummaryType.DOCUMENT -> documentColors()
            SummaryType.TEXT -> textColors()
        }
    }
}
