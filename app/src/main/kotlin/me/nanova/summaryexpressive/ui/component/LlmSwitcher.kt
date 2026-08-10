package me.nanova.summaryexpressive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import me.nanova.summaryexpressive.llm.AIProvider

import androidx.compose.ui.unit.Dp

@Composable
fun LlmSwitcher(
    provider: AIProvider?,
    model: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    boxSize: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        if (onClick != null) {
            IconButton(onClick = onClick) {
                ProviderIcon(provider, iconSize)
            }
        } else {
            ProviderIcon(provider, iconSize)
        }

        if (model?.isNotBlank() == true) {
            ModelLabel(
                model = model,
                boxSize = boxSize,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ProviderIcon(provider: AIProvider?, iconSize: Dp) {
    if (provider != null) {
        Icon(
            painter = painterResource(id = provider.icon),
            contentDescription = provider.name,
            modifier = Modifier.size(iconSize),
            tint = if (provider.isMonochromeIcon) LocalContentColor.current else Color.Unspecified
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = "Select AI Provider",
            modifier = Modifier.size(iconSize),
            tint = LocalContentColor.current
        )
    }
}

@Composable
private fun ModelLabel(model: String, boxSize: Dp, modifier: Modifier = Modifier) {
    val shortName = model.replaceFirst(
        Regex("^(gpt|gemini|claude|deepseek|mistral)-", RegexOption.IGNORE_CASE),
        ""
    )

    Text(
        text = shortName,
        fontSize = 8.sp,
        lineHeight = 8.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .offset(y = if (boxSize < 48.dp) 7.dp else (-5).dp)
            .widthIn(max = boxSize * 1.5f)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 2.dp, vertical = 0.dp)
    )
}

