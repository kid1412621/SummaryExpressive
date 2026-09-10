package me.nanova.summaryexpressive.model

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("LlmSummaryResponse")
@LLMDescription("Structured summarization result of the extracted content")
data class LlmSummaryResponse(
    @property:LLMDescription("A concise, informative title for the content")
    val title: String? = null,

    @property:LLMDescription("The original author or creator if identifiable, otherwise null")
    val author: String? = null,

    @property:LLMDescription("A brief 1-2 sentence executive overview / TL;DR")
    val overview: String? = null,

    @property:LLMDescription("Key bullet points / takeaways summarizing the core arguments")
    val keyPoints: List<String> = emptyList(),

    @property:LLMDescription("Comprehensive summary body according to the requested length")
    val bodySummary: String = "",

    @property:LLMDescription("Relevant topic tags (e.g. ['Android', 'AI', 'Architecture'])")
    val tags: List<String> = emptyList(),

    @property:LLMDescription("Identified original language code of the content, e.g. 'en', 'zh'")
    val detectedLanguage: String? = null,

    @property:LLMDescription("Explanation if the content could not be fully summarized or has caveats, otherwise null")
    val errorReason: String? = null,
)
