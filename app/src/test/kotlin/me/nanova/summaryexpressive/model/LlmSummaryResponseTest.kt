package me.nanova.summaryexpressive.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LlmSummaryResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `test serialize and deserialize LlmSummaryResponse`() {
        val original = LlmSummaryResponse(
            title = "Test Title",
            author = "Test Author",
            overview = "Test Overview",
            keyPoints = listOf("Point 1", "Point 2"),
            bodySummary = "This is the body of the summary.",
            tags = listOf("AI", "Kotlin"),
            detectedLanguage = "en",
            errorReason = null
        )

        val serialized = json.encodeToString(LlmSummaryResponse.serializer(), original)
        val deserialized = json.decodeFromString(LlmSummaryResponse.serializer(), serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `test deserialize partial JSON with defaults`() {
        val jsonString = """
            {
                "title": "Partial Title",
                "bodySummary": "Minimal summary content"
            }
        """.trimIndent()

        val deserialized = json.decodeFromString(LlmSummaryResponse.serializer(), jsonString)

        assertEquals("Partial Title", deserialized.title)
        assertEquals("Minimal summary content", deserialized.bodySummary)
        assertNull(deserialized.author)
        assertNull(deserialized.overview)
        assertTrue(deserialized.keyPoints.isEmpty())
        assertTrue(deserialized.tags.isEmpty())
        assertNull(deserialized.detectedLanguage)
        assertNull(deserialized.errorReason)
    }
}
