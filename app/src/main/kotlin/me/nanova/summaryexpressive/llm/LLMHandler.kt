package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import android.content.Context
import io.ktor.client.HttpClient
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.tools.ArticleExtractorTool
import me.nanova.summaryexpressive.llm.tools.BiliBiliSubtitleTool
import me.nanova.summaryexpressive.llm.tools.FileExtractorTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.model.SummarySource
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val MAX_AGENT_CACHE_SIZE = 20

private data class AgentCacheKey(
    val provider: AIProvider,
    val apiKey: String,
    val baseUrl: String?,
    val model: String?,
    val summaryLength: SummaryLength,
    val systemPrompt: String,
)

class LLMHandler(
    context: Context,
    httpClient: HttpClient,
    userPreferencesRepository: UserPreferencesRepository,
) {
    private val executorFactory = LLMExecutorFactory(httpClient)

    private val fileExtractorTool = FileExtractorTool(context)
    private val articleExtractorTool = ArticleExtractorTool(httpClient)
    private val youTubeTranscriptTool = YouTubeTranscriptTool(httpClient)
    private val bilibiliSubtitleTool = BiliBiliSubtitleTool(httpClient, userPreferencesRepository)

    private val toolRegistry = ToolRegistry {
        tool(articleExtractorTool)
        tool(youTubeTranscriptTool)
        tool(bilibiliSubtitleTool)
        tool(fileExtractorTool)
    }

    private val agentCache = ConcurrentHashMap<AgentCacheKey, AIAgent<SummarySource, SummaryOutput>>()

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
        val promptString = generateFinalPromptString(
            length = summaryLength,
            showLength = showLength,
            useContentLanguage = useContentLanguage,
            appLanguage = appLanguage.getDisplayLanguage(Locale.ENGLISH),
            isAppendMode = isAppendMode,
            customBasePrompt = customBasePrompt,
            additionalSystemPrompt = additionalSystemPrompt,
        )

        val cacheKey = AgentCacheKey(
            provider = provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            summaryLength = summaryLength,
            systemPrompt = promptString,
        )

        return agentCache.computeIfAbsent(cacheKey) {
            if (agentCache.size > MAX_AGENT_CACHE_SIZE) {
                agentCache.clear()
            }
            val executor = executorFactory.getOrCreateExecutor(provider, apiKey, baseUrl, appLanguage)
            val agentConfig = AgentConfigFactory.createAgentConfig(provider, model, promptString)

            AIAgent(
                promptExecutor = executor,
                strategy = createSummarizationStrategy(
                    provider = provider,
                    summaryLength = summaryLength,
                    model = agentConfig.model,
                    articleExtractorTool = articleExtractorTool,
                    youTubeTranscriptTool = youTubeTranscriptTool,
                    bilibiliSubtitleTool = bilibiliSubtitleTool,
                    fileExtractorTool = fileExtractorTool,
                ),
                agentConfig = agentConfig,
                toolRegistry = toolRegistry,
            )
        }
    }
}
