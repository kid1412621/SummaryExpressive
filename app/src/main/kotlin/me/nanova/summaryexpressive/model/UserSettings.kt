package me.nanova.summaryexpressive.model

data class UserSettings(
    val preferences: UserPreferences,
    val providerConfigs: Map<String, ProviderConfig>,
)
