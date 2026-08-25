package me.nanova.summaryexpressive.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.nanova.summaryexpressive.data.AIProviderConfigDao
import me.nanova.summaryexpressive.data.AIProviderConfigEntity
import me.nanova.summaryexpressive.model.ProviderConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProviderConfigRepository @Inject constructor(
    private val aiProviderConfigDao: AIProviderConfigDao
) {
    val providerConfigsFlow: Flow<Map<String, ProviderConfig>> =
        aiProviderConfigDao.getAllConfigsFlow().map { entities ->
            entities.associate { it.provider to it.toProviderConfig() }
        }

    fun getConfigFlow(provider: String): Flow<ProviderConfig?> =
        aiProviderConfigDao.getConfigFlow(provider).map { it?.toProviderConfig() }

    suspend fun getConfig(provider: String): ProviderConfig? =
        aiProviderConfigDao.getConfig(provider)?.toProviderConfig()

    suspend fun saveConfig(provider: String, config: ProviderConfig) {
        aiProviderConfigDao.insertConfig(
            AIProviderConfigEntity.fromProviderConfig(provider, config)
        )
    }

    suspend fun updateApiKey(provider: String, apiKey: String) {
        val current = getConfig(provider) ?: ProviderConfig()
        saveConfig(provider, current.copy(apiKey = apiKey))
    }

    suspend fun updateBaseUrl(provider: String, baseUrl: String) {
        val current = getConfig(provider) ?: ProviderConfig()
        saveConfig(provider, current.copy(baseUrl = baseUrl))
    }

    suspend fun updateModel(provider: String, model: String) {
        val current = getConfig(provider) ?: ProviderConfig()
        saveConfig(provider, current.copy(model = model))
    }
}
