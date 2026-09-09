package me.nanova.summaryexpressive.vm

import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.domain.usecase.DeleteHistorySummaryUseCase
import me.nanova.summaryexpressive.domain.usecase.GetHistorySummariesUseCase
import me.nanova.summaryexpressive.domain.usecase.RestoreHistorySummaryUseCase
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeHistoryRepository : HistoryRepository {
        val items = mutableListOf<HistorySummary>()

        override fun getSummaries(
            query: String,
            type: SummaryType?,
        ): Flow<PagingData<HistorySummary>> = emptyFlow()

        override suspend fun addSummary(summary: HistorySummary) {
            items.removeAll { it.id == summary.id }
            items.add(summary)
        }

        override suspend fun deleteSummary(id: String) {
            items.removeAll { it.id == id }
        }
    }

    private lateinit var fakeRepository: FakeHistoryRepository
    private lateinit var viewModel: HistoryViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHistoryRepository()
        viewModel = HistoryViewModel(
            getHistorySummariesUseCase = GetHistorySummariesUseCase(fakeRepository),
            deleteHistorySummaryUseCase = DeleteHistorySummaryUseCase(fakeRepository),
            restoreHistorySummaryUseCase = RestoreHistorySummaryUseCase(fakeRepository),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test onFilterChanged toggles filter on and off`() {
        assertNull(viewModel.filterType.value)

        // Select VIDEO
        viewModel.onFilterChanged(SummaryType.VIDEO)
        assertEquals(SummaryType.VIDEO, viewModel.filterType.value)

        // Select VIDEO again -> toggles to null
        viewModel.onFilterChanged(SummaryType.VIDEO)
        assertNull(viewModel.filterType.value)

        // Select ARTICLE
        viewModel.onFilterChanged(SummaryType.ARTICLE)
        assertEquals(SummaryType.ARTICLE, viewModel.filterType.value)
    }

    @Test
    fun `test onSearchTextChanged updates searchQuery`() {
        assertEquals("", viewModel.searchQuery.value)

        viewModel.onSearchTextChanged("compose")
        assertEquals("compose", viewModel.searchQuery.value)

        viewModel.onSearchTextChanged("kotlin multiplatform")
        assertEquals("kotlin multiplatform", viewModel.searchQuery.value)
    }

    @Test
    fun `test restoreSummary and deleteSummary delegates to repository`() =
        runTest(testDispatcher) {
            val summary1 = HistorySummary(
                id = "id-1",
                title = "Article 1",
                author = "Author 1",
                summary = "Summary 1",
                length = SummaryLength.SHORT,
                type = SummaryType.ARTICLE,
                provider = "OPENAI"
            )
            val summary2 = HistorySummary(
                id = "id-2",
                title = "Video 2",
                author = "Author 2",
                summary = "Summary 2",
                length = SummaryLength.LONG,
                type = SummaryType.VIDEO,
                provider = "GEMINI"
            )

            viewModel.restoreSummary(summary1)
            viewModel.restoreSummary(summary2)

            assertEquals(2, fakeRepository.items.size)
            assertTrue(fakeRepository.items.contains(summary1))
            assertTrue(fakeRepository.items.contains(summary2))

            viewModel.deleteSummary("id-1")
            assertEquals(1, fakeRepository.items.size)
            assertEquals("id-2", fakeRepository.items.first().id)
        }
}
