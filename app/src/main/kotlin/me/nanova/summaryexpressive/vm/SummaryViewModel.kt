package me.nanova.summaryexpressive.vm

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nanova.summaryexpressive.data.HistoryRepository
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.llm.tools.BiliBiliSubtitleTool
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val llmHandler: LLMHandler,
    private val application: Application,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    sealed class SummarySource {
        data class Video(val url: String) : SummarySource()
        data class Article(val url: String) : SummarySource()
        data class Text(val content: String) : SummarySource()
        data class Document(val filename: String, val uri: String) : SummarySource()
        object None : SummarySource()
    }

    private val _summarizationState = MutableStateFlow(SummarizationState())
    val summarizationState: StateFlow<SummarizationState> = _summarizationState.asStateFlow()

    fun clearCurrentSummary() {
        _summarizationState.update { it.copy(summaryResult = null, error = null) }
    }

    private fun extractHttpUrl(text: String): String {
        val urlRegex = Regex(
            "(?:^|\\W)((http|https)://)" + // Protocol
                    "([\\w\\-]+\\.){1,}" + // Domain name
                    "([\\w\\-]+)" + // Top-level domain
                    "([^\\s<>\"#%{}|\\\\^`]*)" // Path, query, and fragment
        )
        return urlRegex.find(text)?.value?.trim() ?: text
    }

    private val isFileUri =
        { input: String -> input.startsWith("content://") || input.startsWith("file://") }

    fun summarize(text: String, settings: SettingsUiState) {
        viewModelScope.launch {
            val source = if (isFileUri(text)) {
                val uri = text.toUri()
                val filename = getFileName(application, uri)
                SummarySource.Document(filename, text)
            } else {
                val processedText = if (settings.autoExtractUrl) extractHttpUrl(text) else text
                when {
                    processedText.startsWith("http://", ignoreCase = true)
                            || processedText.startsWith("https://", ignoreCase = true) ->
                        if (YouTubeTranscriptTool.isYouTubeLink(processedText)
                            || BiliBiliSubtitleTool.isBiliBiliLink(processedText)
                        )
                            SummarySource.Video(processedText)
                        else SummarySource.Article(processedText)

                    processedText.isNotBlank() -> SummarySource.Text(processedText)
                    else -> SummarySource.None
                }
            }

            summarizeInternal(source, settings)
        }
    }

    private suspend fun summarizeInternal(source: SummarySource, settings: SettingsUiState) {
        _summarizationState.value = SummarizationState(isLoading = true)
        try {
            val currentApiKey = settings.apiKey
            if (currentApiKey.isNullOrEmpty()) {
                throw SummaryException.NoKeyException()
            }
            if (source is SummarySource.None) {
                throw SummaryException.NoContentException()
            }

            val appLanguage = application.resources.configuration.locales[0]
            val customPrompt = settings.savedPrompts.find { it.id == settings.selectedPromptId }?.content

            val agent = llmHandler.getSummarizationAgent(
                provider = settings.aiProvider,
                apiKey = currentApiKey,
                baseUrl = settings.baseUrl,
                model = settings.model,
                summaryLength = settings.summaryLength,
                customPrompt = customPrompt,
                useContentLanguage = settings.useOriginalLanguage,
                appLanguage = appLanguage
            )

            val summaryOutput = withContext(Dispatchers.IO) {
                agent.run(source)
            }

            _summarizationState.update { it.copy(summaryResult = summaryOutput) }
            saveSummaryToHistory(summaryOutput, settings.summaryLength, source)

        } catch (e: Exception) {
            Log.e("LLMViewModel", "Failed to summarize", e)
            val error =
                e as? SummaryException
                    ?: SummaryException.UnknownException(
                        e.message ?: "An unknown error occurred."
                    )
            _summarizationState.update { it.copy(error = error) }
        } finally {
            _summarizationState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun saveSummaryToHistory(
        summaryOutput: SummaryOutput,
        summaryLength: SummaryLength,
        source: SummarySource,
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
                subtype = when {
                    YouTubeTranscriptTool.isYouTubeLink(source.url) -> VideoSubtype.YOUTUBE
                    source.url.contains("bilibili.com") -> VideoSubtype.BILIBILI
                    else -> null
                }
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
            sourceText = sourceText
        )
        if (summary.summary.isNotBlank() && summary.summary != "invalid link") {
            historyRepository.addSummary(summary)
        }
    }
}