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
    fun `test serialization and deserialization with overview, keyPoints, and tags`() {
        val map = mapOf(
            SummaryLength.MEDIUM to HistoryLengthResult(
                length = SummaryLength.MEDIUM,
                summary = "Full summary text here",
                provider = "OPENAI",
                model = "gpt-4o",
                overview = "Executive overview TL;DR",
                keyPoints = listOf("Point 1", "Point 2"),
                tags = listOf("AI", "Android")
            )
        )

        val json = converter.fromLengthResults(map)
        val result = converter.toLengthResults(json)

        assertEquals(map, result)
        assertEquals("Executive overview TL;DR", result?.get(SummaryLength.MEDIUM)?.overview)
        assertEquals(listOf("Point 1", "Point 2"), result?.get(SummaryLength.MEDIUM)?.keyPoints)
        assertEquals(listOf("AI", "Android"), result?.get(SummaryLength.MEDIUM)?.tags)
    }

    @Test
    fun `test serialization and deserialization with SummaryLength NONE`() {
        val map = mapOf(
            SummaryLength.NONE to HistoryLengthResult(
                length = SummaryLength.NONE,
                summary = "Full summary text without length constraint",
                provider = "OPENAI",
                model = "gpt-4.1",
                overview = "Natural overview",
                keyPoints = listOf("Point 1"),
                tags = listOf("AI")
            )
        )

        val json = converter.fromLengthResults(map)
        val result = converter.toLengthResults(json)

        assertEquals(map, result)
        assertEquals("Full summary text without length constraint", result?.get(SummaryLength.NONE)?.summary)
    }

    @Test
    fun `test backwards compatibility with legacy json missing new fields`() {
        val legacyJson = """
            {
                "SHORT": {
                    "length": "SHORT",
                    "summary": "Legacy summary text",
                    "provider": "OPENAI",
                    "model": "gpt-3.5-turbo"
                }
            }
        """.trimIndent()

        val result = converter.toLengthResults(legacyJson)
        val shortResult = result?.get(SummaryLength.SHORT)

        assertEquals("Legacy summary text", shortResult?.summary)
        assertNull(shortResult?.overview)
        assertEquals(emptyList<String>(), shortResult?.keyPoints)
        assertEquals(emptyList<String>(), shortResult?.tags)
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
