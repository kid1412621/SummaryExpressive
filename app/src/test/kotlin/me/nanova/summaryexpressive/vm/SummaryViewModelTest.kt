package me.nanova.summaryexpressive.vm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sun.misc.Unsafe
import java.lang.reflect.Field

class SummaryViewModelTest {

    private fun createSummaryOutput(
        length: SummaryLength,
        summary: String = "Summary text for $length",
    ): SummaryOutput {
        return SummaryOutput(
            title = "Test Title",
            author = "Test Author",
            summary = summary,
            sourceLink = "https://example.com",
            isYoutubeLink = false,
            isBiliBiliLink = false,
            length = length,
        )
    }

    private fun createViewModel(): SummaryViewModel {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as Unsafe
        val viewModel = unsafe.allocateInstance(SummaryViewModel::class.java) as SummaryViewModel

        val stateFlow = MutableStateFlow(SummarizationState())
        val stateField: Field = SummaryViewModel::class.java.getDeclaredField("_summarizationState")
        stateField.isAccessible = true
        stateField.set(viewModel, stateFlow)

        val publicStateField: Field =
            SummaryViewModel::class.java.getDeclaredField("summarizationState")
        publicStateField.isAccessible = true
        publicStateField.set(viewModel, stateFlow.asStateFlow())

        return viewModel
    }

    @Suppress("UNCHECKED_CAST")
    private fun setInternalState(viewModel: SummaryViewModel, state: SummarizationState) {
        val field: Field = SummaryViewModel::class.java.getDeclaredField("_summarizationState")
        field.isAccessible = true
        val flow = field.get(viewModel) as MutableStateFlow<SummarizationState>
        flow.value = state
    }

    @Test
    fun `test switchLength returns early when no summaries generated yet`() {
        val viewModel = createViewModel()

        viewModel.switchLength(SummaryLength.SHORT)
        assertNull(viewModel.summarizationState.value.summaryResult)
        assertTrue(viewModel.summarizationState.value.lengthResults.isEmpty())
    }

    @Test
    fun `test switchLength switches to cached results when available`() {
        val viewModel = createViewModel()

        val shortOutput = createSummaryOutput(SummaryLength.SHORT, "Short summary")
        val longOutput = createSummaryOutput(SummaryLength.LONG, "Long summary")

        setInternalState(
            viewModel,
            SummarizationState(
                summaryResult = shortOutput,
                lengthResults = mapOf(
                    SummaryLength.SHORT to shortOutput,
                    SummaryLength.LONG to longOutput
                )
            )
        )

        // Switch to LONG
        viewModel.switchLength(SummaryLength.LONG)
        assertEquals(longOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to SHORT
        viewModel.switchLength(SummaryLength.SHORT)
        assertEquals(shortOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to MEDIUM (not yet generated)
        viewModel.switchLength(SummaryLength.MEDIUM)
        assertNull(viewModel.summarizationState.value.summaryResult)

        // Switch back to LONG
        viewModel.switchLength(SummaryLength.LONG)
        assertEquals(longOutput, viewModel.summarizationState.value.summaryResult)
    }

    @Test
    fun `test clearCurrentSummary clears all results and cache`() {
        val viewModel = createViewModel()

        val shortOutput = createSummaryOutput(SummaryLength.SHORT)
        setInternalState(
            viewModel,
            SummarizationState(
                summaryResult = shortOutput,
                lengthResults = mapOf(SummaryLength.SHORT to shortOutput)
            )
        )

        viewModel.clearCurrentSummary()
        assertNull(viewModel.summarizationState.value.summaryResult)
        assertTrue(viewModel.summarizationState.value.lengthResults.isEmpty())
    }
}
