package me.nanova.summaryexpressive.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.llm.AIProvider

@Composable
fun LlmIndicator(
    provider: AIProvider?,
    model: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    boxSize: Dp = 48.dp,
) {
    LlmSwitcher(
        provider = provider,
        model = model,
        modifier = modifier,
        iconSize = iconSize,
        boxSize = boxSize,
        onClick = null
    )
}
