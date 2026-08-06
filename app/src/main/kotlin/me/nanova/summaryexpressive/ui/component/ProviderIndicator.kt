package me.nanova.summaryexpressive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import me.nanova.summaryexpressive.llm.AIProvider

import androidx.compose.ui.unit.Dp

@Composable
fun ProviderIndicator(
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
        if (provider != null) {
            if (onClick != null) {
                IconButton(onClick = onClick) {
                    Icon(
                        painter = painterResource(id = provider.icon),
                        contentDescription = provider.name,
                        modifier = Modifier.size(iconSize),
                        tint = if (provider.isMonochromeIcon) LocalContentColor.current else Color.Unspecified
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = provider.icon),
                    contentDescription = provider.name,
                    modifier = Modifier.size(iconSize),
                    tint = if (provider.isMonochromeIcon) LocalContentColor.current else Color.Unspecified
                )
            }
        }

        if (model?.isNotBlank() == true) {
            val shortName = model.replaceFirst(
                Regex("^(gpt|gemini|claude|deepseek)-", RegexOption.IGNORE_CASE),
                ""
            )

            Text(
                text = shortName,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = if (boxSize < 48.dp) 7.dp else (-5).dp)
                    .wrapContentSize(unbounded = true)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 2.dp, vertical = 0.dp)
            )
        }
    }
}
