package me.nanova.summaryexpressive.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nanova.summaryexpressive.llm.AIProvider

enum class LlmIndicatorStyle {
    /**
     * Compact square badge with centered icon and bottom-pinned model label.
     * Suitable for dialog headers, action buttons, or avatar spots.
     */
    BADGE,

    /**
     * Horizontal pill chip with provider icon and model label side-by-side.
     * Expressive Material 3 container style suitable for metadata rows.
     */
    PILL
}

@Composable
fun LlmIndicator(
    provider: AIProvider?,
    model: String?,
    modifier: Modifier = Modifier,
    style: LlmIndicatorStyle = LlmIndicatorStyle.BADGE,
    iconSize: Dp = if (style == LlmIndicatorStyle.PILL) 14.dp else 24.dp,
    boxSize: Dp = 48.dp,
    fontSize: TextUnit = if (style == LlmIndicatorStyle.PILL) 11.sp else 8.sp,
) {
    when (style) {
        LlmIndicatorStyle.BADGE -> {
            LlmSwitcher(
                provider = provider,
                model = model,
                modifier = modifier,
                iconSize = iconSize,
                boxSize = boxSize,
                fontSize = fontSize,
                onClick = null
            )
        }

        LlmIndicatorStyle.PILL -> {
            LlmIndicatorPill(
                provider = provider,
                model = model,
                modifier = modifier,
                iconSize = iconSize,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun LlmIndicatorPill(
    provider: AIProvider?,
    model: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    fontSize: TextUnit = 11.sp,
) {
    if (provider == null && model.isNullOrBlank()) return

    val displayModel = remember(model) {
        model?.substringAfter('/')?.trim()?.ifBlank { null }
    }
    val displayText = displayModel ?: provider?.id?.display ?: provider?.name

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            if (provider != null) {
                Icon(
                    painter = painterResource(id = provider.icon),
                    contentDescription = provider.name,
                    modifier = Modifier.size(iconSize),
                    tint = if (provider.isMonochromeIcon) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color.Unspecified
                    }
                )
            }
            if (!displayText.isNullOrBlank()) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
