package me.nanova.summaryexpressive.llm.tools

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArticleExtractorToolTest {

    @Test
    fun `extractTitle should prioritize og-title over title tag`() {
        val html = """
            <html>
            <head>
                <title>Medium</title>
                <meta property="og:title" content="Comprehensive Guide to Spec-Driven Development" />
            </head>
            <body><p>Some content</p></body>
            </html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://medium.com/@visrow/something")
        val title = extractTitle(doc, "https://medium.com/@visrow/something")
        assertEquals("Comprehensive Guide to Spec-Driven Development", title)
    }

    @Test
    fun `extractTitle should skip generic Medium title and find real title in multiple title tags`() {
        val html = """
            <html>
            <head>
                <title>Medium</title>
                <title>Comprehensive Guide to Spec-Driven Development | by Vishal Mysore | Medium</title>
            </head>
            <body><p>Some content</p></body>
            </html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://medium.com/@visrow/something")
        val title = extractTitle(doc, "https://medium.com/@visrow/something")
        assertEquals("Comprehensive Guide to Spec-Driven Development", title)
    }

    @Test
    fun `extractTitle should use h1 when title is generic Medium`() {
        val html = """
            <html>
            <head>
                <title>Medium</title>
            </head>
            <body>
                <article>
                    <h1>Java Just Got Its Biggest Upgrade in Decades</h1>
                    <p>Some content</p>
                </article>
            </body>
            </html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://cloudwithazeem.medium.com/java-upgrade")
        val title = extractTitle(doc, "https://cloudwithazeem.medium.com/java-upgrade")
        assertEquals("Java Just Got Its Biggest Upgrade in Decades", title)
    }

    @Test
    fun `extractTitle should fall back to URL slug for Medium article with hex post ID`() {
        val url = "https://medium.com/@visrow/comprehensive-guide-to-spec-driven-development-kiro-github-spec-kit-and-bmad-method-5d28ff61b9b1"
        val html = """
            <html>
            <head>
                <title>Medium</title>
            </head>
            <body><p>Some content</p></body>
            </html>
        """.trimIndent()
        val doc = Jsoup.parse(html, url)
        val title = extractTitle(doc, url)
        assertEquals(
            "Comprehensive Guide To Spec Driven Development Kiro Github Spec Kit And Bmad Method",
            title
        )
    }

    @Test
    fun `extractTitle should fall back to URL slug for Medium subdomain article`() {
        val url = "https://cloudwithazeem.medium.com/java-just-got-its-biggest-upgrade-in-decades-459fa70f991a"
        val html = """
            <html>
            <head>
                <title>Medium</title>
            </head>
            <body><p>Some content</p></body>
            </html>
        """.trimIndent()
        val doc = Jsoup.parse(html, url)
        val title = extractTitle(doc, url)
        assertEquals("Java Just Got Its Biggest Upgrade In Decades", title)
    }

    @Test
    fun `extractAuthor should extract from meta author, title suffix, and URL`() {
        // From meta
        val htmlWithMeta = """
            <html>
            <head>
                <meta name="author" content="Vishal Mysore" />
            </head>
            <body></body>
            </html>
        """.trimIndent()
        assertEquals(
            "Vishal Mysore",
            extractAuthor(Jsoup.parse(htmlWithMeta), "https://medium.com/@visrow/something")
        )

        // From title suffix
        val htmlWithTitleSuffix = """
            <html>
            <head>
                <title>Some Article | by Azeem | Medium</title>
            </head>
            <body></body>
            </html>
        """.trimIndent()
        assertEquals(
            "Azeem",
            extractAuthor(Jsoup.parse(htmlWithTitleSuffix), "https://medium.com/something")
        )

        // From URL @username
        val blankDoc = Jsoup.parse("<html><head></head><body></body></html>")
        assertEquals(
            "visrow",
            extractAuthor(blankDoc, "https://medium.com/@visrow/comprehensive-guide-5d28ff61b9b1")
        )

        // From URL subdomain
        assertEquals(
            "cloudwithazeem",
            extractAuthor(blankDoc, "https://cloudwithazeem.medium.com/java-upgrade-459fa70f991a")
        )
    }

    @Test
    fun `isValidTitle should filter out generic and Cloudflare block titles`() {
        assertFalse(isValidTitle("Medium"))
        assertFalse(isValidTitle("Just a moment..."))
        assertFalse(isValidTitle("Attention Required! | Cloudflare"))
        assertFalse(isValidTitle("Access Denied"))
        assertFalse(isValidTitle("403 Forbidden"))
        assertFalse(isValidTitle(""))
        assertFalse(isValidTitle(" "))

        assertTrue(isValidTitle("Comprehensive Guide to Spec-Driven Development"))
        assertTrue(isValidTitle("Java Just Got Its Biggest Upgrade in Decades"))
    }

    @Test
    fun `isAntiBotPage should identify Cloudflare challenge pages`() {
        assertTrue(isAntiBotPage("<html><head><title>Just a moment...</title></head></html>"))
        assertTrue(isAntiBotPage("<html><head><title>Attention Required! | Cloudflare</title></head></html>"))
        assertTrue(isAntiBotPage("<script src=\"/cdn-cgi/challenge-platform/h/g\"></script>"))
        assertTrue(isAntiBotPage("<h1>Sorry, you have been blocked</h1><p>Cloudflare</p>"))

        assertFalse(isAntiBotPage("<html><head><title>Real Article</title></head><body>Hello world</body></html>"))
    }

    @Test
    fun `parseReaderContent should extract title, author and clean markdown body`() {
        val readerBody = """
            Title: Modern Java Has Changed More Than You Think

            URL Source: https://cloudwithazeem.medium.com/java-just-got-its-biggest-upgrade-in-decades-459fa70f991a

            Published Time: 2026-07-28T13:03:37Z

            Markdown Content:
            ## _After diving deep into Java 26, I realised this isn’t just another release — it’s a complete shift in how modern Java applications are built._

            [![Image 1: Cloud With Azeem](https://miro.medium.com/v2/resize:fill:32:32/1*oJWwUx75Cf5oGoEfAefJpw.png)](https://cloudwithazeem.medium.com/)

            ![Image 2](https://miro.medium.com/v2/resize:fit:565/0*20rqPhzSR6bYjIwf)

            If someone had told me a few years ago that I’d be genuinely excited about a new Java release, I probably would’ve laughed.
        """.trimIndent()

        val extracted = parseReaderContent(
            readerBody,
            "https://cloudwithazeem.medium.com/java-just-got-its-biggest-upgrade-in-decades-459fa70f991a"
        )
        assertEquals("Modern Java Has Changed More Than You Think", extracted.title)
        assertEquals("Cloud With Azeem", extracted.author)
        assertTrue(extracted.content.contains("After diving deep into Java 26"))
        assertTrue(extracted.content.contains("If someone had told me a few years ago"))
        assertFalse(extracted.content.contains("miro.medium.com"))
    }

    @Test
    fun `parseReaderContent should fall back to URL author and slug if title or author missing in body`() {
        val readerBody = """
            Markdown Content:
            Some clean article body without explicit Title header.
        """.trimIndent()

        val extracted = parseReaderContent(
            readerBody,
            "https://medium.com/@visrow/comprehensive-guide-to-spec-driven-development-5d28ff61b9b1"
        )
        assertEquals("Comprehensive Guide To Spec Driven Development", extracted.title)
        assertEquals("visrow", extracted.author)
        assertEquals("Some clean article body without explicit Title header.", extracted.content)
    }

    @Test
    fun `isPublicWebUrl should identify valid public web urls`() {
        assertTrue(isPublicWebUrl("https://medium.com/@visrow/article"))
        assertTrue(isPublicWebUrl("http://example.com/post"))
        assertTrue(isPublicWebUrl("https://cloudwithazeem.medium.com/test"))

        assertFalse(isPublicWebUrl("http://localhost:8080/test"))
        assertFalse(isPublicWebUrl("http://127.0.0.1/test"))
        assertFalse(isPublicWebUrl("http://192.168.1.5/test"))
        assertFalse(isPublicWebUrl("ftp://medium.com"))
        assertFalse(isPublicWebUrl("file:///android_asset/test.html"))
    }
}
