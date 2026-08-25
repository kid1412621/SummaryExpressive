package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    // state
    val isOnboarded: Boolean = false,
    // settings
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val aiProvider: String? = null,
    val showLength: Boolean = true,
    val summaryLength: String = SummaryLength.MEDIUM.name,
    val autoExtractUrl: Boolean = true,
    val sessData: String = "",
    val sessDataExpires: Long = 0L,
    val isAppendMode: Boolean = true,
    val customBasePrompt: String = "",
    val additionalSystemPrompt: String = ""
)
