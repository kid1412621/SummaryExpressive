package me.nanova.summaryexpressive.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.nanova.summaryexpressive.domain.provider.AppLocaleProvider
import me.nanova.summaryexpressive.domain.provider.DocumentMetadataProvider
import me.nanova.summaryexpressive.domain.repository.AIProviderConfigRepository
import me.nanova.summaryexpressive.domain.repository.HistoryRepository
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.exception.toSummaryException
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.model.SummarySource
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import javax.inject.Inject

interface SummarizeContentUseCase {
    suspend operator fun invoke(
        text: String,
        overrideLength: SummaryLength? = null,
    ): Result<SummaryOutput>
}

class SummarizeContentUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
    private val historyRepository: HistoryRepository,
    private val llmHandler: LLMHandler,
    private val documentMetadataProvider: DocumentMetadataProvider,
    private val appLocaleProvider: AppLocaleProvider,
) : SummarizeContentUseCase {
    override suspend operator fun invoke(
        text: String,
        overrideLength: SummaryLength?,
    ): Result<SummaryOutput> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = userPreferencesRepository.preferencesFlow.first()
            val providerConfigs = aiProviderConfigRepository.providerConfigsFlow.first()

            val activeProviderEnum = prefs.activeProvider?.let { name ->
                AIProvider.entries.find { it.name == name }
            } ?: throw SummaryException.NoKeyException()

            val providerConfig = providerConfigs[activeProviderEnum.name]
            val apiKey = providerConfig?.apiKey?.takeIf { it.isNotBlank() }
                ?: throw SummaryException.NoKeyException()

            val source = resolveSummarySource(text, prefs.autoExtractUrl)
            if (source is SummarySource.None) {
                throw SummaryException.NoContentException()
            }

            val targetLength = overrideLength
                ?: SummaryLength.entries.find { it.name == prefs.summaryLength }
                ?: SummaryLength.MEDIUM

            val agent = llmHandler.getSummarizationAgent(
                provider = activeProviderEnum,
                apiKey = apiKey,
                baseUrl = providerConfig.baseUrl.takeIf { it.isNotBlank() },
                model = providerConfig.activeModel.takeIf { it.isNotBlank() },
                summaryLength = targetLength,
                showLength = prefs.showLength,
                useContentLanguage = prefs.useOriginalLanguage,
                appLanguage = appLocaleProvider.getCurrentLocale(),
                isAppendMode = prefs.isAppendMode,
                customBasePrompt = prefs.customBasePrompt,
                additionalSystemPrompt = prefs.additionalSystemPrompt,
            )

            val summaryOutput = agent.run(source)

            saveSummaryToHistory(
                summaryOutput = summaryOutput,
                summaryLength = targetLength,
                source = source,
                provider = activeProviderEnum.name,
                model = providerConfig.activeModel.takeIf { it.isNotBlank() },
            )

            summaryOutput
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toSummaryException()) }
        )
    }

    private suspend fun resolveSummarySource(text: String, autoExtractUrl: Boolean): SummarySource {
        if (text.startsWith("content://") || text.startsWith("file://")) {
            val filename = documentMetadataProvider.getFileName(text) ?: "Document"
            return SummarySource.Document(filename, text)
        }

        val processedText = if (autoExtractUrl) extractHttpUrl(text) else text
        return when {
            processedText.startsWith("http://", ignoreCase = true) ||
                    processedText.startsWith("https://", ignoreCase = true) -> {
                if (VideoSubtype.fromUrl(processedText) != null) {
                    SummarySource.Video(processedText)
                } else {
                    SummarySource.Article(processedText)
                }
            }
            processedText.isNotBlank() -> SummarySource.Text(processedText)
            else -> SummarySource.None
        }
    }

    private fun extractHttpUrl(text: String): String {
        val urlRegex = Regex(
            "(?:^|\\W)((http|https)://)" +
                    "([\\w\\-]+\\.)+" +
                    "([\\w\\-]+)" +
                    "([^\\s<>\"#%{}|\\\\^`]*)"
        )
        return urlRegex.find(text)?.value?.trim() ?: text
    }

    private suspend fun saveSummaryToHistory(
        summaryOutput: SummaryOutput,
        summaryLength: SummaryLength,
        source: SummarySource,
        provider: String,
        model: String?,
    ) {
        if (source is SummarySource.None) return

        val type: SummaryType
        var subtype: VideoSubtype? = null
        var sourceLink: String? = null
        var sourceText: String? = null

        when (source) {
            is SummarySource.Article -> {
                type = SummaryType.ARTICLE
                sourceLink = source.url
            }
            is SummarySource.Document -> {
                type = SummaryType.DOCUMENT
                sourceLink = source.uri
            }
            is SummarySource.Text -> {
                type = SummaryType.TEXT
                sourceText = source.content
            }
            is SummarySource.Video -> {
                type = SummaryType.VIDEO
                sourceLink = source.url
                subtype = VideoSubtype.fromUrl(source.url)
            }
            is SummarySource.None -> return
        }

        val summary = HistorySummary(
            title = summaryOutput.title,
            author = summaryOutput.author,
            summary = summaryOutput.summary.trim(),
            length = summaryLength,
            type = type,
            subtype = subtype,
            sourceLink = sourceLink,
            sourceText = sourceText,
            provider = provider,
            model = model,
        )
        if (summary.summary.isNotBlank() && summary.summary != "invalid link") {
            historyRepository.addSummary(summary)
        }
    }
}
