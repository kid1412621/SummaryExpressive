package me.nanova.summaryexpressive.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.TimeoutCancellationException
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

/**
 * State holder for managing 2D quick switcher gesture interactions.
 * Handles long-press detection, horizontal provider navigation, vertical model navigation,
 * and tactile haptic feedback.
 */
class LlmSwitcherState(
    availableProviders: List<AIProvider> = emptyList(),
    getModelsForProvider: (AIProvider) -> List<String> = { it.defaultModelIds },
    var stepXPx: Float = 100f,
    var stepYPx: Float = 100f,
    var hapticFeedback: HapticFeedback? = null,
    var onConfirmSwitch: ((AIProvider, String) -> Unit)? = null,
) {
    var availableProviders: List<AIProvider> by mutableStateOf(availableProviders)
    var getModelsForProvider: (AIProvider) -> List<String> by mutableStateOf(getModelsForProvider)

    var isLongPressing: Boolean by mutableStateOf(false)
        internal set

    var selectedProvider: AIProvider? by mutableStateOf(null)
        internal set

    var selectedModel: String by mutableStateOf("")
        internal set

    var accumulatedX: Float = 0f
        internal set

    var accumulatedY: Float = 0f
        internal set

    fun onLongPressStart(initialProvider: AIProvider?, initialModel: String?) {
        val curProvider = initialProvider?.takeIf { it in availableProviders }
            ?: availableProviders.firstOrNull()
            ?: AIProvider.OPENAI

        val curModels = getModelsForProvider(curProvider)
        val curModel = initialModel?.takeIf { it in curModels }
            ?: curModels.firstOrNull()
            ?: ""

        selectedProvider = curProvider
        selectedModel = curModel
        accumulatedX = 0f
        accumulatedY = 0f
        isLongPressing = true
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onDrag(dragDelta: Offset) {
        if (!isLongPressing) return

        accumulatedX += dragDelta.x
        accumulatedY += dragDelta.y

        val curProvider = selectedProvider ?: return
        var curModels = getModelsForProvider(curProvider)

        // Horizontal movement: Select Provider
        if (accumulatedX >= stepXPx) {
            val currentIdx = availableProviders.indexOf(curProvider)
            if (currentIdx in 0 until availableProviders.size - 1) {
                val nextProvider = availableProviders[currentIdx + 1]
                selectedProvider = nextProvider
                curModels = getModelsForProvider(nextProvider)
                selectedModel = curModels.firstOrNull() ?: ""
                accumulatedY = 0f
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            accumulatedX = 0f
        } else if (accumulatedX <= -stepXPx) {
            val currentIdx = availableProviders.indexOf(curProvider)
            if (currentIdx > 0) {
                val prevProvider = availableProviders[currentIdx - 1]
                selectedProvider = prevProvider
                curModels = getModelsForProvider(prevProvider)
                selectedModel = curModels.firstOrNull() ?: ""
                accumulatedY = 0f
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            accumulatedX = 0f
        }

        // Vertical movement: Select Model
        if (curModels.isNotEmpty()) {
            if (accumulatedY >= stepYPx) {
                val currentIdx = curModels.indexOf(selectedModel)
                if (currentIdx in 0 until curModels.size - 1) {
                    selectedModel = curModels[currentIdx + 1]
                    hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                accumulatedY = 0f
            } else if (accumulatedY <= -stepYPx) {
                val currentIdx = curModels.indexOf(selectedModel)
                if (currentIdx > 0) {
                    selectedModel = curModels[currentIdx - 1]
                    hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                accumulatedY = 0f
            }
        }
    }

    fun onRelease() {
        if (!isLongPressing) return
        val provider = selectedProvider
        val model = selectedModel
        isLongPressing = false
        accumulatedX = 0f
        accumulatedY = 0f
        if (provider != null) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.Confirm)
            onConfirmSwitch?.invoke(provider, model)
        }
    }

    fun onCancel() {
        isLongPressing = false
        accumulatedX = 0f
        accumulatedY = 0f
    }
}

@Composable
fun rememberLlmSwitcherState(
    availableProviders: List<AIProvider>,
    getModelsForProvider: (AIProvider) -> List<String>,
    stepXPx: Float,
    stepYPx: Float,
    hapticFeedback: HapticFeedback? = LocalHapticFeedback.current,
    onConfirmSwitch: ((AIProvider, String) -> Unit)? = null,
): LlmSwitcherState {
    val state = remember {
        LlmSwitcherState(
            availableProviders = availableProviders,
            getModelsForProvider = getModelsForProvider,
            stepXPx = stepXPx,
            stepYPx = stepYPx,
            hapticFeedback = hapticFeedback,
            onConfirmSwitch = onConfirmSwitch
        )
    }
    state.availableProviders = availableProviders
    state.getModelsForProvider = getModelsForProvider
    state.stepXPx = stepXPx
    state.stepYPx = stepYPx
    state.hapticFeedback = hapticFeedback
    state.onConfirmSwitch = onConfirmSwitch
    return state
}

@Composable
fun LlmSwitcher(
    provider: AIProvider?,
    model: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    boxSize: Dp = 48.dp,
    fontSize: TextUnit = 8.sp,
    onClick: (() -> Unit)? = null,
    availableProviders: List<AIProvider> = AIProvider.entries,
    getModelsForProvider: (AIProvider) -> List<String> = { it.defaultModelIds },
    onConfirmSwitch: ((AIProvider, String) -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnConfirmSwitch by rememberUpdatedState(onConfirmSwitch)
    val currentProviders by rememberUpdatedState(availableProviders)
    val currentGetModels by rememberUpdatedState(getModelsForProvider)
    val currentProvider by rememberUpdatedState(provider)
    val currentModel by rememberUpdatedState(model)

    val stepXPx = with(density) { 36.dp.toPx() }
    val stepYPx = with(density) { 32.dp.toPx() }

    val switcherState = rememberLlmSwitcherState(
        availableProviders = currentProviders,
        getModelsForProvider = currentGetModels,
        stepXPx = stepXPx,
        stepYPx = stepYPx,
        hapticFeedback = haptics,
        onConfirmSwitch = currentOnConfirmSwitch
    )

    val canLongPress = currentOnConfirmSwitch != null && currentProviders.isNotEmpty()
    val isInteractive = currentOnClick != null || canLongPress

    val scale by animateFloatAsState(
        targetValue = if (switcherState.isLongPressing) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "switcher_scale"
    )

    Box(
        modifier = modifier
            .size(boxSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                if (isInteractive) {
                    role = Role.Button
                }
            }
            .indication(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = boxSize / 2)
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val pressInteraction = PressInteraction.Press(down.position)
                            interactionSource.tryEmit(pressInteraction)

                            val pointerId = down.id
                            val startPos = down.position
                            val touchSlop = viewConfiguration.touchSlop
                            var isLongPress = false
                            var isTap = false

                            try {
                                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change =
                                            event.changes.firstOrNull { it.id == pointerId }
                                                ?: break

                                        if (change.changedToUp() || !change.pressed) {
                                            change.consume()
                                            isTap = true
                                            break
                                        }

                                        val moved = (change.position - startPos).getDistance()
                                        if (moved > touchSlop) {
                                            break
                                        }
                                    }
                                }
                            } catch (_: TimeoutCancellationException) {
                                isLongPress = true
                            } catch (_: Exception) {
                                isLongPress = true
                            }

                            if (isTap) {
                                interactionSource.tryEmit(PressInteraction.Release(pressInteraction))
                                currentOnClick?.invoke()
                            } else if (isLongPress && canLongPress) {
                                interactionSource.tryEmit(PressInteraction.Release(pressInteraction))
                                switcherState.onLongPressStart(currentProvider, currentModel)

                                var currentPointerId = pointerId
                                var lastPosition = startPos
                                var confirmed = false

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change =
                                            event.changes.firstOrNull { it.id == currentPointerId }
                                                ?: event.changes.firstOrNull()
                                                ?: break

                                        currentPointerId = change.id

                                        if (change.changedToUp() || !change.pressed) {
                                            change.consume()
                                            confirmed = true
                                            break
                                        }

                                        val dragDelta = change.position - lastPosition
                                        lastPosition = change.position
                                        change.consume()

                                        switcherState.onDrag(dragDelta)
                                    }
                                } finally {
                                    if (confirmed) {
                                        switcherState.onRelease()
                                    } else {
                                        switcherState.onCancel()
                                    }
                                }
                            } else {
                                interactionSource.tryEmit(PressInteraction.Cancel(pressInteraction))
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        ProviderIcon(provider, iconSize)

        if (model?.isNotBlank() == true) {
            ModelLabel(
                model = model,
                boxSize = boxSize,
                fontSize = fontSize,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (switcherState.isLongPressing && canLongPress) {
            val verticalGapPx = with(density) { 8.dp.roundToPx() }
            val bottomMarginPx = with(density) { 16.dp.roundToPx() }
            val popupPositionProvider = remember(verticalGapPx, bottomMarginPx) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset {
                        val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
                        val y = (anchorBounds.bottom + verticalGapPx).coerceIn(
                            0,
                            (windowSize.height - popupContentSize.height - bottomMarginPx).coerceAtLeast(
                                0
                            )
                        )
                        return IntOffset(x, y)
                    }
                }
            }

            Popup(
                popupPositionProvider = popupPositionProvider,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                val currentModels =
                    switcherState.selectedProvider?.let { currentGetModels(it) } ?: emptyList()
                LlmQuickSwitcherPopup(
                    selectedProvider = switcherState.selectedProvider,
                    selectedModel = switcherState.selectedModel,
                    availableProviders = currentProviders,
                    modelsForProvider = currentModels
                )
            }
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
private fun ModelLabel(
    model: String,
    boxSize: Dp,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 8.sp,
) {
    val shortName = model.replaceFirst(
        Regex("^(gpt|gemini|claude|deepseek|mistral|kimi|minimax|glm)-", RegexOption.IGNORE_CASE),
        ""
    )

    Text(
        text = shortName,
        fontSize = fontSize,
        lineHeight = fontSize,
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
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 2.dp, vertical = 0.dp)
    )
}

@Preview
@Composable
private fun LlmSwitcherPreview() {
    SummaryExpressiveTheme {
        LlmSwitcher(
            provider = AIProvider.OPENAI,
            model = "gpt-4o",
            onClick = {},
            availableProviders = AIProvider.entries,
            onConfirmSwitch = { _, _ -> }
        )
    }
}


