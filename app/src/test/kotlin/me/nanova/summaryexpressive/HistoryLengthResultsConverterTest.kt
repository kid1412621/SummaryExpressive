package me.nanova.summaryexpressive

import me.nanova.summaryexpressive.data.converters.HistoryLengthResultsConverter
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.SummaryLength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HistoryLengthResultsConverterTest {

    private val converter = HistoryLengthResultsConverter()

    @Test
    fun `test serialization and deserialization of length results map`() {
        val map = mapOf(
            SummaryLength.SHORT to HistoryLengthResult(
                length = SummaryLength.SHORT,
                summary = "Short summary text",
                provider = "OPENAI",
                model = "gpt-4o-mini"
            ),
            SummaryLength.LONG to HistoryLengthResult(
                length = SummaryLength.LONG,
                summary = "Detailed long summary text",
                provider = "GEMINI",
                model = "gemini-1.5-pro"
            )
        )

        val json = converter.fromLengthResults(map)
        val result = converter.toLengthResults(json)

        assertEquals(map, result)
    }

    @Test
    fun `test null and blank handling`() {
        assertNull(converter.fromLengthResults(null))
        assertNull(converter.fromLengthResults(emptyMap()))
        assertNull(converter.toLengthResults(null))
        assertNull(converter.toLengthResults(""))
        assertNull(converter.toLengthResults("   "))
    }

    @Test
    fun `test invalid json handling`() {
        assertNull(converter.toLengthResults("{invalid_json}"))
    }
}
