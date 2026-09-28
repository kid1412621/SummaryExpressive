package me.nanova.summaryexpressive.ui.page.onboarding.section

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import me.nanova.summaryexpressive.ui.component.ContentBadge
import me.nanova.summaryexpressive.ui.component.LengthSelector
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

/**
 * Step 2 Onboarding Demo:
 * Highlights Input URL/Text with ContentBadge, Length Selection,
 * AI Generating Thinking shimmer, and authentic SummaryCard presentation.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SummarizeContentDemo(modifier: Modifier = Modifier) {
    // 3-phase generation lifecycle loop:
    // 0: Ready / Paste & Submit phase (Initial empty field + Paste FAB -> tap 1 -> URL pasted + Submit FAB -> tap 2)
    // 1: Generating phase (shimmering skeleton & AI thinking)
    // 2: Result phase (authentic SummaryCard revealed)
    var stepPhase by remember { mutableIntStateOf(0) }
    var hasPasted by remember { mutableStateOf(false) }
    var isFabPressed by remember { mutableStateOf(false) }
    var showTapRipple by remember { mutableStateOf(false) }
    var isTapActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            stepPhase = 0
            hasPasted = false
            isFabPressed = false
            showTapRipple = false
            isTapActive = false
            delay(800) // View empty input with clean unobstructed Paste FAB

            // Step 2a: Tap Paste FAB
            isTapActive = true
            isFabPressed = true
            showTapRipple = true // Dynamic Tap event on Paste FAB!
            delay(350)
            isFabPressed = false // Button springs back
            hasPasted = true     // URL populates into text field; FAB morphs to Submit icon
            delay(250)           // Ripple finishes expanding
            isTapActive = false  // Lift finger and fade away
            showTapRipple = false

            delay(900) // View populated URL and Submit Checkmark FAB

            // Step 2b: Tap Submit FAB to begin summarization
            isTapActive = true
            isFabPressed = true
            showTapRipple = true // Dynamic Tap event on Submit FAB!
            delay(350)
            isFabPressed = false // Button springs back
            delay(250)           // Ripple finishes expanding
            isTapActive = false  // Lift finger and fade away
            showTapRipple = false

            delay(500) // Transition pause before generating
            stepPhase = 1 // Generating phase (shimmer skeleton)
            delay(2200)
            stepPhase = 2 // Result display phase (SummaryCard)
            delay(4200)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "summarize_demo_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "fab_scale"
    )
    val fabContainerColor by animateColorAsState(
        targetValue = if (!hasPasted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(300),
        label = "fab_container_color"
    )
    val fabContentColor by animateColorAsState(
        targetValue = if (!hasPasted) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(300),
        label = "fab_content_color"
    )
    val inputBorderColor by animateColorAsState(
        targetValue = if (stepPhase == 0 && hasPasted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "input_border_color"
    )
    val rippleScale by animateFloatAsState(
        targetValue = if (showTapRipple) 2.2f else 0.8f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "ripple_scale"
    )
    val rippleAlpha by animateFloatAsState(
        targetValue = if (showTapRipple) 0f else 0.6f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "ripple_alpha"
    )
    val cursorAlpha by animateFloatAsState(
        targetValue = if (stepPhase == 0 && isTapActive) 1f else 0f,
        animationSpec = tween(200),
        label = "cursor_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Input URL Bar (Authentic TextField height ~52dp)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, inputBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = hasPasted,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "input_bar_content"
                ) { pasted ->
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContentBadge(
                            urlOrText = if (pasted) "https://youtu.be/dQw4w9WgXcQ" else "",
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        if (pasted) {
                            Text(
                                text = "https://youtu.be/dQw4w9WgXcQ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = stringResource(R.string.clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.url_or_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Length Selector Connected Buttons using authentic LengthSelector component
            LengthSelector(
                selectedIndex = 1,
                onSelectedIndexChange = {},
                options = listOf(
                    stringResource(R.string.short_length),
                    stringResource(R.string.middle_length),
                    stringResource(R.string.long_length)
                ),
                enabled = true,
                useContainerBackground = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Middle Dynamic Lifecycle Phase Area
            AnimatedContent(
                targetState = stepPhase,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "step2_phase_anim",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { phase ->
                when (phase) {
                    0 -> {
                        // Ready State: Authentic LargeFloatingActionButton at BottomEnd
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            LargeFloatingActionButton(
                                onClick = {},
                                shape = RoundedCornerShape(28.dp),
                                containerColor = fabContainerColor,
                                contentColor = fabContentColor,
                                modifier = Modifier
                                    .padding(end = 4.dp, bottom = 4.dp)
                                    .scale(fabScale)
                            ) {
                                AnimatedContent(
                                    targetState = hasPasted,
                                    transitionSpec = {
                                        fadeIn(tween(200)) togetherWith fadeOut(
                                            tween(
                                                200
                                            )
                                        )
                                    },
                                    label = "fab_icon_swap"
                                ) { pasted ->
                                    if (!pasted) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = stringResource(id = R.string.paste_from_clipboard),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = stringResource(id = R.string.summarize),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // Dynamic Touch Ripple on Tap: Perfectly aligned with FAB bounds and only appears during tap event!
                            if (cursorAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 4.dp, bottom = 4.dp)
                                        .size(96.dp)
                                        .alpha(cursorAlpha),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Expanding touch ripple wave
                                    if (showTapRipple) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .scale(rippleScale)
                                                .clip(CircleShape)
                                                .border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = rippleAlpha),
                                                    CircleShape
                                                )
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = rippleAlpha * 0.25f)
                                                )
                                        )
                                    }

                                    // Soft translucent touch ring
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .scale(if (isFabPressed) 0.78f else 1f)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                            .border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Generating Phase: AI Thinking & Shimmer
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f * shimmerAlpha)
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(rotation)
                                    )
                                    Text(
                                        text = "Summarizing with claude-5-sonnet…",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Shimmer Placeholder Skeleton Bars
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.95f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f * shimmerAlpha))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f * shimmerAlpha))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.70f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f * shimmerAlpha))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Extracted ~3,200 tokens",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Result Phase: Authentic SummaryCard layout
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Header: Badge, Title & Meta
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ContentBadge(
                                            summary = HistorySummary(
                                                title = "",
                                                length = SummaryLength.MEDIUM,
                                                type = SummaryType.VIDEO,
                                                subtype = VideoSubtype.YOUTUBE
                                            ),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Attention Is All You Need",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Ashish Vaswani et al. • Medium • claude-5-sonnet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Body: Overview & Key bullet points
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Introduces the Transformer architecture, eliminating recurrence entirely in favor of multi-head self-attention.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "• Replaces RNN/CNN with multi-head self-attention",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "• Superior translation quality with parallel training",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Bottom Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "Show More",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SummarizeContentDemoPreview() {
    SummaryExpressiveTheme {
        SummarizeContentDemo()
    }
}
