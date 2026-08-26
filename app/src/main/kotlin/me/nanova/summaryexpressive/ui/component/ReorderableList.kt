package me.nanova.summaryexpressive.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import kotlin.time.Duration.Companion.milliseconds

/**
 * State holder for managing reorderable list interactions in MD3 Expressive design.
 * Handles drag offsets, reordering thresholds, auto-scrolling near viewport edges, and tactile haptic feedback.
 */
class ReorderableListState(
    val lazyListState: LazyListState,
    private val scope: CoroutineScope? = null,
    private val hapticFeedback: HapticFeedback? = null,
    private val onMove: (Int, Int) -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var draggedDistance by mutableFloatStateOf(0f)
        private set

    private var autoScrollJob: Job? = null
    private var currentScrollSpeed = 0f

    fun onDragStart(key: Any) {
        draggingKey = key
        draggedDistance = 0f
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onDrag(dragAmountY: Float) {
        val currentKey = draggingKey ?: return
        draggedDistance += dragAmountY

        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val currentItem = visibleItems.find { it.key == currentKey } ?: return
        val currentItemCenter = currentItem.offset + draggedDistance + currentItem.size / 2f

        val targetItem = visibleItems.find { item ->
            item.key != currentKey &&
                    if (item.offset < currentItem.offset) {
                        currentItemCenter < (item.offset + item.size / 2f)
                    } else {
                        currentItemCenter > (item.offset + item.size / 2f)
                    }
        }

        if (targetItem != null) {
            val fromIndex = currentItem.index
            val toIndex = targetItem.index
            if (fromIndex != toIndex) {
                onMove(fromIndex, toIndex)
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                draggedDistance += (currentItem.offset - targetItem.offset)
            }
        }

        // Auto-scroll when dragging near viewport boundaries
        if (scope != null) {
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val viewportHeight = viewportEnd - viewportStart

            if (viewportHeight > 0) {
                val scrollMargin = (viewportHeight * 0.16f).coerceIn(48f, 180f)
                val topBoundary = viewportStart + scrollMargin
                val bottomBoundary = viewportEnd - scrollMargin

                val currentTop = currentItem.offset + draggedDistance
                val currentBottom = currentTop + currentItem.size

                if (currentTop < topBoundary && lazyListState.canScrollBackward) {
                    val intensity = ((topBoundary - currentTop) / scrollMargin).coerceIn(0f, 1f)
                    startAutoScroll(-1f * (10f + 25f * intensity))
                } else if (currentBottom > bottomBoundary && lazyListState.canScrollForward) {
                    val intensity =
                        ((currentBottom - bottomBoundary) / scrollMargin).coerceIn(0f, 1f)
                    startAutoScroll(10f + 25f * intensity)
                } else {
                    stopAutoScroll()
                }
            }
        }
    }

    private fun startAutoScroll(speed: Float) {
        currentScrollSpeed = speed
        if (autoScrollJob?.isActive == true) return
        val coroutineScope = scope ?: return
        autoScrollJob = coroutineScope.launch {
            while (draggingKey != null) {
                lazyListState.scrollBy(currentScrollSpeed)
                delay(16.milliseconds)
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        currentScrollSpeed = 0f
    }

    fun onDragEnd() {
        draggingKey = null
        draggedDistance = 0f
        stopAutoScroll()
    }

    fun onDragCancel() {
        draggingKey = null
        draggedDistance = 0f
        stopAutoScroll()
    }
}

@Composable
fun rememberReorderableListState(
    lazyListState: LazyListState = rememberLazyListState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    hapticFeedback: HapticFeedback = LocalHapticFeedback.current,
    onMove: (Int, Int) -> Unit,
): ReorderableListState {
    return remember(lazyListState, scope, hapticFeedback, onMove) {
        ReorderableListState(
            lazyListState = lazyListState,
            scope = scope,
            hapticFeedback = hapticFeedback,
            onMove = onMove
        )
    }
}

/**
 * Reorderable item container with MD3 Expressive spring physics.
 * Animates elevation, scale, and neighboring item displacement using spring physics.
 */
@Composable
fun LazyItemScope.ReorderableItem(
    reorderState: ReorderableListState,
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    val isDragging = reorderState.draggingKey == key

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "reorder_item_scale"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "reorder_item_elevation"
    )

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                translationY = if (isDragging) reorderState.draggedDistance else 0f
                scaleX = scale
                scaleY = scale
                this.shadowElevation = shadowElevation.toPx()
            }
            .animateItem(
                placementSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        content(isDragging)
    }
}

/**
 * Drag handle composable conforming to MD3 interactive touch target standards (minimum 48dp).
 */
@Composable
fun ReorderDragHandle(
    reorderState: ReorderableListState,
    key: Any,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.dragToReorder),
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            }
            .pointerInput(key) {
                detectDragGestures(
                    onDragStart = { reorderState.onDragStart(key) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        reorderState.onDrag(dragAmount.y)
                    },
                    onDragEnd = { reorderState.onDragEnd() },
                    onDragCancel = { reorderState.onDragCancel() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
