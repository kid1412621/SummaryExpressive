package me.nanova.summaryexpressive.ui.page.onboarding.section

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import kotlin.math.roundToInt

private val DEMO_PROVIDERS = listOf(
    AIProvider.OPENAI,
    AIProvider.CLAUDE,
    AIProvider.GEMINI,
    AIProvider.DEEPSEEK
)

private val DEMO_MODELS_MAP = mapOf(
    AIProvider.OPENAI to listOf("gpt-6-astra", "gpt-5.6-luna"),
    AIProvider.CLAUDE to listOf("claude-5-sonnet", "claude-5.1-fable", "claude-5-opus"),
    AIProvider.GEMINI to listOf("gemini-3.1-pro", "gemini-3.8-flash"),
    AIProvider.DEEPSEEK to listOf("deepseek-chat", "deepseek-reasoner")
)

/**
 * Step 1 Onboarding Demo:
 * Highlights the AI Provider & Model Switcher with two interaction models:
 * 1. Single Tap: Opens selection sheet/popup with provider tabs and model list.
 * 2. Press-Hold & Drag: Quick switcher gesture with clear horizontal (Provider) and vertical (Model) guidance.
 */
@Composable
fun LlmQuickSwitchDemo(modifier: Modifier = Modifier) {
    var selectedMode by remember { mutableIntStateOf(0) } // 0: Tap to Select, 1: Hold to Drag

    // Auto-cycle between Tap and Hold demo modes smoothly
    LaunchedEffect(Unit) {
        while (true) {
            delay(6500)
            selectedMode = (selectedMode + 1) % 2
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Interactive showcase container
            AnimatedContent(
                targetState = selectedMode,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "llm_mode_anim",
                modifier = Modifier.fillMaxSize()
            ) { mode ->
                if (mode == 0) {
                    TapToSelectShowcase()
                } else {
                    HoldToDragShowcase()
                }
            }
        }
    }
}

/**
 * Showcase for Single Tap: Opens selection sheet/popup and selects provider & model.
 * Features dynamic tap event animations: touch down, expanding ripple wave, and target spring reactions.
 */
