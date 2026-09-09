package me.nanova.summaryexpressive.vm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.nanova.summaryexpressive.domain.usecase.SummarizeContentUseCase
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeSummarizeContentUseCase : SummarizeContentUseCase {
        var summaryToReturn: SummaryOutput? = null
        var errorToThrow: Throwable? = null
        var lastInput: String? = null
        var lastOverrideLength: SummaryLength? = null

        override suspend operator fun invoke(
            text: String,
            overrideLength: SummaryLength?,
        ): Result<SummaryOutput> {
            lastInput = text
            lastOverrideLength = overrideLength
            errorToThrow?.let { return Result.failure(it) }
            val length = overrideLength ?: SummaryLength.MEDIUM
            val output = summaryToReturn ?: createSummaryOutput(length)
            return Result.success(output)
        }
    }

    private lateinit var fakeUseCase: FakeSummarizeContentUseCase
    private lateinit var viewModel: SummaryViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUseCase = FakeSummarizeContentUseCase()
        viewModel = SummaryViewModel(fakeUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test summarize success updates state and caches result`() = runTest(testDispatcher) {
        val output = createSummaryOutput(SummaryLength.MEDIUM, "Medium summary")
        fakeUseCase.summaryToReturn = output

        viewModel.summarize("https://example.com/article")

        assertEquals("https://example.com/article", fakeUseCase.lastInput)
        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(output, state.summaryResult)
        assertEquals(output, state.lengthResults[SummaryLength.MEDIUM])
    }

    @Test
    fun `test summarize failure updates error state`() = runTest(testDispatcher) {
        fakeUseCase.errorToThrow = SummaryException.NoKeyException()

        viewModel.summarize("https://example.com")

        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.summaryResult)
        assertTrue(state.error is SummaryException.NoKeyException)
    }

    @Test
    fun `test switchLength returns early when no summaries generated yet`() {
        viewModel.switchLength(SummaryLength.SHORT)
        assertNull(viewModel.summarizationState.value.summaryResult)
        assertTrue(viewModel.summarizationState.value.lengthResults.isEmpty())
    }

    @Test
    fun `test switchLength switches between cached results`() = runTest(testDispatcher) {
        val shortOutput = createSummaryOutput(SummaryLength.SHORT, "Short summary")
        val longOutput = createSummaryOutput(SummaryLength.LONG, "Long summary")

        fakeUseCase.summaryToReturn = shortOutput
        viewModel.summarize("https://example.com", SummaryLength.SHORT)

        fakeUseCase.summaryToReturn = longOutput
        viewModel.summarize("https://example.com", SummaryLength.LONG)

        // Switch to SHORT
        viewModel.switchLength(SummaryLength.SHORT)
        assertEquals(shortOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to LONG
        viewModel.switchLength(SummaryLength.LONG)
        assertEquals(longOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to MEDIUM (not yet cached)
        viewModel.switchLength(SummaryLength.MEDIUM)
        assertNull(viewModel.summarizationState.value.summaryResult)
    }

    @Test
    fun `test clearCurrentSummary clears all results and cache`() = runTest(testDispatcher) {
        fakeUseCase.summaryToReturn = createSummaryOutput(SummaryLength.SHORT)
        viewModel.summarize("https://example.com", SummaryLength.SHORT)

        assertNotNull(viewModel.summarizationState.value.summaryResult)

        viewModel.clearCurrentSummary()
        assertNull(viewModel.summarizationState.value.summaryResult)
        assertNull(viewModel.summarizationState.value.error)
        assertTrue(viewModel.summarizationState.value.lengthResults.isEmpty())
    }

    companion object {
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
    }
}
