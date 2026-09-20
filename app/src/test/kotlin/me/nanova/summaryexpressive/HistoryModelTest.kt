package me.nanova.summaryexpressive

import me.nanova.summaryexpressive.data.local.database.mapper.toDomain
import me.nanova.summaryexpressive.data.local.database.mapper.toEntity
import me.nanova.summaryexpressive.model.HistoryLengthResult
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import me.nanova.summaryexpressive.model.isBiliBiliLink
import me.nanova.summaryexpressive.model.isYouTubeLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryModelTest {

    @Test
    fun `test computed getters return defaults when lengthResults is empty`() {
        val summary = HistorySummary(
            id = "1",
            title = "Test Title",
            author = "Test Author",
            type = SummaryType.ARTICLE,
            lengthResults = emptyMap()
        )

        assertEquals("", summary.summary)
        assertNull(summary.overview)
        assertTrue(summary.keyPoints.isEmpty())
        assertTrue(summary.tags.isEmpty())
        assertNull(summary.provider)
        assertNull(summary.model)
        assertTrue(summary.allLengthResults.isEmpty())
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
            length = SummaryLength.LONG,
            type = SummaryType.ARTICLE,
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
            length = SummaryLength.MEDIUM,
            type = SummaryType.VIDEO,
            sourceLink = "https://youtube.com/watch?v=123",
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

    @Test
    fun `test isBiliBiliLink detects bilibili subtype or url`() {
        val withSubtype = HistorySummary(
            title = "Bili Video",
            type = SummaryType.VIDEO,
            subtype = VideoSubtype.BILIBILI
        )
        assertEquals(true, withSubtype.isBiliBiliLink)

        val withUrl = HistorySummary(
            title = "Bili Video",
            type = SummaryType.VIDEO,
            sourceLink = "https://www.bilibili.com/video/BV1xx411c7mD"
        )
        assertEquals(true, withUrl.isBiliBiliLink)

        val withB23 = HistorySummary(
            title = "Bili Video",
            type = SummaryType.ARTICLE,
            sourceLink = "https://b23.tv/BV1xx411c7mD"
        )
        assertEquals(true, withB23.isBiliBiliLink)
    }

    @Test
    fun `test isYoutubeLink detects youtube subtype or url`() {
        val withSubtype = HistorySummary(
            title = "YT Video",
            type = SummaryType.VIDEO,
            subtype = VideoSubtype.YOUTUBE
        )
        assertEquals(true, withSubtype.isYoutubeLink)

        val withShortUrl = HistorySummary(
            title = "YT Video",
            type = SummaryType.VIDEO,
            sourceLink = "https://youtu.be/dQw4w9WgXcQ"
        )
        assertEquals(true, withShortUrl.isYoutubeLink)
    }

    @Test
    fun `test VideoSubtype URL detection and classification`() {
        // YouTube positive cases
        val ytUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "http://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "youtube.com/watch?v=dQw4w9WgXcQ",
            "youtu.be/dQw4w9WgXcQ",
            "Check this out: https://youtu.be/dQw4w9WgXcQ it's great!",
            "【YouTube】https://www.youtube.com/watch?v=123"
        )
        for (url in ytUrls) {
            assertTrue(isYouTubeLink(url), "Expected $url to be detected as YouTube")
            assertFalse(isBiliBiliLink(url), "Expected $url not to be detected as BiliBili")
            assertEquals(
                VideoSubtype.YOUTUBE,
                VideoSubtype.fromUrl(url),
                "Expected $url to resolve to YOUTUBE"
            )
        }

        // BiliBili positive cases
        val biliUrls = listOf(
            "https://www.bilibili.com/video/BV1xx411c7mD",
            "http://bilibili.com/video/BV1xx411c7mD",
            "https://b23.tv/BV1xx411c7mD",
            "https://b23.ms/BV1xx411c7mD",
            "https://live.bilibili.com/123456",
            "bilibili.com/video/BV1xx411c7mD",
            "b23.tv/BV1xx411c7mD",
            "【某某视频】https://b23.tv/BV1xx411c7mD 点击查看",
            "来看这个：https://www.bilibili.com/video/BV1xx411c7mD"
        )
        for (url in biliUrls) {
            assertTrue(isBiliBiliLink(url), "Expected $url to be detected as BiliBili")
            assertFalse(isYouTubeLink(url), "Expected $url not to be detected as YouTube")
            assertEquals(
                VideoSubtype.BILIBILI,
                VideoSubtype.fromUrl(url),
                "Expected $url to resolve to BILIBILI"
            )
        }

        // Negative cases (articles, lookalikes, false domains)
        val negativeUrls = listOf(
            "https://notyoutube.com/watch?v=123",
            "https://example.com/youtube.com",
            "https://notbilibili.com/video",
            "https://example.com/b23.tv",
            "https://medium.com/@visrow/some-article-title",
            "https://cloudwithazeem.medium.com/java-article",
            "https://github.com/google/material-design-icons",
            "Just some plain text without any url",
            "",
            null
        )
        for (url in negativeUrls) {
            assertFalse(isYouTubeLink(url), "Expected '$url' not to be YouTube")
            assertFalse(isBiliBiliLink(url), "Expected '$url' not to be BiliBili")
            assertNull(VideoSubtype.fromUrl(url), "Expected '$url' to resolve to null")
        }
    }

    @Test
    fun `test HistorySummary with SummaryLength NONE has no length and delegates properties`() {
        val summary = HistorySummary(
            id = "none-id",
            title = "No Length Title",
            author = "Author",
            length = SummaryLength.NONE,
            type = SummaryType.ARTICLE,
            lengthResults = mapOf(
                SummaryLength.NONE to HistoryLengthResult(
                    length = SummaryLength.NONE,
                    summary = "Summary generated without length constraints",
                    provider = "OPENAI",
                    model = "gpt-4.1",
                    overview = "Natural Overview",
                    keyPoints = listOf("Key Point 1"),
                    tags = listOf("Tech")
                )
            )
        )

        assertFalse(summary.hasLength)
        assertEquals(SummaryLength.NONE, summary.length)
        assertEquals("Summary generated without length constraints", summary.summary)
        assertEquals("Natural Overview", summary.overview)
        assertEquals(listOf("Key Point 1"), summary.keyPoints)
        assertEquals(listOf("Tech"), summary.tags)
        assertEquals("OPENAI", summary.provider)
        assertEquals("gpt-4.1", summary.model)
    }

    @Test
    fun `test HistorySummary resolves getters dynamically based on active length`() {
        val shortResult = HistoryLengthResult(
            length = SummaryLength.SHORT,
            summary = "Short text",
            provider = "OPENAI",
            model = "gpt-4o-mini",
            overview = "Short Overview"
        )
        val longResult = HistoryLengthResult(
            length = SummaryLength.LONG,
            summary = "Long text",
            provider = "GEMINI",
            model = "gemini-2.0-flash",
            overview = "Long Overview"
        )
        val multiSummary = HistorySummary(
            id = "multi-id",
            title = "Multi Length Title",
            length = SummaryLength.SHORT,
            type = SummaryType.ARTICLE,
            lengthResults = mapOf(
                SummaryLength.SHORT to shortResult,
                SummaryLength.LONG to longResult
            )
        )

        assertEquals("Short text", multiSummary.summary)
        assertEquals("Short Overview", multiSummary.overview)
        assertEquals("OPENAI", multiSummary.provider)
        assertEquals("gpt-4o-mini", multiSummary.model)

        val switched = multiSummary.copy(length = SummaryLength.LONG)
        assertEquals("Long text", switched.summary)
        assertEquals("Long Overview", switched.overview)
        assertEquals("GEMINI", switched.provider)
        assertEquals("gemini-2.0-flash", switched.model)
    }

    @Test
    fun `test HistoryMapper with SummaryLength NONE round-trips cleanly`() {
        val domain = HistorySummary(
            id = "none-test-id",
            title = "No Length Domain",
            author = "Author",
            length = SummaryLength.NONE,
            type = SummaryType.TEXT,
            lengthResults = mapOf(
                SummaryLength.NONE to HistoryLengthResult(
                    length = SummaryLength.NONE,
                    summary = "Natural summary without length",
                    provider = "OPENAI",
                    model = "gpt-4.1",
                    overview = "Overview",
                    keyPoints = listOf("KP1"),
                    tags = listOf("Tag1")
                )
            )
        )

        val entity = domain.toEntity()
        assertEquals(SummaryLength.NONE, entity.length)
        assertEquals("Natural summary without length", entity.summary)
        assertEquals("OPENAI", entity.provider)
        assertEquals("gpt-4.1", entity.model)

        val roundTrip = entity.toDomain()
        assertEquals(domain.id, roundTrip.id)
        assertEquals(SummaryLength.NONE, roundTrip.length)
        assertFalse(roundTrip.hasLength)
        assertEquals(domain.summary, roundTrip.summary)
        assertEquals(domain.overview, roundTrip.overview)
        assertEquals(domain.keyPoints, roundTrip.keyPoints)
        assertEquals(domain.tags, roundTrip.tags)
    }
}

