package me.nanova.summaryexpressive.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import me.nanova.summaryexpressive.data.AIProviderConfigDao
import me.nanova.summaryexpressive.data.AIProviderConfigEntity
import me.nanova.summaryexpressive.model.ProviderConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AIProviderConfigRepository @Inject constructor(
    private val aiProviderConfigDao: AIProviderConfigDao? = null,
) {
    open val providerConfigsFlow: Flow<Map<String, ProviderConfig>>
        get() = aiProviderConfigDao?.getAllConfigsFlow()?.map { entities ->
            entities.associate { it.provider to it.toProviderConfig() }
        } ?: emptyFlow()

    open fun getConfigFlow(provider: String): Flow<ProviderConfig?> =
        aiProviderConfigDao?.getConfigFlow(provider)?.map { it?.toProviderConfig() }
            ?: emptyFlow()

    open suspend fun getConfig(provider: String): ProviderConfig? =
        aiProviderConfigDao?.getConfig(provider)?.toProviderConfig()

    open suspend fun saveConfig(provider: String, config: ProviderConfig) {
        aiProviderConfigDao?.insertConfig(
            AIProviderConfigEntity.fromProviderConfig(provider, config)
        )
    }

    open suspend fun updateApiKey(provider: String, apiKey: String) {
        val current = getConfig(provider) ?: ProviderConfig()
        saveConfig(provider, current.copy(apiKey = apiKey))
    }

    open suspend fun updateBaseUrl(provider: String, baseUrl: String) {
        val current = getConfig(provider) ?: ProviderConfig()
        saveConfig(provider, current.copy(baseUrl = baseUrl))
    }

    open suspend fun updateModel(provider: String, model: String) {
        val current = getConfig(provider) ?: ProviderConfig()
        saveConfig(provider, current.copy(activeModel = model))
    }
}
