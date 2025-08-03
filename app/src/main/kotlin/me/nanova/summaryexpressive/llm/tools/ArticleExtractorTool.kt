package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class Article(val url: String) : ToolArgs

class ArticleExtractorTool(private val client: HttpClient) : SimpleTool<Article>() {
    override val argsSerializer: KSerializer<Article>
        get() = Article.serializer()

    override val descriptor = ToolDescriptor(
        name = "extract_article_text_from_url",
        description = "Fetches the content of a web article from a given URL and extracts its main textual content.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "url",
                description = "The full URL of the web article to be parsed.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun doExecute(args: Article): String {
        return try {
            val article = extractTextFromArticleUrl(args.url)
            "Title: ${article.title}\nAuthor: ${article.author}\n\n${article.text}"
        } catch (e: Exception) {
            "Error extracting article: ${e.message}"
        }
    }

    private suspend fun fetchUrlContent(url: String): String {
        return client.get(url).bodyAsText()
    }

    private suspend fun extractTextFromArticleUrl(url: String): ExtractedArticle {
        val htmlContent = fetchUrlContent(url)
        return extractArticleContent(htmlContent, url)
    }
}

private data class ExtractedArticle(
    val text: String,
    val title: String,
    val author: String,
)

private fun extractArticleContent(htmlContent: String, sourceUrl: String): ExtractedArticle {
    // Paywall detection
    val paywallPattern =
        "\"(is|isAccessibleFor)Free\"\\s*:\\s*\"?false\"?".toRegex(RegexOption.IGNORE_CASE)
    if (paywallPattern.containsMatchIn(htmlContent)) {
        throw Exception("Paywall detected.")
    }

    val doc = Jsoup.parse(htmlContent, sourceUrl)

    // Extract title and author first from the original document
    val title = doc.title().ifBlank { sourceUrl }
    val author = doc.select("meta[name=author]").attr("content").ifBlank { "Article" }

    // Remove irrelevant elements before extracting the main content
    doc.select("header, footer, nav, aside, script, style").remove()

    // Find the main content element by trying selectors in order of priority
    val contentElement = doc.select("article").first()
        ?: doc.select("main").first()
        // For section and divs, look for the one with the most content
        ?: doc.select("section").maxByOrNull { it.text().length }
        ?: doc.select("#content, .content, #main, .main, #main-content, #article, .article, #post-body, .post-body")
            .maxByOrNull { it.text().length }

    // Get text from the found element, or fall back to the whole body
    val text = (contentElement?.text()?.ifBlank { null } ?: doc.body().text())
        .replace(Regex("\\s+"), " ").trim()

    if (text.isBlank()) {
        throw Exception("Could not extract text from URL.")
    }

    return ExtractedArticle(text, title, author)
}