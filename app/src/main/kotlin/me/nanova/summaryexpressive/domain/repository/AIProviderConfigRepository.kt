package me.nanova.summaryexpressive.domain.repository

import kotlinx.coroutines.flow.Flow
import me.nanova.summaryexpressive.model.ProviderConfig

interface AIProviderConfigRepository {
    val providerConfigsFlow: Flow<Map<String, ProviderConfig>>

    fun getConfigFlow(provider: String): Flow<ProviderConfig?>
    suspend fun getConfig(provider: String): ProviderConfig?
    suspend fun saveConfig(provider: String, config: ProviderConfig)
    suspend fun updateApiKey(provider: String, apiKey: String)
    suspend fun updateBaseUrl(provider: String, baseUrl: String)
    suspend fun updateModel(provider: String, model: String)
}
