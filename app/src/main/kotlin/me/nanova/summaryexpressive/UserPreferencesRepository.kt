package me.nanova.summaryexpressive

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class ProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = ""
)

@Serializable
data class UserPreferences(
    // state
    val isOnboarded: Boolean = false,
    // settings
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val aiProvider: String = AIProvider.OPENAI.name,
    val providerConfigs: Map<String, ProviderConfig> = emptyMap(),
    val showLength: Boolean = true,
    val summaryLength: String = SummaryLength.MEDIUM.name,
    val autoExtractUrl: Boolean = true,
    val sessData: String = "",
    val sessDataExpires: Long = 0L,
    // legacy fields for migration
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
)

class UserPreferencesRepository(private val context: Context) {
    private val userPreferencesKey = stringPreferencesKey("user_preferences")

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[userPreferencesKey]?.let { jsonString ->
                runCatching { Json.decodeFromString<UserPreferences>(jsonString) }.getOrNull()
            } ?: UserPreferences()
        }

    private suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { preferences ->
            val currentPreferencesJson = preferences[userPreferencesKey]
            val currentPreferences = currentPreferencesJson?.let {
                runCatching { Json.decodeFromString<UserPreferences>(it) }.getOrNull()
            } ?: UserPreferences()
            val newPreferences = transform(currentPreferences)
            preferences[userPreferencesKey] = Json.encodeToString(newPreferences)
        }
    }

    suspend fun migrateLegacyFields() {
        updatePreferences { userPrefs ->
            if (userPrefs.apiKey.isNotEmpty() || userPrefs.baseUrl.isNotEmpty() || userPrefs.model.isNotEmpty()) {
                val currentConfig = userPrefs.providerConfigs[userPrefs.aiProvider] ?: ProviderConfig()
                val updatedConfig = currentConfig.copy(
                    apiKey = userPrefs.apiKey.takeIf { it.isNotEmpty() } ?: currentConfig.apiKey,
                    baseUrl = userPrefs.baseUrl.takeIf { it.isNotEmpty() } ?: currentConfig.baseUrl,
                    model = userPrefs.model.takeIf { it.isNotEmpty() } ?: currentConfig.model
                )
                val newConfigs = userPrefs.providerConfigs.toMutableMap()
                newConfigs[userPrefs.aiProvider] = updatedConfig
                userPrefs.copy(
                    providerConfigs = newConfigs,
                    apiKey = "",
                    baseUrl = "",
                    model = ""
                )
            } else {
                userPrefs
            }
        }
    }

    suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    private suspend fun updateProviderConfig(transform: (ProviderConfig) -> ProviderConfig) {
        updatePreferences { prefs ->
            val currentConfig = prefs.providerConfigs[prefs.aiProvider] ?: ProviderConfig()
            val newConfigs = prefs.providerConfigs.toMutableMap()
            newConfigs[prefs.aiProvider] = transform(currentConfig)
            prefs.copy(providerConfigs = newConfigs)
        }
    }

    suspend fun setProviderConfig(provider: String, baseUrl: String, apiKey: String) {
        updatePreferences { prefs ->
            val currentConfig = prefs.providerConfigs[provider] ?: ProviderConfig()
            val newConfigs = prefs.providerConfigs.toMutableMap()
            newConfigs[provider] = currentConfig.copy(baseUrl = baseUrl, apiKey = apiKey)
            prefs.copy(providerConfigs = newConfigs, aiProvider = provider)
        }
    }

    suspend fun setBaseUrl(value: String) = updateProviderConfig { it.copy(baseUrl = value) }

    suspend fun setApiKey(value: String) = updateProviderConfig { it.copy(apiKey = value) }

    suspend fun setAIProvider(value: String) = updatePreferences { it.copy(aiProvider = value) }

    suspend fun setModel(value: String) = updateProviderConfig { it.copy(model = value) }

    suspend fun setIsOnboarded(value: Boolean) =
        updatePreferences { it.copy(isOnboarded = value) }

    suspend fun setShowLength(value: Boolean) = updatePreferences { it.copy(showLength = value) }

    suspend fun setSummaryLength(value: String) =
        updatePreferences { it.copy(summaryLength = value) }

    suspend fun setAutoExtractUrl(value: Boolean) =
        updatePreferences { it.copy(autoExtractUrl = value) }

    suspend fun setSessData(data: String, expires: Long) =
        updatePreferences { it.copy(sessData = data, sessDataExpires = expires) }

    suspend fun clearSessData() =
        updatePreferences { it.copy(sessData = "", sessDataExpires = 0L) }
}