package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteSingleTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekClientSettings
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import android.content.Context
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.tools.Article
import me.nanova.summaryexpressive.llm.tools.ArticleExtractorTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliSubtitleTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliVideo
import me.nanova.summaryexpressive.llm.tools.File
import me.nanova.summaryexpressive.llm.tools.FileExtractorTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscript
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.model.ExtractedContent
import me.nanova.summaryexpressive.model.SummaryData
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.vm.SummaryViewModel.SummarySource

@Serializable
data class SummaryOutput(
    override val title: String,
    override val author: String,
    override val summary: String,
    val sourceLink: String? = null,
    // TODO: just use SummarySource
    val isYoutubeLink: Boolean,
    val isBiliBiliLink: Boolean,
    val length: SummaryLength,
) : SummaryData

class LLMHandler(context: Context, httpClient: HttpClient) {
    private val userPreferencesRepository = UserPreferencesRepository(context)
    private val fileExtractorTool: FileExtractorTool = FileExtractorTool(context)
    private val articleExtractorTool = ArticleExtractorTool(httpClient)
    private val youTubeTranscriptTool = YouTubeTranscriptTool(httpClient)
    private val bilibiliSubtitleTool = BiliBiliSubtitleTool(httpClient, userPreferencesRepository)

    fun getSummarizationAgent(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String? = null,
        model: String? = null,
        summaryLength: SummaryLength = SummaryLength.MEDIUM,
        useOriginalLanguage: Boolean,
        language: String,
    ): AIAgent<SummarySource, SummaryOutput> {
        val executor = createExecutor(provider, apiKey, baseUrl)

        val articleToolRegistry = ToolRegistry { tool(articleExtractorTool) }
        val youtubeToolRegistry = ToolRegistry { tool(youTubeTranscriptTool) }
        val bilibiliToolRegistry = ToolRegistry { tool(bilibiliSubtitleTool) }
        val fileToolRegistry = ToolRegistry { tool(fileExtractorTool) }
        val tools =
            articleToolRegistry + youtubeToolRegistry + bilibiliToolRegistry + fileToolRegistry

        val agentConfig =
            createAgentConfig(provider, model, summaryLength, useOriginalLanguage, language)

        return AIAgent(
            promptExecutor = executor,
            strategy = createSummarizationStrategy(
                articleExtractorTool,
                youTubeTranscriptTool,
                fileExtractorTool,
                summaryLength
            ),
            agentConfig = agentConfig,
            toolRegistry = tools
        )
    }

    private fun createExecutor(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
    ): PromptExecutor {
        return when (provider) {
            AIProvider.OPENAI -> createOpenAIExecutor(apiKey, baseUrl)
            AIProvider.GEMINI -> createGeminiExecutor(apiKey, baseUrl)
            AIProvider.CLAUDE -> createClaudExecutor(apiKey, baseUrl)
            AIProvider.DEEPSEEK -> createDeepSeekExecutor(apiKey, baseUrl)
        }
    }

    private fun createAgentConfig(
        provider: AIProvider,
        modelName: String?,
        summaryLength: SummaryLength,
        useOriginalLanguage: Boolean,
        language: String,
    ): AIAgentConfig {
        val llmModel = when (provider) {
            AIProvider.OPENAI -> modelName?.let { name -> provider.models.find { it.id == name } }
                ?: OpenAIModels.CostOptimized.GPT4oMini

            AIProvider.GEMINI -> modelName?.let { name -> provider.models.find { it.id == name } }
                ?: GoogleModels.Gemini2_5Flash

            AIProvider.CLAUDE -> modelName?.let { name -> provider.models.find { it.id == name } }
                ?: AnthropicModels.Sonnet_3_5

            AIProvider.DEEPSEEK -> modelName?.let { name -> provider.models.find { it.id == name } }
                ?: DeepSeekModels.DeepSeekChat
        }

        return AIAgentConfig(
            prompt = createSummarizationPrompt(summaryLength, useOriginalLanguage, language),
            model = llmModel,
            maxAgentIterations = 10,
        )
    }

    private fun createOpenAIExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { OpenAILLMClient(apiKey, settings = OpenAIClientSettings(baseUrl = it)) }
            ?: OpenAILLMClient(apiKey)