@Composable
private fun TapToSelectShowcase() {
    // 4-phase animation cycle:
    // 0: Pointer taps switcher pill (1.6s)
    // 1: Pointer moves to select Gemini provider tab (1.6s)
    // 2: Gemini models appear, pointer selects gemini-3.1-pro (1.6s)
    // 3: gemini-3.1-pro confirmed, pointer fades out (1.7s)
    var phase by remember { mutableIntStateOf(0) }
    var isTapping by remember { mutableStateOf(false) }
    var showTapRipple by remember { mutableStateOf(false) }
    var isPointerVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            // Phase 0: Tap top switcher pill
            phase = 0
            isPointerVisible = true
            isTapping = false
            showTapRipple = false
            delay(450)
            isTapping = true
            showTapRipple = true
            delay(350)
            isTapping = false
            delay(800)

            // Phase 1: Tap Gemini tab
            phase = 1
            showTapRipple = false
            delay(450)
            isTapping = true
            showTapRipple = true
            delay(350)
            isTapping = false
            delay(800)

            // Phase 2: Tap Gemini model card
            phase = 2
            showTapRipple = false
            delay(450)
            isTapping = true
            showTapRipple = true
            delay(350)
            isTapping = false
            delay(800)

            // Phase 3: Confirmed & Completed
            phase = 3
            showTapRipple = false
            isPointerVisible = false // Lift and fade away smoothly
            delay(1700)
        }
    }

    val activeProvider = if (phase >= 2) AIProvider.GEMINI else AIProvider.CLAUDE
    val activeModel = if (phase >= 2) "gemini-3.1-pro" else "claude-5-sonnet"

    // Dynamic spring reactions on tapped elements
    val pillScale by animateFloatAsState(
        targetValue = if (phase == 0 && isTapping) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pill_scale"
    )
    val geminiTabScale by animateFloatAsState(
        targetValue = if (phase == 1 && isTapping) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "gemini_tab_scale"
    )
    val modelCardScale by animateFloatAsState(
        targetValue = if (phase == 2 && isTapping) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "model_card_scale"
    )

    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var topPillCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var geminiTabCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var geminiModelCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun calcCenter(coords: LayoutCoordinates?): Offset? {
        val root = rootCoords
        if (coords == null || root == null || !coords.isAttached || !root.isAttached) return null
        return root.localPositionOf(coords, Offset(coords.size.width / 2f, coords.size.height / 2f))
    }

    val topPillCenter = calcCenter(topPillCoords)
    val geminiTabCenter = calcCenter(geminiTabCoords)
    val geminiModelCenter = calcCenter(geminiModelCoords)

    var lastValidTarget by remember { mutableStateOf(Offset.Zero) }
    val currentTarget = when (phase) {
        0 -> topPillCenter
        1 -> geminiTabCenter
        2 -> geminiModelCenter
        else -> geminiModelCenter
    }

    if (currentTarget != null && currentTarget != Offset.Zero) {
        lastValidTarget = currentTarget
    }

    val animatedPointerOffset by animateOffsetAsState(
        targetValue = if (currentTarget != null && currentTarget != Offset.Zero) currentTarget else lastValidTarget,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 280f),
        label = "pointer_offset"
    )

    val pointerScale by animateFloatAsState(
        targetValue = if (isTapping) 0.78f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "pointer_scale"
    )
    val pointerAlpha by animateFloatAsState(
        targetValue = if (isPointerVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "pointer_alpha"
    )
    val rippleScale by animateFloatAsState(
        targetValue = if (showTapRipple) 2.2f else 0.8f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "ripple_scale"
    )
    val rippleAlpha by animateFloatAsState(
        targetValue = if (showTapRipple) 0f else 0.6f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "ripple_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoords = it },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Authentic LlmSwitcher Pill
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (phase == 0 || phase == 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (phase == 0 || phase == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.4f
                    )
                ),
                tonalElevation = if (phase == 0 || phase == 3) 3.dp else 1.dp,
                modifier = Modifier
                    .scale(pillScale)
                    .onGloballyPositioned { topPillCoords = it }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val contentColor =
                        if (phase == 0 || phase == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    Icon(
                        painter = painterResource(activeProvider.icon),
                        contentDescription = activeProvider.name,
                        tint = if (activeProvider.isMonochromeIcon) contentColor else Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = activeModel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (phase == 0 || phase == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Authentic Selection Sheet (Simulating ProviderModelBottomSheet)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.selectAIProvider),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap to choose",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Provider Tabs Row (FilterChip matching ProviderModelBottomSheet.kt)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DEMO_PROVIDERS, key = { it.name }) { provider ->
                            val isSelected = provider == activeProvider
                            FilterChip(
                                selected = isSelected,
                                onClick = {},
                                label = {
                                    Text(
                                        text = provider.id.display,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = provider.icon),
                                        contentDescription = provider.name,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (provider.isMonochromeIcon) {
                                            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current
                                        } else {
                                            Color.Unspecified
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier
                                    .scale(if (provider == AIProvider.GEMINI) geminiTabScale else 1f)
                                    .onGloballyPositioned { coords ->
                                        if (provider == AIProvider.GEMINI) {
                                            geminiTabCoords = coords
                                        }
                                    }
                            )
                        }
                    }

                    // Model Cards list for active provider (Authentic comfortable height)
                    val currentModels = DEMO_MODELS_MAP[activeProvider] ?: emptyList()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentModels.forEach { modelName ->
                            val isChosen = modelName == activeModel
                            val isGeminiTarget = modelName == "gemini-3.1-pro"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (isChosen) BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary
                                ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(if (isGeminiTarget) modelCardScale else 1f)
                                    .onGloballyPositioned { coords ->
                                        if (isGeminiTarget) {
                                            geminiModelCoords = coords
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = modelName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dynamic Touch Ripple on Tap: Centered at exact target, never obscures text or icons!
        val hasValidTarget = lastValidTarget != Offset.Zero
        if (hasValidTarget && pointerAlpha > 0.01f) {
            val density = LocalDensity.current
            val pointerRadiusPx = with(density) { 20.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (animatedPointerOffset.x - pointerRadiusPx).roundToInt(),
                            (animatedPointerOffset.y - pointerRadiusPx).roundToInt()
                        )
                    }
                    .size(40.dp)
                    .alpha(pointerAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Expanding tap ripple wave
                if (showTapRipple) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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

                // Translucent touch ring: thin border & translucent fill ensures text remains 100% legible
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .scale(pointerScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

/**
 * Showcase for Press-Hold & Drag: Quick Switch Gesture with explicit direction indicators:
 * Horizontal: ↔ Provider
 * Vertical: ↕ Model
 * Features dynamic hold ripple, smooth directional gliding, and release dissipation.
 */
@Composable
private fun HoldToDragShowcase() {
    // 4-phase gesture simulation loop:
    // 0: Hold on Claude tab (1.5s)
    // 1: Drag VERTICALLY down to select model (1.7s)
    // 2: Drag HORIZONTALLY right to select Gemini provider (1.7s)
    // 3: Release & Confirm (1.6s)
    var dragPhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            dragPhase = 0
            delay(1500)
            dragPhase = 1
            delay(1700)
            dragPhase = 2
            delay(1700)
            dragPhase = 3
            delay(1600)
        }
    }

    val activeProvider = when (dragPhase) {
        0, 1 -> AIProvider.CLAUDE
        else -> AIProvider.GEMINI
    }

    val activeModel = when (dragPhase) {
        0 -> "claude-5-sonnet"
        1 -> "claude-5.1-fable"
        else -> "gemini-3.8-flash"
    }

    // Infinite hold breathing animation for Phase 0
    val infiniteTransition = rememberInfiniteTransition(label = "hold_pulse")
    val holdPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hold_scale"
    )
    val holdPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hold_alpha"
    )

    // Release dissipation animation for Phase 3
    val releaseAlpha by animateFloatAsState(
        targetValue = if (dragPhase == 3) 0f else 1f,
        animationSpec = tween(450),
        label = "release_alpha"
    )
    val releaseScale by animateFloatAsState(
        targetValue = if (dragPhase == 3) 1.8f else 1f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "release_scale"
    )

    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var claudeTabCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var geminiTabCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var targetModelCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun calcCenter(coords: LayoutCoordinates?): Offset? {
        val root = rootCoords
        if (coords == null || root == null || !coords.isAttached || !root.isAttached) return null
        return root.localPositionOf(coords, Offset(coords.size.width / 2f, coords.size.height / 2f))
    }

    val claudeTabCenter = calcCenter(claudeTabCoords)
    val geminiTabCenter = calcCenter(geminiTabCoords)
    val targetModelCenter = calcCenter(targetModelCoords)

    var lastValidCursorTarget by remember { mutableStateOf(Offset.Zero) }
    val currentCursorTarget = when (dragPhase) {
        0 -> claudeTabCenter
        1 -> {
            val c = claudeTabCenter
            val m = targetModelCenter
            if (c != null && m != null) Offset(c.x, m.y) else c ?: m
        }

        2, 3 -> {
            val g = geminiTabCenter
            val m = targetModelCenter
            if (g != null && m != null) Offset(g.x, m.y) else g ?: m
        }

        else -> targetModelCenter
    }

    if (currentCursorTarget != null && currentCursorTarget != Offset.Zero) {
        lastValidCursorTarget = currentCursorTarget
    }

    val cursorOffset by animateOffsetAsState(
        targetValue = if (currentCursorTarget != null && currentCursorTarget != Offset.Zero) currentCursorTarget else lastValidCursorTarget,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 180f),
        label = "cursor_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoords = it },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Real LlmQuickSwitcherPopup HUD structure
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                tonalElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Title & "Release to switch" badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.quick_switch_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (dragPhase == 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = 0.7f
                            )
                        ) {
                            Text(
                                text = if (dragPhase == 3) "✓ Released" else stringResource(R.string.release_to_switch),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dragPhase == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // ─── 1. HORIZONTAL AXIS: PROVIDER SELECTION ───
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.drag_horizontal_provider),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                        )

                        // Horizontal Provider Row (aligning with LlmQuickSwitcherPopup.kt)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            items(DEMO_PROVIDERS, key = { it.name }) { provider ->
                                val isSelected = provider == activeProvider
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    },
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        if (provider == AIProvider.CLAUDE) claudeTabCoords = coords
                                        if (provider == AIProvider.GEMINI) geminiTabCoords = coords
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(provider.icon),
                                            contentDescription = provider.name,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (provider.isMonochromeIcon) {
                                                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current
                                            } else {
                                                Color.Unspecified
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = provider.id.display,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── 2. VERTICAL AXIS: MODEL SELECTION ───
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.drag_vertical_model),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                        )

                        // Vertical Model column for active provider
                        val models = DEMO_MODELS_MAP[activeProvider] ?: listOf("model-1", "model-2")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            models.forEach { model ->
                                val isSelected = model == activeModel
                                val isTargetModel =
                                    (model == "claude-5.1-fable" && activeProvider == AIProvider.CLAUDE) ||
                                            (model == "gemini-3.8-flash" && activeProvider == AIProvider.GEMINI)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coords ->
                                            if (isTargetModel) {
                                                targetModelCoords = coords
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 11.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = model,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
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

        // Calibrated Gesture Cursor Tracking across Horizontal & Vertical Axes
        val hasValidCursor = lastValidCursorTarget != Offset.Zero
        if (hasValidCursor && releaseAlpha > 0.01f) {
            val density = LocalDensity.current
            val cursorRadiusPx = with(density) { 22.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (cursorOffset.x - cursorRadiusPx).roundToInt(),
                            (cursorOffset.y - cursorRadiusPx).roundToInt()
                        )
                    }
                    .size(44.dp)
                    .scale(releaseScale)
                    .alpha(releaseAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing wave when holding down in Phase 0
                if (dragPhase == 0) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .scale(holdPulseScale)
                            .clip(CircleShape)
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = holdPulseAlpha),
                                CircleShape
                            )
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = holdPulseAlpha * 0.25f)
                            )
                    )
                }

                // Dynamic direction indicator badge floating near the touch point
                val badgeText = when (dragPhase) {
                    0 -> "Hold"
                    1 -> "↕"
                    2 -> "↔"
                    else -> "✓"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (dragPhase) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.offset(x = 24.dp, y = (-16).dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Translucent finger touch ring (never obscures provider or model text)
                val indicatorColor =
                    if (dragPhase == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(indicatorColor.copy(alpha = 0.20f))
                        .border(1.5.dp, indicatorColor.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(indicatorColor.copy(alpha = 0.60f))
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LlmQuickSwitchDemoPreview() {
    SummaryExpressiveTheme {
        LlmQuickSwitchDemo()
    }
}
