package me.nanova.summaryexpressive.vm

import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.ProviderConfig
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput

data class AppStartAction(val content: String? = null, val autoTrigger: Boolean = false)

data class SettingsUiState(
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val aiProvider: AIProvider? = null,
    val providerConfigs: Map<String, ProviderConfig> = emptyMap(),
    val model: String? = null,
    val autoExtractUrl: Boolean = true,
    val showLength: Boolean = true,
    val summaryLength: SummaryLength = SummaryLength.MEDIUM,
    val sessData: String = "",
    val sessDataExpires: Long = 0L,
    val isAppendMode: Boolean = true,
    val customBasePrompt: String = "",
    val additionalSystemPrompt: String = "",
)

data class SummarizationState(
    val isLoading: Boolean = false,
    val summaryResult: SummaryOutput? = null,
    val error: Throwable? = null
)
