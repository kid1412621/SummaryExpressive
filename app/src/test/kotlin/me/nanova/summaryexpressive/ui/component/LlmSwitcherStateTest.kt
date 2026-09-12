package me.nanova.summaryexpressive.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import me.nanova.summaryexpressive.llm.AIProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LlmSwitcherStateTest {

    private class FakeHapticFeedback : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events.add(hapticFeedbackType)
        }
    }

    private val providers = listOf(AIProvider.OPENAI, AIProvider.GEMINI, AIProvider.CLAUDE)
    private val modelsMap = mapOf(
        AIProvider.OPENAI to listOf("gpt-4o", "gpt-4o-mini"),
        AIProvider.GEMINI to listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.5-flash-8b"),
        AIProvider.CLAUDE to listOf("claude-3-5-sonnet")
    )

    private fun createTestState(
        hapticFeedback: HapticFeedback? = null,
        onConfirm: ((AIProvider, String) -> Unit)? = null
    ): LlmSwitcherState {
        return LlmSwitcherState(
            availableProviders = providers,
            getModelsForProvider = { modelsMap[it] ?: emptyList() },
            stepXPx = 100f,
            stepYPx = 80f,
            hapticFeedback = hapticFeedback,
            onConfirmSwitch = onConfirm
        )
    }

    @Test
    fun `test onLongPressStart initializes state and triggers long press haptic`() {
        val haptics = FakeHapticFeedback()
        val state = createTestState(hapticFeedback = haptics)

        assertFalse(state.isLongPressing)

        state.onLongPressStart(initialProvider = AIProvider.GEMINI, initialModel = "gemini-1.5-flash")

        assertTrue(state.isLongPressing)
        assertEquals(AIProvider.GEMINI, state.selectedProvider)
        assertEquals("gemini-1.5-flash", state.selectedModel)
        assertEquals(1, haptics.events.size)
        assertEquals(HapticFeedbackType.LongPress, haptics.events.first())
    }

    @Test
    fun `test onLongPressStart falls back when provider or model not found`() {
        val state = createTestState()

        // Null provider and model
        state.onLongPressStart(initialProvider = null, initialModel = null)
        assertEquals(AIProvider.OPENAI, state.selectedProvider)
        assertEquals("gpt-4o", state.selectedModel)

        // Invalid model for provider
        state.onLongPressStart(initialProvider = AIProvider.OPENAI, initialModel = "non-existent-model")
        assertEquals(AIProvider.OPENAI, state.selectedProvider)
        assertEquals("gpt-4o", state.selectedModel)
    }

    @Test
    fun `test horizontal drag cycles providers forward and backward with clamping`() {
        val haptics = FakeHapticFeedback()
        val state = createTestState(hapticFeedback = haptics)

        state.onLongPressStart(initialProvider = AIProvider.OPENAI, initialModel = "gpt-4o")
        haptics.events.clear()

        // Drag right to switch to GEMINI
        state.onDrag(Offset(100f, 0f))
        assertEquals(AIProvider.GEMINI, state.selectedProvider)
        assertEquals("gemini-1.5-pro", state.selectedModel)
        assertEquals(listOf(HapticFeedbackType.TextHandleMove), haptics.events)

        // Drag right again to switch to CLAUDE
        state.onDrag(Offset(100f, 0f))
        assertEquals(AIProvider.CLAUDE, state.selectedProvider)
        assertEquals("claude-3-5-sonnet", state.selectedModel)

        // Drag right beyond last provider -> clamped at CLAUDE, no extra haptics
        val countBeforeClamp = haptics.events.size
        state.onDrag(Offset(100f, 0f))
        assertEquals(AIProvider.CLAUDE, state.selectedProvider)
        assertEquals(countBeforeClamp, haptics.events.size)

        // Drag left to switch back to GEMINI
        state.onDrag(Offset(-100f, 0f))
        assertEquals(AIProvider.GEMINI, state.selectedProvider)
        assertEquals("gemini-1.5-pro", state.selectedModel)

        // Drag left to switch back to OPENAI
        state.onDrag(Offset(-100f, 0f))
        assertEquals(AIProvider.OPENAI, state.selectedProvider)
        assertEquals("gpt-4o", state.selectedModel)

        // Drag left beyond first provider -> clamped at OPENAI
        val countBeforeFirstClamp = haptics.events.size
        state.onDrag(Offset(-100f, 0f))
        assertEquals(AIProvider.OPENAI, state.selectedProvider)
        assertEquals(countBeforeFirstClamp, haptics.events.size)
    }

    @Test
    fun `test vertical drag cycles models within current provider with clamping`() {
        val haptics = FakeHapticFeedback()
        val state = createTestState(hapticFeedback = haptics)

        // Start at GEMINI (has 3 models: gemini-1.5-pro, gemini-1.5-flash, gemini-1.5-flash-8b)
        state.onLongPressStart(initialProvider = AIProvider.GEMINI, initialModel = "gemini-1.5-pro")
        haptics.events.clear()

        // Drag down to next model
        state.onDrag(Offset(0f, 80f))
        assertEquals("gemini-1.5-flash", state.selectedModel)
        assertEquals(listOf(HapticFeedbackType.TextHandleMove), haptics.events)

        // Drag down to last model
        state.onDrag(Offset(0f, 80f))
        assertEquals("gemini-1.5-flash-8b", state.selectedModel)

        // Drag down beyond last model -> clamped
        val countBeforeClamp = haptics.events.size
        state.onDrag(Offset(0f, 80f))
        assertEquals("gemini-1.5-flash-8b", state.selectedModel)
        assertEquals(countBeforeClamp, haptics.events.size)

        // Drag up to previous model
        state.onDrag(Offset(0f, -80f))
        assertEquals("gemini-1.5-flash", state.selectedModel)

        // Drag up to first model
        state.onDrag(Offset(0f, -80f))
        assertEquals("gemini-1.5-pro", state.selectedModel)

        // Drag up beyond first model -> clamped
        val countBeforeFirstClamp = haptics.events.size
        state.onDrag(Offset(0f, -80f))
        assertEquals("gemini-1.5-pro", state.selectedModel)
        assertEquals(countBeforeFirstClamp, haptics.events.size)
    }

    @Test
    fun `test fractional drag accumulation`() {
        val state = createTestState()
        state.onLongPressStart(initialProvider = AIProvider.OPENAI, initialModel = "gpt-4o")

        // 60px then 40px = 100px threshold
        state.onDrag(Offset(60f, 0f))
        assertEquals(AIProvider.OPENAI, state.selectedProvider)

        state.onDrag(Offset(40f, 0f))
        assertEquals(AIProvider.GEMINI, state.selectedProvider)
    }

    @Test
    fun `test provider switch resets vertical drag accumulation`() {
        val state = createTestState()
        state.onLongPressStart(initialProvider = AIProvider.OPENAI, initialModel = "gpt-4o")

        // Accumulate 50px downward drag (not enough to switch model yet, threshold is 80px)
        state.onDrag(Offset(0f, 50f))
        assertEquals("gpt-4o", state.selectedModel)

        // Trigger horizontal switch to GEMINI (resets vertical accumulator)
        state.onDrag(Offset(100f, 0f))
        assertEquals(AIProvider.GEMINI, state.selectedProvider)
        assertEquals("gemini-1.5-pro", state.selectedModel)

        // Dragging 40px down should not switch model because previous 50px was cleared (40 < 80)
        state.onDrag(Offset(0f, 40f))
        assertEquals("gemini-1.5-pro", state.selectedModel)

        // Another 40px down (total 80px since reset) triggers model switch
        state.onDrag(Offset(0f, 40f))
        assertEquals("gemini-1.5-flash", state.selectedModel)
    }

    @Test
    fun `test onRelease confirms switch with haptic feedback`() {
        val haptics = FakeHapticFeedback()
        var confirmedProvider: AIProvider? = null
        var confirmedModel: String? = null

        val state = createTestState(
            hapticFeedback = haptics,
            onConfirm = { p, m ->
                confirmedProvider = p
                confirmedModel = m
            }
        )

        state.onLongPressStart(initialProvider = AIProvider.OPENAI, initialModel = "gpt-4o")
        state.onDrag(Offset(100f, 0f)) // Switches to GEMINI
        state.onDrag(Offset(0f, 80f)) // Switches to flash

        state.onRelease()

        assertFalse(state.isLongPressing)
        assertEquals(AIProvider.GEMINI, confirmedProvider)
        assertEquals("gemini-1.5-flash", confirmedModel)
        assertTrue(HapticFeedbackType.Confirm in haptics.events)
    }

    @Test
    fun `test onCancel does not confirm switch`() {
        val haptics = FakeHapticFeedback()
        var confirmedProvider: AIProvider? = null

        val state = createTestState(
            hapticFeedback = haptics,
            onConfirm = { p, _ -> confirmedProvider = p }
        )

        state.onLongPressStart(initialProvider = AIProvider.OPENAI, initialModel = "gpt-4o")
        state.onDrag(Offset(100f, 0f)) // Moved to GEMINI

        state.onCancel()

        assertFalse(state.isLongPressing)
        assertNull(confirmedProvider)
        assertFalse(HapticFeedbackType.Confirm in haptics.events)
    }
}
