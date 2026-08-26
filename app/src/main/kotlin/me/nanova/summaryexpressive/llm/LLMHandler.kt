package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.agents.core.dsl.extension.nodeExecuteSingleTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequestWithoutTools
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeClientSettings
import ai.koog.prompt.executor.clients.dashscope.DashscopeLLMClient
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekClientSettings
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIClientSettings
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.message.Message
import android.content.Context
import io.ktor.client.HttpClient
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.llm.tools.Article
import me.nanova.summaryexpressive.llm.tools.ArticleExtractorTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliSubtitleTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliVideo
import me.nanova.summaryexpressive.llm.tools.File
import me.nanova.summaryexpressive.llm.tools.FileExtractorTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscript
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.model.ExtractedContent
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.model.SummarySource
import java.util.Locale

class LLMHandler(
    context: Context,
    private val httpClient: HttpClient,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val koogHttpClientFactory = KtorKoogHttpClient.Factory(httpClient)
    private val geminiKoogHttpClientFactory = KtorKoogHttpClient.Factory(
        HttpClient(GeminiSanitizingHttpClientEngine(httpClient.engine))
    )
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
        showLength: Boolean = true,
        useContentLanguage: Boolean,
        appLanguage: Locale,
        isAppendMode: Boolean = true,
        customBasePrompt: String = "",
        additionalSystemPrompt: String = "",
    ): AIAgent<SummarySource, SummaryOutput> {
        val executor = createExecutor(provider, apiKey, baseUrl, appLanguage)

        val articleToolRegistry = ToolRegistry { tool(articleExtractorTool) }
        val youtubeToolRegistry = ToolRegistry { tool(youTubeTranscriptTool) }
        val bilibiliToolRegistry = ToolRegistry { tool(bilibiliSubtitleTool) }
        val fileToolRegistry = ToolRegistry { tool(fileExtractorTool) }
        val tools =
            articleToolRegistry + youtubeToolRegistry + bilibiliToolRegistry + fileToolRegistry

        val agentConfig =
            createAgentConfig(provider, model, summaryLength, showLength, useContentLanguage, appLanguage, isAppendMode, customBasePrompt, additionalSystemPrompt)

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
        appLanguage: Locale,
    ): PromptExecutor {
        return when (provider) {
            AIProvider.OPENAI -> createOpenAIExecutor(apiKey, baseUrl)
            AIProvider.GEMINI -> createGeminiExecutor(apiKey, baseUrl)
            AIProvider.CLAUDE -> createClaudExecutor(apiKey, baseUrl)
            AIProvider.DEEPSEEK -> createDeepSeekExecutor(apiKey, baseUrl)
            AIProvider.MISTRAL -> createMistralExecutor(apiKey, baseUrl)
            AIProvider.QWEN -> createQwenExecutor(apiKey, baseUrl, appLanguage)
            AIProvider.OLLAMA -> createOllamaExecutor(baseUrl)
            AIProvider.OPEN_ROUTER -> createOpenRouterExecutor(apiKey, baseUrl)
        }
    }

    private fun createAgentConfig(
        provider: AIProvider,
        modelName: String?,
        summaryLength: SummaryLength,
        showLength: Boolean,
        useContentLanguage: Boolean,
        appLanguage: Locale,
        isAppendMode: Boolean,
        customBasePrompt: String,
        additionalSystemPrompt: String,
    ): AIAgentConfig {
        val llmModel = modelName?.takeIf { it.isNotBlank() }?.let { name ->
            provider.models.find { it.id == name } ?: CustomLLModel(provider, name).toLLModel()
        } ?: when (provider) {
            AIProvider.OPENAI -> OpenAIModels.Chat.GPT4oMini
            AIProvider.GEMINI -> GoogleModels.Gemini2_5Flash
            AIProvider.CLAUDE -> AnthropicModels.Sonnet_4
            AIProvider.DEEPSEEK -> DeepSeekModels.DeepSeekV4Flash
            AIProvider.MISTRAL -> MistralAIModels.Chat.MistralMedium31
            AIProvider.QWEN -> DashscopeModels.QWEN_FLASH
            AIProvider.OLLAMA -> OllamaModels.Alibaba.QWQ
            AIProvider.OPEN_ROUTER -> OpenRouterModels.Claude3Sonnet
        }

        val lang = appLanguage.getDisplayLanguage(Locale.ENGLISH)
        return AIAgentConfig(
            prompt = createSummarizationPrompt(summaryLength, showLength, useContentLanguage, lang, isAppendMode, customBasePrompt, additionalSystemPrompt),
            model = llmModel,
            maxAgentIterations = 10,
        )
    }

    private fun createOpenAIExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { OpenAILLMClient(apiKey, settings = OpenAIClientSettings(baseUrl = it), httpClientFactory = koogHttpClientFactory) }
            ?: OpenAILLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createGeminiExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { GoogleLLMClient(apiKey, settings = GoogleClientSettings(baseUrl = it), httpClientFactory = geminiKoogHttpClientFactory) }
            ?: GoogleLLMClient(apiKey, httpClientFactory = geminiKoogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createClaudExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { AnthropicLLMClient(apiKey, settings = AnthropicClientSettings(baseUrl = it), httpClientFactory = koogHttpClientFactory) }
            ?: AnthropicLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createDeepSeekExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { DeepSeekLLMClient(apiKey, settings = DeepSeekClientSettings(baseUrl = it), httpClientFactory = koogHttpClientFactory) }
            ?: DeepSeekLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createMistralExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { MistralAILLMClient(apiKey, settings = MistralAIClientSettings(baseUrl = it), httpClientFactory = koogHttpClientFactory) }
            ?: MistralAILLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createQwenExecutor(
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        // assume mainland china user if using simple Chinese
        val isSimplifiedChinese = appLanguage.language == "zh" && appLanguage.script == "Hans"

        val finalBaseUrl = if (isSimplifiedChinese) {
            "https://dashscope.aliyuncs.com"
        } else {
            baseUrl
        }
        val client = finalBaseUrl?.takeIf { it.isNotBlank() }
            ?.let { DashscopeLLMClient(apiKey, settings = DashscopeClientSettings(baseUrl = it), httpClientFactory = koogHttpClientFactory) }
            ?: DashscopeLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createOllamaExecutor(baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { OllamaClient(baseUrl = it, httpClientFactory = koogHttpClientFactory) }
            ?: throw IllegalArgumentException("Base URL is required for Ollama")

        return MultiLLMPromptExecutor(client)
    }

    private fun createOpenRouterExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { OpenRouterLLMClient(apiKey, settings = OpenRouterClientSettings(baseUrl = it), httpClientFactory = koogHttpClientFactory) }
            ?: OpenRouterLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
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

            val nodeSummarizeText by nodeLLMRequestWithoutTools(
                "summarize_extracted_text",
            )

            val nodeCombineResult by node<Message.Assistant, SummaryOutput>("combine_result") { response ->
                val content = response.textContent()
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
