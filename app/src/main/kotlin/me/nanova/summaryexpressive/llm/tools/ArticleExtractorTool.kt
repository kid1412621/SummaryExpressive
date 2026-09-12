package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.model.ExtractedContent
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.Locale

@Serializable
data class Article(
    @property:LLMDescription("The full URL of the web article to be parsed.")
    val url: String,
)

class ArticleExtractorTool(private val client: HttpClient) : Tool<Article, ExtractedContent>(
    argsType = typeToken<Article>(),
    resultType = typeToken<ExtractedContent>(),
    name = "extract_article_text_from_url",
    description = "Fetches the content of a web article from a given URL and extracts its main textual content."
) {

    override suspend fun execute(args: Article): ExtractedContent {
        return extractTextFromArticleUrl(args.url)
    }

    private suspend fun extractTextFromArticleUrl(url: String): ExtractedContent {
        return try {
            val htmlContent = fetchUrlContent(url)
            extractArticleContent(htmlContent, url)
        } catch (e: Exception) {
            Log.d(
                "ArticleExtractorTool",
                "extractTextFromArticleUrl exception: $e, isPublic=${isPublicWebUrl(url)}, shouldFallback=${
                    shouldFallbackToReader(
                        e,
                        url
                    )
                }"
            )
            if (shouldFallbackToReader(e, url)) {
                fetchViaReader(url, fallbackError = e)
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchUrlContent(url: String): String {
        val response = client.get(url) {
            headers {
                append(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
                )
                append(
                    HttpHeaders.Accept,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
                )
                append(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
                append(
                    "Sec-Ch-Ua",
                    "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\""
                )
                append("Sec-Ch-Ua-Mobile", "?0")
                append("Sec-Ch-Ua-Platform", "\"Windows\"")
                append("Sec-Fetch-Dest", "document")
                append("Sec-Fetch-Mode", "navigate")
                append("Sec-Fetch-Site", "none")
                append("Sec-Fetch-User", "?1")
                append("Upgrade-Insecure-Requests", "1")
            }
        }

        if (response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.Unauthorized) {
            throw SummaryException.AccessDeniedException(statusCode = response.status.value)
        }
        if (response.status.value !in 200..299) {
            throw SummaryException.fromMessage("HTTP ${response.status.value}: ${response.status.description}")
        }

        val body = response.bodyAsText()
        if (isAntiBotPage(body)) {
            throw SummaryException.AccessDeniedException(
                detail = "Access blocked by anti-bot verification.",
                statusCode = 403
            )
        }
        return body
    }

    private fun shouldFallbackToReader(e: Throwable, url: String): Boolean {
        if (!isPublicWebUrl(url)) return false
        return when (e) {
            is SummaryException.AccessDeniedException -> true
            is SummaryException.NoContentException -> true
            is SummaryException.PaywallException -> true
            else -> {
                val message = e.message.orEmpty().lowercase(Locale.ROOT)
                message.contains("403") || message.contains("401") ||
                        message.contains("access denied") || message.contains("blocked") ||
                        message.contains("cloudflare")
            }
        }
    }

    private suspend fun fetchViaReader(
        url: String,
        fallbackError: Throwable? = null,
    ): ExtractedContent {
        return try {
            Log.d("ArticleExtractorTool", "Attempting fetchViaReader for $url")
            val readerUrl = "https://r.jina.ai/$url"
            val response = client.get(readerUrl) {
                headers {
                    append(HttpHeaders.UserAgent, "SummaryExpressive/1.0")
                    append("X-Return-Format", "markdown")
                }
            }
            Log.d("ArticleExtractorTool", "fetchViaReader status=${response.status.value}")

            if (response.status.value !in 200..299) {
                val errBody = runCatching { response.bodyAsText() }.getOrDefault("")
                Log.w(
                    "ArticleExtractorTool",
                    "fetchViaReader bad status: ${response.status.value}, body=$errBody"
                )
                throw fallbackError
                    ?: SummaryException.AccessDeniedException(statusCode = response.status.value)
            }

            val body = response.bodyAsText()
            Log.d("ArticleExtractorTool", "fetchViaReader body len=${body.length}")
            if (body.isBlank() || isAntiBotPage(body)) {
                Log.w("ArticleExtractorTool", "fetchViaReader empty or anti-bot body")
                throw fallbackError ?: SummaryException.NoContentException()
            }

            parseReaderContent(body, url, fallbackError)
        } catch (e: Exception) {
            Log.e("ArticleExtractorTool", "fetchViaReader exception: $e", e)
            if (e is SummaryException && e !is SummaryException.AccessDeniedException) throw e
            throw fallbackError ?: e
        }
    }

    internal fun extractArticleContent(htmlContent: String, sourceUrl: String): ExtractedContent {
        // Paywall detection
        val paywallPattern =
            "\"(is|isAccessibleFor)Free\"\\s*:\\s*\"?false\"?".toRegex(RegexOption.IGNORE_CASE)
        if (paywallPattern.containsMatchIn(htmlContent)) {
            throw SummaryException.PaywallException()
        }

        val doc = Jsoup.parse(htmlContent, sourceUrl)

        // Extract title and author first from the original document
        val title = extractTitle(doc, sourceUrl).ifBlank { sourceUrl }
        val author = extractAuthor(doc, sourceUrl).ifBlank { "Article" }

        // Remove irrelevant elements before extracting the main content
        doc.select("header, footer, nav, aside, script, style, noscript, svg").remove()

        // Find the main content element by trying selectors in order of priority
        val contentElement = doc.select("article").first()
            ?: doc.select("main").first()
            // For section and divs, look for the one with the most content
            ?: doc.select("section").maxByOrNull { it.text().length }
            ?: doc.select("#content, .content, #main, .main, #main-content, #article, .article, #post-body, .post-body, [class~=content]")
                .maxByOrNull { it.text().length }

        // Get text from the found element, or fall back to the whole body
        val text = (contentElement?.text()?.ifBlank { null } ?: doc.body().text())
            .replace(Regex("\\s+"), " ").trim()

        if (text.isBlank()) {
            throw SummaryException.NoContentException()
        }

        return ExtractedContent(title, author, text)
    }
}

internal fun extractTitle(doc: Document, sourceUrl: String): String {
    // 1. OpenGraph / Twitter meta tags
    val ogTitle = doc.select("meta[property=og:title], meta[name=og:title]").attr("content").trim()
    if (isValidTitle(ogTitle)) {
        return cleanTitle(ogTitle)
    }

    val twitterTitle =
        doc.select("meta[name=twitter:title], meta[property=twitter:title]").attr("content").trim()
    if (isValidTitle(twitterTitle)) {
        return cleanTitle(twitterTitle)
    }

    // 2. Heading (h1)
    val h1 = doc.select("article h1, main h1, h1").firstOrNull()?.text()?.trim()
    if (!h1.isNullOrBlank() && isValidTitle(h1)) {
        return cleanTitle(h1)
    }

    // 3. Scan all <title> tags in document (e.g. Medium has multiple <title> elements)
    for (el in doc.select("title")) {
        val titleText = el.text().trim()
        if (isValidTitle(titleText)) {
            return cleanTitle(titleText)
        }
    }

    // 4. URL slug fallback (e.g. Medium, Substack, Dev.to)
    val urlTitle = extractTitleFromUrl(sourceUrl)
    if (isValidTitle(urlTitle)) {
        return urlTitle
    }

    // 5. Default title fallback
    val defaultTitle = doc.title().trim()
    return if (isValidTitle(defaultTitle)) cleanTitle(defaultTitle) else ""
}

internal fun cleanTitle(title: String): String {
    return title
        .replace(Regex("\\s*\\|\\s*by\\s+.*\\|\\s*Medium$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*[|·—–-]\\s*Medium$", RegexOption.IGNORE_CASE), "")
        .trim()
}

internal fun isValidTitle(title: String?): Boolean {
    if (title.isNullOrBlank()) return false
    val trimmed = title.trim()
    if (trimmed.length < 2) return false
    val lower = trimmed.lowercase(Locale.ROOT)
    val genericTitles = setOf(
        "medium", "article", "untitled", "text input", "home",
        "just a moment...", "attention required! | cloudflare",
        "attention required!", "cloudflare", "access denied",
        "403 forbidden", "forbidden", "security check", "robot or human?",
        "sorry, you have been blocked", "blocked"
    )
    if (lower in genericTitles) return false
    if (lower.startsWith("just a moment") || lower.startsWith("attention required")) return false
    return true
}

internal fun extractTitleFromUrl(url: String): String {
    return runCatching {
        val uri = URI(url)
        val path = uri.path ?: return ""
        val lastSegment = path.split('/').lastOrNull { it.isNotBlank() } ?: return ""

        // Strip trailing hex/alphanumeric post ID (must contain digits, e.g. -5d28ff61b9b1 or -459fa70f991a)
        val slug = lastSegment
            .replace(Regex("-(?=.*\\d)[a-zA-Z0-9]{8,16}$", RegexOption.IGNORE_CASE), "")

        if (slug.isBlank() || (slug == lastSegment && lastSegment.length <= 16 && lastSegment.matches(
                Regex("^[a-f0-9]+$")
            ))
        ) {
            return ""
        }

        val words = slug.split('-', '_').filter { it.isNotBlank() }
        if (words.isEmpty()) return ""

        words.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }.getOrDefault("")
}

internal fun extractAuthor(doc: Document, sourceUrl: String): String {
    // 1. Meta tags
    val metaAuthor = doc.select(
        "meta[name=author], meta[property=article:author], meta[name=twitter:creator], meta[property=author]"
    ).firstOrNull { it.attr("content").isNotBlank() }?.attr("content")?.trim()

    if (!metaAuthor.isNullOrBlank() && !isUnknownAuthor(metaAuthor)) {
        return metaAuthor
    }

    // 2. Medium title tag pattern: "| by <Author> | Medium"
    for (el in doc.select("title")) {
        val match =
            Regex("\\|\\s*by\\s+([^|]+)\\s*\\|\\s*Medium", RegexOption.IGNORE_CASE).find(el.text())
        if (match != null) {
            val author = match.groupValues[1].trim()
            if (author.isNotBlank() && !isUnknownAuthor(author)) {
                return author
            }
        }
    }

    // 3. Fallback from URL
    val urlAuthor = extractAuthorFromUrl(sourceUrl)
    if (urlAuthor.isNotBlank() && !isUnknownAuthor(urlAuthor)) {
        return urlAuthor
    }

    return "Article"
}

internal fun extractAuthorFromUrl(url: String): String {
    return runCatching {
        val uri = URI(url)
        val host = uri.host?.removePrefix("www.")?.lowercase(Locale.ROOT) ?: ""
        val path = uri.path ?: ""

        val userMatch = Regex("/@([a-zA-Z0-9_.-]+)").find(path)
        if (userMatch != null) {
            return userMatch.groupValues[1]
        }

        if (host.endsWith(".medium.com") && host != "medium.com") {
            val sub = host.removeSuffix(".medium.com")
            if (sub.isNotBlank() && sub !in setOf(
                    "api",
                    "cdn-images-1",
                    "link",
                    "status",
                    "about"
                )
            ) {
                return sub
            }
        }
        ""
    }.getOrDefault("")
}

internal fun isUnknownAuthor(author: String?): Boolean {
    if (author.isNullOrBlank()) return true
    val trimmed = author.trim().lowercase(Locale.ROOT)
    return trimmed in setOf("unknown", "unknown author", "n/a", "none", "article")
}

internal fun isAntiBotPage(html: String): Boolean {
    val lower = html.lowercase(Locale.ROOT)
    return (lower.contains("<title>just a moment...</title>") ||
            lower.contains("<title>attention required! | cloudflare</title>") ||
            lower.contains("challenge-platform") ||
            (lower.contains("cloudflare") && lower.contains("ray id") && lower.contains("verify you are human")) ||
            lower.contains("sorry, you have been blocked"))
}

internal fun parseReaderContent(
    body: String,
    sourceUrl: String,
    fallbackError: Throwable? = null,
): ExtractedContent {
    // 1. Title
    val rawTitle = Regex("^Title:\\s*(.+)$", RegexOption.MULTILINE)
        .find(body)?.groupValues?.get(1)?.trim()
    val title = when {
        isValidTitle(rawTitle) -> cleanTitle(rawTitle!!)
        else -> extractTitleFromUrl(sourceUrl).ifBlank { sourceUrl }
    }

    // 2. Author
    val authorFromAlt = Regex("\\[!\\[Image \\d+:\\s*([^]]+)]")
        .find(body)?.groupValues?.get(1)?.trim()
    val author = when {
        !authorFromAlt.isNullOrBlank() && !isUnknownAuthor(authorFromAlt) -> authorFromAlt
        else -> extractAuthorFromUrl(sourceUrl).ifBlank { "Article" }
    }

    // 3. Content
    val rawContent = if (body.contains("Markdown Content:")) {
        body.substringAfter("Markdown Content:").trim()
    } else {
        body.trim()
    }

    // Remove pure Markdown image references (e.g. ![...](...) or [![...](...)...)
    val cleanedContent = rawContent
        .replace(Regex("\\[?!\\[[^]]*]\\([^)]*\\)]?(\\([^)]*\\))?"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    if (cleanedContent.isBlank()) {
        throw fallbackError ?: SummaryException.NoContentException()
    }

    return ExtractedContent(title, author, cleanedContent)
}

internal fun isPublicWebUrl(url: String): Boolean {
    val lower = url.lowercase(Locale.ROOT)
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
    val host = runCatching { URI(url).host?.lowercase(Locale.ROOT) }.getOrNull() ?: return false
    return !(host == "localhost" || host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith(
        "10."
    ) || host.endsWith(".local"))
}
