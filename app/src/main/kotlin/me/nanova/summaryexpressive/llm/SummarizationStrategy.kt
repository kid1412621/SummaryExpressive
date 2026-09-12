package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.context.AIAgentLLMContext
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.exception.toSummaryException
import me.nanova.summaryexpressive.llm.tools.Article
import me.nanova.summaryexpressive.llm.tools.ArticleExtractorTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliSubtitleTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliVideo
import me.nanova.summaryexpressive.llm.tools.File
import me.nanova.summaryexpressive.llm.tools.FileExtractorTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscript
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.llm.tools.extractAuthorFromUrl
import me.nanova.summaryexpressive.llm.tools.extractTitleFromUrl
import me.nanova.summaryexpressive.llm.tools.isUnknownAuthor
import me.nanova.summaryexpressive.llm.tools.isValidTitle
import me.nanova.summaryexpressive.model.ExtractedContent
import me.nanova.summaryexpressive.model.LlmSummaryResponse
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.model.SummarySource

data class ExtractedPayload(
    val content: ExtractedContent,
    val sourceLink: String? = null,
    val isYoutube: Boolean = false,
    val isBiliBili: Boolean = false,
)

data class SummarizationResult(
    val summaryText: String,
    val structured: LlmSummaryResponse? = null,
)

fun createSummarizationStrategy(
    provider: AIProvider,
    summaryLength: SummaryLength,
    model: LLModel,
    articleExtractorTool: ArticleExtractorTool,
    youTubeTranscriptTool: YouTubeTranscriptTool,
    bilibiliSubtitleTool: BiliBiliSubtitleTool,
    fileExtractorTool: FileExtractorTool,
): AIAgentGraphStrategy<SummarySource, SummaryOutput> =
    strategy("summarization_router_strategy") {
        val contentExtractorSubgraph by subgraph<SummarySource, ExtractedPayload>("content_extractor") {
            val nodeExtractArticle by node<SummarySource.Article, ExtractedPayload>("extract_article") { source ->
                val content = try {
                    articleExtractorTool.execute(Article(source.url))
                } catch (e: Throwable) {
                    throw e.toSummaryException()
                }
                ExtractedPayload(content = content, sourceLink = source.url)
            }

            val nodeExtractYoutube by node<SummarySource.Video, ExtractedPayload>("extract_youtube") { source ->
                val content = try {
                    youTubeTranscriptTool.execute(YouTubeTranscript(source.url))
                } catch (e: Throwable) {
                    throw e.toSummaryException()
                }
                ExtractedPayload(content = content, sourceLink = source.url, isYoutube = true)
            }

            val nodeExtractBiliBili by node<SummarySource.Video, ExtractedPayload>("extract_bilibili") { source ->
                val content = try {
                    bilibiliSubtitleTool.execute(BiliBiliVideo(source.url))
                } catch (e: Throwable) {
                    throw e.toSummaryException()
                }
                ExtractedPayload(content = content, sourceLink = source.url, isBiliBili = true)
            }

            val nodeExtractFile by node<SummarySource.Document, ExtractedPayload>("extract_file") { source ->
                val content = try {
                    fileExtractorTool.execute(File(source.uri))
                } catch (e: Throwable) {
                    throw e.toSummaryException()
                }
                ExtractedPayload(content = content, sourceLink = source.uri)
            }

            val nodePreparePlainText by node<SummarySource.Text, ExtractedPayload>("prepare_plain_text") { source ->
                ExtractedPayload(content = ExtractedContent("Text Input", "", source.content))
            }

            edge(
                nodeStart forwardTo nodeExtractArticle
                        onCondition { it is SummarySource.Article }
                        transformed { it as SummarySource.Article }
            )
            edge(
                nodeStart forwardTo nodeExtractYoutube
                        onCondition {
                            it is SummarySource.Video && YouTubeTranscriptTool.isYouTubeLink(it.url)
                        }
                        transformed { it as SummarySource.Video }
            )
            edge(
                nodeStart forwardTo nodeExtractBiliBili
                        onCondition {
                            it is SummarySource.Video && BiliBiliSubtitleTool.isBiliBiliLink(it.url)
                        }
                        transformed { it as SummarySource.Video }
            )
            edge(
                nodeStart forwardTo nodeExtractFile
                        onCondition { it is SummarySource.Document }
                        transformed { it as SummarySource.Document }
            )
            edge(
                nodeStart forwardTo nodePreparePlainText
                        onCondition { it is SummarySource.Text }
                        transformed { it as SummarySource.Text }
            )

            edge(nodeExtractArticle forwardTo nodeFinish)
            edge(nodeExtractYoutube forwardTo nodeFinish)
            edge(nodeExtractBiliBili forwardTo nodeFinish)
            edge(nodeExtractFile forwardTo nodeFinish)
            edge(nodePreparePlainText forwardTo nodeFinish)
        }

        val nodeSummarize by node<ExtractedPayload, SummaryOutput>("summarize_extracted_text") { payload ->
            val textToSummarize = payload.content.content
            if (textToSummarize.isBlank()) {
                throw SummaryException.NoContentException()
            }

            val supportsStructured = model.supports(LLMCapability.Schema.JSON.Standard) ||
                    model.supports(LLMCapability.Schema.JSON.Basic)

            val result = if (supportsStructured) {
                requestStructuredSummary(llm, textToSummarize, model)
            } else {
                requestPlainTextSummary(llm, textToSummarize)
            }

            val summaryText = result.summaryText
            if (summaryText.isBlank() || summaryText.startsWith("Error:", ignoreCase = true)) {
                throw SummaryException.fromMessage(summaryText)
            }

            val structuredResult = result.structured
            val finalTitle = structuredResult?.title?.takeIf { isValidTitle(it) }
                ?: payload.content.title.takeIf {
                    isValidTitle(it) && !it.equals(
                        "Text Input",
                        ignoreCase = true
                    )
                }
                ?: payload.sourceLink?.let { extractTitleFromUrl(it) }?.takeIf { isValidTitle(it) }
                ?: payload.content.title.takeIf { it.isNotBlank() && !it.equals("Text Input", ignoreCase = true) }
                ?: ""

            val rawAuthor = structuredResult?.author?.takeIf { !isUnknownAuthor(it) }
                ?: payload.content.author.takeIf { !isUnknownAuthor(it) }
                ?: payload.sourceLink?.let { extractAuthorFromUrl(it) }
                    ?.takeIf { !isUnknownAuthor(it) }
            val finalAuthor = if (isUnknownAuthor(rawAuthor)) "" else rawAuthor?.trim().orEmpty()

            SummaryOutput(
                title = finalTitle,
                author = finalAuthor,
                summary = summaryText.trim(),
                sourceLink = payload.sourceLink,
                isYoutubeLink = payload.isYoutube,
                isBiliBiliLink = payload.isBiliBili,
                length = summaryLength,
                provider = provider.name,
                model = model.id,
                overview = structuredResult?.overview,
                keyPoints = structuredResult?.keyPoints ?: emptyList(),
                tags = structuredResult?.tags ?: emptyList(),
                detectedLanguage = structuredResult?.detectedLanguage,
                errorReason = structuredResult?.errorReason,
            )
        }

        edge(nodeStart forwardTo contentExtractorSubgraph)
        edge(contentExtractorSubgraph forwardTo nodeSummarize)
        edge(nodeSummarize forwardTo nodeFinish)
    }

private suspend fun requestStructuredSummary(
    llmContext: AIAgentLLMContext,
    textToSummarize: String,
    model: LLModel,
): SummarizationResult {
    val result = llmContext.writeSession {
        appendPrompt {
            user(textToSummarize)
        }
        requestLLMStructured<LlmSummaryResponse>(
            fixingParser = StructureFixingParser(model = model, retries = 2)
        )
    }
    val structured = result.getOrNull()?.data
    return if (structured != null && structured.bodySummary.isNotBlank()) {
        SummarizationResult(
            summaryText = structured.bodySummary,
            structured = structured,
        )
    } else {
        requestPlainTextSummary(llmContext, textToSummarize)
    }
}

private suspend fun requestPlainTextSummary(
    llmContext: AIAgentLLMContext,
    textToSummarize: String,
): SummarizationResult {
    val response = llmContext.writeSession {
        appendPrompt {
            user(textToSummarize)
        }
        requestLLMWithoutTools()
    }
    return SummarizationResult(summaryText = response.textContent())
}
