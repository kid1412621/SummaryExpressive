package me.nanova.summaryexpressive

import me.nanova.summaryexpressive.data.local.database.mapper.toDomain
import me.nanova.summaryexpressive.data.local.database.mapper.toEntity
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class HistoryModelTest {

    @Test
    fun `test allLengthResults fallback when lengthResults is empty`() {
        val summary = HistorySummary(
            id = "1",
            title = "Test Title",
            author = "Test Author",
            summary = "Default Short Summary",
            length = SummaryLength.SHORT,
            type = SummaryType.ARTICLE,
            provider = "OPENAI",
            model = "gpt-4o",
            lengthResults = emptyMap()
        )

        val results = summary.allLengthResults
        assertEquals(1, results.size)
        val shortResult = results[SummaryLength.SHORT]
        assertNotNull(shortResult)
        assertEquals("Default Short Summary", shortResult?.summary)
        assertEquals("OPENAI", shortResult?.provider)
        assertEquals("gpt-4o", shortResult?.model)
    }

    @Test
    fun `test allLengthResults returns full map when lengthResults is populated`() {
        val lengthMap = mapOf(
            SummaryLength.SHORT to HistoryLengthResult(
                length = SummaryLength.SHORT,
                summary = "Short text",
                provider = "OPENAI",
                model = "gpt-4o-mini"
            ),
            SummaryLength.LONG to HistoryLengthResult(
                length = SummaryLength.LONG,
                summary = "Long text",
                provider = "GEMINI",
                model = "gemini-1.5-pro"
            )
        )

        val summary = HistorySummary(
            id = "1",
            title = "Test Title",
            author = "Test Author",
            summary = "Long text",
            length = SummaryLength.LONG,
            type = SummaryType.ARTICLE,
            provider = "GEMINI",
            model = "gemini-1.5-pro",
            lengthResults = lengthMap
        )

        val results = summary.allLengthResults
        assertEquals(2, results.size)
        assertEquals("Short text", results[SummaryLength.SHORT]?.summary)
        assertEquals("Long text", results[SummaryLength.LONG]?.summary)
    }

    @Test
    fun `test HistoryMapper preserves lengthResults both directions`() {
        val lengthMap = mapOf(
            SummaryLength.SHORT to HistoryLengthResult(
                length = SummaryLength.SHORT,
                summary = "Short summary",
                provider = "DEEPSEEK",
                model = "deepseek-chat"
            ),
            SummaryLength.MEDIUM to HistoryLengthResult(
                length = SummaryLength.MEDIUM,
                summary = "Medium summary",
                provider = "DEEPSEEK",
                model = "deepseek-chat"
            )
        )

        val domain = HistorySummary(
            id = "test-id",
            title = "Domain Title",
            author = "Domain Author",
            summary = "Medium summary",
            length = SummaryLength.MEDIUM,
            type = SummaryType.VIDEO,
            sourceLink = "https://youtube.com/watch?v=123",
            provider = "DEEPSEEK",
            model = "deepseek-chat",
            lengthResults = lengthMap
        )

        val entity = domain.toEntity()
        assertEquals(lengthMap, entity.lengthResults)
        assertEquals("test-id", entity.id)

        val roundTripDomain = entity.toDomain()
        assertEquals(domain.id, roundTripDomain.id)
        assertEquals(domain.lengthResults, roundTripDomain.lengthResults)
        assertEquals(domain.allLengthResults, roundTripDomain.allLengthResults)
    }
}
