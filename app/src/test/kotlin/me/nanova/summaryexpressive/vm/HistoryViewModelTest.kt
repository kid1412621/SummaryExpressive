package me.nanova.summaryexpressive.vm

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.nanova.summaryexpressive.data.HistoryDao
import me.nanova.summaryexpressive.data.repository.HistoryRepository
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

    private class FakeHistoryDao : HistoryDao {
        val items = mutableListOf<HistorySummary>()

        override fun getSummaries(
            query: String,
            type: SummaryType?,
        ): PagingSource<Int, HistorySummary> {
            val filtered = items.filter { summary ->
                (type == null || summary.type == type) &&
                        (query.isBlank() || summary.title.contains(query, ignoreCase = true)
                                || summary.author.contains(query, ignoreCase = true)
                                || summary.summary.contains(query, ignoreCase = true))
            }
            return object : PagingSource<Int, HistorySummary>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HistorySummary> {
                    return LoadResult.Page(
                        data = filtered,
                        prevKey = null,
                        nextKey = null
                    )
                }

                override fun getRefreshKey(state: PagingState<Int, HistorySummary>): Int? = null
            }
        }

        override suspend fun insert(summary: HistorySummary) {
            items.removeAll { it.id == summary.id }
            items.add(summary)
        }

        override suspend fun deleteById(id: String) {
            items.removeAll { it.id == id }
        }
    }

    private lateinit var fakeDao: FakeHistoryDao
    private lateinit var historyRepository: HistoryRepository
    private lateinit var viewModel: HistoryViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeHistoryDao()
        historyRepository = HistoryRepository(fakeDao)
        viewModel = HistoryViewModel(historyRepository)
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
    fun `test onSearchTextChanged updates searchState`() {
        assertEquals("", viewModel.searchState.text.toString())

        viewModel.onSearchTextChanged("compose")
        assertEquals("compose", viewModel.searchState.text.toString())

        viewModel.onSearchTextChanged("kotlin multiplatform")
        assertEquals("kotlin multiplatform", viewModel.searchState.text.toString())
    }

    @Test
    fun `test addHistorySummary and removeHistorySummary delegates to repository`() =
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

            viewModel.addHistorySummary(summary1)
            viewModel.addHistorySummary(summary2)

            assertEquals(2, fakeDao.items.size)
            assertTrue(fakeDao.items.contains(summary1))
            assertTrue(fakeDao.items.contains(summary2))

            viewModel.removeHistorySummary("id-1")
            assertEquals(1, fakeDao.items.size)
            assertEquals("id-2", fakeDao.items.first().id)
        }
}
