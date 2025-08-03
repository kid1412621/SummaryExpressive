package me.nanova.summaryexpressive.vm

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.model.SummaryResult
import me.nanova.summaryexpressive.model.TextSummary
import java.net.URL
import java.util.UUID
import javax.inject.Inject

sealed class SummarySource {
    data class Document(val filename: String?, val uri: String) : SummarySource()
    data class Article(val url: String) : SummarySource()
    data class Video(val url: String) : SummarySource()
    data class Text(val content: String) : SummarySource()
    data object None : SummarySource()
}

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val llmHandler: LLMHandler,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val textSummaries = mutableStateListOf<TextSummary>()

    // Original Language in summary
    private val _useOriginalLanguage = MutableStateFlow(true)
    val useOriginalLanguage: StateFlow<Boolean> = _useOriginalLanguage.asStateFlow()
    fun setUseOriginalLanguageValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setUseOriginalLanguage, newValue)

    // Multiline URL-Field
    private val _multiLine = MutableStateFlow(true)
    val multiLine: StateFlow<Boolean> = _multiLine.asStateFlow()
    fun setMultiLineValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setMultiLine, newValue)

    // UltraDark - Mode
    private val _ultraDark = MutableStateFlow(false)
    val ultraDark: StateFlow<Boolean> = _ultraDark.asStateFlow()
    fun setUltraDarkValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setUltraDark, newValue)

    // DesignNumber for Dark, Light or System
    private val _designNumber = MutableStateFlow(0)
    val designNumber: StateFlow<Int> = _designNumber.asStateFlow()
    fun setDesignNumber(newValue: Int) =
        savePreference(userPreferencesRepository::setDesignNumber, newValue)

    // API Key
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    fun setApiKeyValue(newValue: String) =
        savePreference(userPreferencesRepository::setApiKey, newValue)

    // API base url
    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()
    fun setBaseUrlValue(newValue: String) =
        savePreference(userPreferencesRepository::setBaseUrl, newValue)

    // AI-Model
    private val _model = MutableStateFlow(AIProvider.OPENAI)
    val model: StateFlow<AIProvider> = _model.asStateFlow()
    fun setModelValue(newValue: String) =
        savePreference(userPreferencesRepository::setModel, newValue)

    // OnboardingScreen
    private val _showOnboardingScreen = MutableStateFlow(false)
    val showOnboardingScreen: StateFlow<Boolean> = _showOnboardingScreen.asStateFlow()
    fun setShowOnboardingScreenValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowOnboarding, newValue)

    // Show length
    private val _showLength = MutableStateFlow(true)
    val showLength: StateFlow<Boolean> = _showLength.asStateFlow()
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowLength, newValue)

    // Summary Length
    private val _summaryLength = MutableStateFlow(SummaryLength.MEDIUM)
    val summaryLength: StateFlow<SummaryLength> = _summaryLength.asStateFlow()
    fun setSummaryLength(newValue: SummaryLength) =
        savePreference(userPreferencesRepository::setSummaryLength, newValue.name)

    init {
        loadSummaries()
        loadPreferences()
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            userPreferencesRepository.getTextSummaries().collect { summariesJson ->
                val type = object : TypeToken<List<TextSummary>>() {}.type
                val summaries =
                    kotlin.runCatching { Gson().fromJson<List<TextSummary>>(summariesJson, type) }
                        .getOrNull()
                textSummaries.clear()
                summaries?.let { textSummaries.addAll(it) }
            }
        }
    }

    private fun loadPreferences() {
        userPreferencesRepository.getUseOriginalLanguage().collectInto(_useOriginalLanguage)
        userPreferencesRepository.getMultiLine().collectInto(_multiLine)
        userPreferencesRepository.getUltraDark().collectInto(_ultraDark)
        userPreferencesRepository.getDesignNumber().collectInto(_designNumber)
        userPreferencesRepository.getApiKey().collectInto(_apiKey)
        userPreferencesRepository.getBaseUrl().collectInto(_baseUrl)
        userPreferencesRepository.getModel()
            .collectInto(_model) { AIProvider.valueOf(it) }
        userPreferencesRepository.getShowOnboarding()
            .collectInto(_showOnboardingScreen)
        userPreferencesRepository.getShowLength().collectInto(_showLength)
        userPreferencesRepository.getSummaryLength()
            .collectInto(_summaryLength) { SummaryLength.valueOf(it) }
    }

    fun addTextSummary(title: String?, author: String?, text: String?, youtubeLink: Boolean) {
        val nonNullTitle = title ?: ""
        val nonNullAuthor = author ?: ""

        if (!text.isNullOrBlank() && text != "invalid link") {
            val uniqueId = UUID.randomUUID().toString()
            val newTextSummary =
                TextSummary(uniqueId, nonNullTitle, nonNullAuthor, text, youtubeLink)
            textSummaries.add(newTextSummary)
            // Save text data in SharedPreferences
            saveTextSummaries()
        }
    }

    private fun saveTextSummaries() {
        viewModelScope.launch {
            val textSummariesJson = Gson().toJson(textSummaries)
            userPreferencesRepository.setTextSummaries(textSummariesJson)
        }
    }

    fun removeTextSummary(id: String) {
        // Find the TextSummary object to remove based on id
        val textSummaryToRemove = textSummaries.firstOrNull { it.id == id }

        // Check whether a matching TextSummary object was found and remove it
        textSummaryToRemove?.let {
            textSummaries.remove(it)

            // After removing, save the updated ViewModel (if necessary)
            saveTextSummaries()
        }
    }

    fun searchTextSummary(searchText: String): List<String> {
        return textSummaries
            .filter {
                it.title.contains(searchText, ignoreCase = true) or it.author.contains(
                    searchText,
                    ignoreCase = true
                ) or it.text.contains(searchText, ignoreCase = true)
            }
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()
    }

    // --- Summarization State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSummaryResult = MutableStateFlow<SummaryResult?>(null)
    val currentSummaryResult: StateFlow<SummaryResult?> = _currentSummaryResult.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    fun clearCurrentSummary() {
        _currentSummaryResult.value = null
        _error.value = null
    }

    fun summarize(source: SummarySource) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentSummaryResult.value = null
            _error.value = null
            try {
                if (apiKey.value.isEmpty()) {
                    throw SummaryException.NoKeyException
                }

                val inputString = prepareSummarizationInput(source)

                val agent = llmHandler.createSummarizationAgent(
                    provider = model.value,
                    apiKey = apiKey.value,
                    baseUrl = if (model.value == AIProvider.OPENAI) baseUrl.value else null,
                    modelName = null, // TODO: user custom model selection
                    summaryLength = summaryLength.value
                )

                val summary = withContext(Dispatchers.IO) {
                    agent.run(inputString)
                }

                if (summary.startsWith("Error:")) {
                    if (summary.contains(
                            "API key",
                            ignoreCase = true
                        )
                    ) throw SummaryException.IncorrectKeyException
                    if (summary.contains(
                            "rate limit",
                            ignoreCase = true
                        )
                    ) throw SummaryException.RateLimitException
                    throw SummaryException.UnknownException(summary)
                }

                val result = createSummaryResult(source, summary)
                _currentSummaryResult.value = result
                addTextSummary(result.title, result.author, result.summary, result.isYoutubeLink)

            } catch (e: Exception) {
                Log.e("SummaryViewModel", "Failed to summarize", e)
                _error.value =
                    if (e is SummaryException) e else SummaryException.UnknownException(
                        e.message ?: "An unknown error occurred."
                    )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun prepareSummarizationInput(source: SummarySource): String {
        val inputString = when (source) {
            is SummarySource.Article -> source.url
            is SummarySource.Document -> source.uri
            is SummarySource.Text -> source.content
            is SummarySource.Video -> source.url
            is SummarySource.None -> ""
        }

        if (inputString.isBlank()) {
            throw SummaryException.NoContentException
        }

        if (source is SummarySource.Article || source is SummarySource.Video) {
            if (runCatching { URL(inputString).host }.getOrNull().isNullOrEmpty()) {
                throw SummaryException.InvalidLinkException
            }
        }
        return inputString
    }

    private fun createSummaryResult(source: SummarySource, summary: String): SummaryResult {
        val isYoutube = source is SummarySource.Video
        val title = when (source) {
            is SummarySource.Article -> runCatching { URL(source.url).host }.getOrNull()
                ?: "Article"

            is SummarySource.Document -> source.filename ?: "Document"
            is SummarySource.Text -> "Text"
            is SummarySource.Video -> "YouTube Video"
            is SummarySource.None -> ""
        }
        val author = when (source) {
            is SummarySource.Article -> "Web"
            is SummarySource.Document -> "File"
            is SummarySource.Text -> "User Input"
            is SummarySource.Video -> "YouTube"
            is SummarySource.None -> ""
        }
        return SummaryResult(title, author, summary.trim(), isYoutube)
    }


    // --- Preference Handling Helpers ---
    private fun <T, R> Flow<T>.collectInto(
        stateFlow: MutableStateFlow<R>,
        transform: (T) -> R,
    ) {
        viewModelScope.launch {
            this@collectInto.collect { stateFlow.value = transform(it) }
        }
    }

    private fun <T> Flow<T>.collectInto(stateFlow: MutableStateFlow<T>) =
        collectInto(stateFlow) { it }

    private fun <T> savePreference(setter: suspend (T) -> Unit, value: T) {
        viewModelScope.launch {
            setter(value)
        }
    }
}