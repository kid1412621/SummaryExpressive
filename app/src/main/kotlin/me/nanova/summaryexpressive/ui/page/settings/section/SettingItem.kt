package me.nanova.summaryexpressive.ui.page.settings.section

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable as CoreAnimatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Android 16 Settings Item with Individual Rounded Shape and Tonal Background
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    iconBadge: @Composable () -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = groupShape(position)
    val defaultContainer = MaterialTheme.colorScheme.surfaceContainer
    val highlightContainer = MaterialTheme.colorScheme.secondaryContainer

    val animatedColor = remember(defaultContainer) { Animatable(defaultContainer) }
    val animatedBorderWidth = remember { CoreAnimatable(0f) }

    LaunchedEffect(highlighted, defaultContainer, highlightContainer) {
        if (highlighted) {
            launch {
                animatedColor.animateTo(highlightContainer, animationSpec = inTween())
                animatedColor.animateTo(defaultContainer, animationSpec = outTween())
            }
            launch {
                animatedBorderWidth.animateTo(2f, animationSpec = inTween())
                animatedBorderWidth.animateTo(0f, animationSpec = outTween())
            }
        } else {
            animatedColor.snapTo(defaultContainer)
            animatedBorderWidth.snapTo(0f)
        }
    }

    val clickableModifier = when {
        !enabled -> Modifier
        onLongClick != null && onClick != null -> Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = animatedColor.value,
        border = if (animatedBorderWidth.value > 0) BorderStroke(
            animatedBorderWidth.value.dp,
            MaterialTheme.colorScheme.secondary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconBadge()
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

/**
 * 40dp Circular Icon Badge Container with Tonal Tinting
 */
@Composable
fun SettingIconBadge(
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
 * Curated Android 16 Icon Badge Palettes
 */
object SettingBadges {
    @Composable
    fun aiColors() = if (isSystemInDarkTheme()) {
        Color(0xFF2D325A) to Color(0xFFA5B4FC)
    } else {
        Color(0xFFEEF2FF) to Color(0xFF4F46E5)
    }

    @Composable
    fun modelColors() = if (isSystemInDarkTheme()) {
        Color(0xFF3B2856) to Color(0xFFD8B4FE)
    } else {
        Color(0xFFF5EEFD) to Color(0xFF9333EA)
    }

    @Composable
    fun advancedColors() = if (isSystemInDarkTheme()) {
        Color(0xFF1E3A40) to Color(0xFF5EEAD4)
    } else {
        Color(0xFFE6FFFA) to Color(0xFF0D9488)
    }

    @Composable
    fun languageColors() = if (isSystemInDarkTheme()) {
        Color(0xFF1E3354) to Color(0xFF93C5FD)
    } else {
        Color(0xFFEFF6FF) to Color(0xFF2563EB)
    }

    @Composable
    fun themeColors() = if (isSystemInDarkTheme()) {
        Color(0xFF3F2344) to Color(0xFFF472B6)
    } else {
        Color(0xFFFDF2F8) to Color(0xFFDB2777)
    }

    @Composable
    fun dynamicColorColors() = if (isSystemInDarkTheme()) {
        Color(0xFF4A3319) to Color(0xFFFDBA74)
    } else {
        Color(0xFFFFF7ED) to Color(0xFFEA580C)
    }

    @Composable
    fun bilibiliColors() = if (isSystemInDarkTheme()) {
        Color(0xFF452233) to Color(0xFFFB7185)
    } else {
        Color(0xFFFFF1F2) to Color(0xFFE11D48)
    }

    @Composable
    fun linkColors() = if (isSystemInDarkTheme()) {
        Color(0xFF1A3828) to Color(0xFF86EFAC)
    } else {
        Color(0xFFF0FDF4) to Color(0xFF16A34A)
    }

    @Composable
    fun tutorialColors() = if (isSystemInDarkTheme()) {
        Color(0xFF1E3448) to Color(0xFF7DD3FC)
    } else {
        Color(0xFFF0F9FF) to Color(0xFF0284C7)
    }

    @Composable
    fun playStoreColors() = if (isSystemInDarkTheme()) {
        Color(0xFF423415) to Color(0xFFFCD34D)
    } else {
        Color(0xFFFEFCE8) to Color(0xFFCA8A04)
    }

    @Composable
    fun discordColors() = if (isSystemInDarkTheme()) {
        Color(0xFF2B2E56) to Color(0xFFA5B4FC)
    } else {
        Color(0xFFEEF2FF) to Color(0xFF5865F2)
    }

    @Composable
    fun githubColors() = if (isSystemInDarkTheme()) {
        Color(0xFF2A2E35) to Color(0xFFCBD5E1)
    } else {
        Color(0xFFF1F5F9) to Color(0xFF334155)
    }
}

private fun <T> inTween(): TweenSpec<T> = tween(durationMillis = 700)
private fun <T> outTween(): TweenSpec<T> = tween(durationMillis = 1000, delayMillis = 500)
