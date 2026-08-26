package me.nanova.summaryexpressive.ui.component

import androidx.compose.foundation.lazy.LazyListState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReorderableListStateTest {

    @Test
    fun `test drag start, end and cancel state transitions`() {
        val lazyListState = LazyListState(0, 0)
        var moved = false
        val state = ReorderableListState(
            lazyListState = lazyListState,
            scope = null,
            hapticFeedback = null,
            onMove = { _, _ -> moved = true }
        )

        assertNull(state.draggingKey)
        assertEquals(0f, state.draggedDistance)

        state.onDragStart("key_1")
        assertEquals("key_1", state.draggingKey)
        assertEquals(0f, state.draggedDistance)

        state.onDragEnd()
        assertNull(state.draggingKey)
        assertEquals(0f, state.draggedDistance)

        state.onDragStart("key_2")
        assertEquals("key_2", state.draggingKey)

        state.onDragCancel()
        assertNull(state.draggingKey)
        assertEquals(0f, state.draggedDistance)
    }
}
