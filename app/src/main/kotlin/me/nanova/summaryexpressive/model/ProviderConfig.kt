package me.nanova.summaryexpressive.model

data class ProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val activeModel: String = "",
    val models: List<String> = emptyList(),
)