        return SingleLLMPromptExecutor(client)
    }

    private fun createGeminiExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { GoogleLLMClient(apiKey, settings = GoogleClientSettings(baseUrl = it)) }
            ?: GoogleLLMClient(apiKey)

        return SingleLLMPromptExecutor(client)
    }

    private fun createClaudExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { AnthropicLLMClient(apiKey, settings = AnthropicClientSettings(baseUrl = it)) }
            ?: AnthropicLLMClient(apiKey)

        return SingleLLMPromptExecutor(client)
    }

    private fun createDeepSeekExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { DeepSeekLLMClient(apiKey, settings = DeepSeekClientSettings(baseUrl = it)) }
            ?: DeepSeekLLMClient(apiKey)

        return SingleLLMPromptExecutor(client)
    }

    private fun createSummarizationStrategy(
        articleExtractorTool: ArticleExtractorTool,
        youTubeTranscriptTool: YouTubeTranscriptTool,
        fileExtractorTool: FileExtractorTool,
        summaryLength: SummaryLength,
    ): AIAgentGraphStrategy<SummarySource, SummaryOutput> =
        strategy("summarization_router_strategy")
        {
            var extractedContent: ExtractedContent? = null
            var isYoutube = false
            var isBiliBili = false
            var sourceLink = ""

            val contentExtractorSubgraph by subgraph<SummarySource, String>("content_extractor") {
                val nodeExtractArticle by nodeExecuteSingleTool(tool = articleExtractorTool)
                val nodeExtractYoutube by nodeExecuteSingleTool(tool = youTubeTranscriptTool)
                val nodeExtractBiliBili by nodeExecuteSingleTool(tool = bilibiliSubtitleTool)
                val nodeExtractFile by nodeExecuteSingleTool(tool = fileExtractorTool)

                val nodePreparePlainText by node<String, ExtractedContent>("prepare_plain_text") { text ->
                    ExtractedContent("Text Input", "Unknown", text)
                }

                val nodePreLLMRequest by node<ExtractedContent, String>("trigger_summary_from_tool") {
                    extractedContent = it
                    "Content has been extracted. Summarize it based on the instructions."
                }

                val processToolResult = { it: SafeTool.Result<out ExtractedContent> ->
                    when (it) {
                        is SafeTool.Result.Success -> it.result
                        is SafeTool.Result.Failure -> throw SummaryException.fromMessage(it.message)
                    }
                }

                edge(
                    nodeStart forwardTo nodeExtractArticle
                            onCondition { it is SummarySource.Article }
                            transformed { source ->
                                val url = (source as SummarySource.Article).url
                                sourceLink = url
                                Article(url)
                            }
                )
                edge(
                    nodeStart forwardTo nodeExtractYoutube
                            onCondition { it is SummarySource.Video && YouTubeTranscriptTool.isYouTubeLink((it).url) }
                            transformed { source ->
                                val url = (source as SummarySource.Video).url
                                isYoutube = true
                                sourceLink = url
                                YouTubeTranscript(url)
                            }
                )
                edge(
                    nodeStart forwardTo nodeExtractBiliBili
                            onCondition { it is SummarySource.Video && BiliBiliSubtitleTool.isBiliBiliLink((it).url) }
                            transformed { source ->
                                val url = (source as SummarySource.Video).url
                                isBiliBili = true
                                sourceLink = url
                                BiliBiliVideo(url)
                            }
                )
                edge(
                    nodeStart forwardTo nodeExtractFile
                            onCondition { it is SummarySource.Document }
                            transformed { source -> File((source as SummarySource.Document).uri) }
                )
                edge(
                    nodeStart forwardTo nodePreparePlainText
                            onCondition { it is SummarySource.Text }
                            transformed { (it as SummarySource.Text).content })

                // Tool-based paths
                edge(nodeExtractArticle forwardTo nodePreLLMRequest transformed { processToolResult(it) })
                edge(nodeExtractYoutube forwardTo nodePreLLMRequest transformed { processToolResult(it) })
                edge(nodeExtractBiliBili forwardTo nodePreLLMRequest transformed { processToolResult(it) })
                edge(nodeExtractFile forwardTo nodePreLLMRequest transformed { processToolResult(it) })
                edge(nodePreLLMRequest forwardTo nodeFinish)

                // Plain text path
                edge(nodePreparePlainText forwardTo nodeFinish transformed {
                    extractedContent = it
                    it.content
                })
            }

            val nodeSummarizeText by nodeLLMRequest(
                "summarize_extracted_text",
                allowToolCalls = false,
            )

            val nodeCombineResult by node<Message.Response, SummaryOutput>("combine_result") { response ->
                val content = response.content
                if (content.isBlank() || content.startsWith("Error:", ignoreCase = true)) {
                    throw SummaryException.fromMessage(content)
                }
                SummaryOutput(
                    title = extractedContent?.title ?: "",
                    author = extractedContent?.author ?: "",
                    summary = content,
                    sourceLink = sourceLink,

                    isYoutubeLink = isYoutube,
                    isBiliBiliLink = isBiliBili,
                    length = summaryLength,
                )
            }


            edge(nodeStart forwardTo contentExtractorSubgraph)

            edge(contentExtractorSubgraph forwardTo nodeSummarizeText)

            edge(nodeSummarizeText forwardTo nodeCombineResult)

            edge(nodeCombineResult forwardTo nodeFinish)
        }
}