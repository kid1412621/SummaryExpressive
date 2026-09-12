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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `test isBiliBiliLink detects bilibili subtype or url`() {
        val withSubtype = HistorySummary(
            title = "Bili Video",
            summary = "Summary",
            length = SummaryLength.SHORT,
            type = SummaryType.VIDEO,
            subtype = VideoSubtype.BILIBILI
        )
        assertEquals(true, withSubtype.isBiliBiliLink)

        val withUrl = HistorySummary(
            title = "Bili Video",
            summary = "Summary",
            length = SummaryLength.SHORT,
            type = SummaryType.VIDEO,
            sourceLink = "https://www.bilibili.com/video/BV1xx411c7mD"
        )
        assertEquals(true, withUrl.isBiliBiliLink)

        val withB23 = HistorySummary(
            title = "Bili Video",
            summary = "Summary",
            length = SummaryLength.SHORT,
            type = SummaryType.ARTICLE,
            sourceLink = "https://b23.tv/BV1xx411c7mD"
        )
        assertEquals(true, withB23.isBiliBiliLink)
    }

    @Test
    fun `test isYoutubeLink detects youtube subtype or url`() {
        val withSubtype = HistorySummary(
            title = "YT Video",
            summary = "Summary",
            length = SummaryLength.SHORT,
            type = SummaryType.VIDEO,
            subtype = VideoSubtype.YOUTUBE
        )
        assertEquals(true, withSubtype.isYoutubeLink)

        val withShortUrl = HistorySummary(
            title = "YT Video",
            summary = "Summary",
            length = SummaryLength.SHORT,
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
            assertEquals(VideoSubtype.YOUTUBE, VideoSubtype.fromUrl(url), "Expected $url to resolve to YOUTUBE")
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
            assertEquals(VideoSubtype.BILIBILI, VideoSubtype.fromUrl(url), "Expected $url to resolve to BILIBILI")
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
}

