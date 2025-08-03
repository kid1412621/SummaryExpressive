package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import android.content.Context
import io.ktor.client.HttpClient
import me.nanova.summaryexpressive.llm.tools.Article
import me.nanova.summaryexpressive.llm.tools.ArticleExtractorTool
import me.nanova.summaryexpressive.llm.tools.File
import me.nanova.summaryexpressive.llm.tools.FileExtractorTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscript
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool

class LLMHandler(context: Context, httpClient: HttpClient) {
    private val fileExtractorTool: FileExtractorTool = FileExtractorTool(context)
    private val articleExtractorTool = ArticleExtractorTool(httpClient)
    private val youTubeTranscriptTool = YouTubeTranscriptTool(httpClient)

    fun createSummarizationAgent(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String? = null,
        modelName: String? = null,
        summaryLength: SummaryLength = SummaryLength.MEDIUM,
    ): AIAgent<String, String> {
        val executor = createExecutor(provider, apiKey, baseUrl)

        val articleToolRegistry = ToolRegistry { tool(articleExtractorTool) }
        val videoToolRegistry = ToolRegistry { tool(youTubeTranscriptTool) }
        val fileToolRegistry = ToolRegistry { tool(fileExtractorTool) }
        val tools = articleToolRegistry + videoToolRegistry + fileToolRegistry

        val agentConfig = createAgentConfig(provider, modelName, summaryLength)

        return AIAgent(
            promptExecutor = executor,
            strategy = createSummarizationStrategy(
                articleExtractorTool,
                youTubeTranscriptTool,
                fileExtractorTool,
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
            AIProvider.GEMINI -> simpleGoogleAIExecutor(apiKey)
            AIProvider.GROQ -> createGroqExecutor(apiKey)
        }
    }

    private fun createAgentConfig(
        provider: AIProvider,
        modelName: String?,
        summaryLength: SummaryLength,
    ): AIAgentConfig {
        val llmModel = when (provider) {
            // FIXME
            AIProvider.OPENAI -> (modelName?.let { OpenAIModels.Chat.GPT4_1 }
                ?: OpenAIModels.Chat.GPT4o)

            AIProvider.GEMINI -> (modelName?.let { GoogleModels.Gemini2_5Pro }
                ?: GoogleModels.Gemini2_5Flash)

            AIProvider.GROQ -> OpenAIModels.Chat.GPT4o
//            (
//                modelName ?: "llama3-8b-8192"
//            )
        }

        return AIAgentConfig(
            prompt = createSummarizationPrompt(summaryLength),
            model = llmModel,
            maxAgentIterations = 10,
        )
    }

    private fun createOpenAIExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = if (baseUrl.isNullOrBlank()) {
            OpenAILLMClient(apiKey)
        } else {
            OpenAILLMClient(
                apiKey,
                settings = OpenAIClientSettings(baseUrl = baseUrl)
            )
        }
        return SingleLLMPromptExecutor(client)
    }

    private fun createGroqExecutor(apiKey: String): PromptExecutor {
        val client = OpenAILLMClient(
            apiKey,
            settings = OpenAIClientSettings(baseUrl = "https://api.groq.com/openai/v1")
        )
        return SingleLLMPromptExecutor(client)
    }

    private val isYouTubeUrl = { input: String -> YouTubeTranscriptTool.isYouTubeLink(input) }
    private val isWebUrl = { input: String -> input.startsWith("http") && !isYouTubeUrl(input) }
    private val isFileUri =
        { input: String -> input.startsWith("content://") || input.startsWith("file://") }
    private val isPlainText =
        { input: String -> !isWebUrl(input) && !isYouTubeUrl(input) && !isFileUri(input) && input.isNotBlank() }

    /**
     * Creates an AI agent strategy for summarizing text from various sources.
     *
     * This strategy takes an input string (URL, URI, or plain text),
     * extracts text if necessary, and then uses an LLM to generate a summary.
     *
     * **Strategy Graph:**
     * ```
     * [Start] --> [Analyze Input] --+--> [Extract & Prepare Article] -----+--> [Summarize Text] --> [Extract Summary] --> [Finish]
     *                             |                                        |
     *                             +--> [Extract & Prepare Video] ------+
     *                             |                                        |
     *                             +--> [Extract & Prepare File] -------+
     *                             |                                        |
     *                             +--> [Prepare Plain Text] ----------+
     * ```
     *
     * @return An `AIAgentStrategy` that takes a String input and returns a String summary.
     */
    private fun createSummarizationStrategy(
        articleExtractorTool: ArticleExtractorTool,
        youTubeTranscriptTool: YouTubeTranscriptTool,
        fileExtractorTool: FileExtractorTool,
    ): AIAgentStrategy<String, String> =
        strategy("summarization_router_strategy")
        {
            val nodeAnalyzeInput by node<String, String>("analyze_input") { input ->
                input
            }

            val nodeExtractAndPrepareArticle by node<String, String>("extract_and_prepare_article") { url ->
                val extractedText = articleExtractorTool.doExecute(Article(url))
                if (extractedText.startsWith("Error:")) extractedText
                else "Please summarize the following article text:\n\n$extractedText"
            }

            val nodeExtractAndPrepareVideo by node<String, String>("extract_and_prepare_video") { url ->
                val extractedCaptions = youTubeTranscriptTool.doExecute(YouTubeTranscript(url))
                if (extractedCaptions.startsWith("Error:")) extractedCaptions
                else "Please summarize the following video transcript:\n\n$extractedCaptions"
            }

            val nodeExtractAndPrepareFile by node<String, String>("extract_and_prepare_file") { uriString ->
                val extractedText = fileExtractorTool.doExecute(File(uriString))
                if (extractedText.startsWith("Error:")) extractedText
                else "Please summarize the following text extracted from the document:\n\n$extractedText"
            }

            val nodePrepareSummarizationInput by node<String, String>("prepare_summarization_input") { text ->
                "Please summarize the following text:\n\n$text"
            }

            val nodeSummarizeText by nodeLLMRequest(
                "summarize_extracted_text",
                allowToolCalls = false
            )

            val nodeExtractSummaryContent by node<Message.Response, String>("extract_summary_content") { response ->
                response.content
            }

            edge(nodeStart forwardTo nodeAnalyzeInput)

            edge(
                nodeAnalyzeInput forwardTo nodeExtractAndPrepareArticle
                        onCondition { isWebUrl(it) })
            edge(
                nodeAnalyzeInput forwardTo nodeExtractAndPrepareVideo
                        onCondition { isYouTubeUrl(it) })
            edge(
                nodeAnalyzeInput forwardTo nodeExtractAndPrepareFile
                        onCondition { isFileUri(it) })
            edge(
                nodeAnalyzeInput forwardTo nodePrepareSummarizationInput
                        onCondition { isPlainText(it) })

            edge(nodeExtractAndPrepareArticle forwardTo nodeSummarizeText)
            edge(nodeExtractAndPrepareVideo forwardTo nodeSummarizeText)
            edge(nodeExtractAndPrepareFile forwardTo nodeSummarizeText)
            edge(nodePrepareSummarizationInput forwardTo nodeSummarizeText)

            edge(nodeSummarizeText forwardTo nodeExtractSummaryContent)
            edge(nodeExtractSummaryContent forwardTo nodeFinish)
        }
}