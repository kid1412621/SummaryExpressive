package me.nanova.summaryexpressive.ui.page.settings.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * Returns an expressive rounded corner shape matching Android 16 System Settings
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
 * Android 16 Grouped Island Container with 2.dp inter-item gaps
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        content()
    }
}
