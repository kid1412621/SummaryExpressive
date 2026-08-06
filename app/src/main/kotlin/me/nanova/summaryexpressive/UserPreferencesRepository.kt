package me.nanova.summaryexpressive

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import java.io.IOException

val Context.dataStore: DataStore<UserPreferences> by dataStore(
    fileName = "user_prefs.pb",
    serializer = UserPreferencesSerializer
)

private val Context.legacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class ProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = ""
)

@Serializable
data class LegacyUserPreferences(
    val isOnboarded: Boolean = false,
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
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
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
    val showLength: Boolean = true,
    val summaryLength: String = SummaryLength.MEDIUM.name,
    val autoExtractUrl: Boolean = true,
    val sessData: String = "",
    val sessDataExpires: Long = 0L,
    val hasMigratedFromLegacy: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {
    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(UserPreferences())
            } else {
                throw exception
            }
        }

    private suspend fun updatePreferences(transform: suspend (UserPreferences) -> UserPreferences) {
        context.dataStore.updateData { transform(it) }
    }

    suspend fun getLegacyPreferences(): LegacyUserPreferences? {
        val userPreferencesKey = stringPreferencesKey("user_preferences")
        val prefs = context.legacyDataStore.data.firstOrNull() ?: return null
        val jsonString = prefs[userPreferencesKey] ?: return null
        return runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<LegacyUserPreferences>(jsonString) }.getOrNull()
    }

    suspend fun markMigratedFromLegacy() {
        updatePreferences { it.copy(hasMigratedFromLegacy = true) }
    }

    suspend fun updateFromLegacy(legacy: LegacyUserPreferences) {
        updatePreferences {
            it.copy(
                isOnboarded = legacy.isOnboarded,
                useOriginalLanguage = legacy.useOriginalLanguage,
                dynamicColor = legacy.dynamicColor,
                theme = legacy.theme,
                aiProvider = legacy.aiProvider,
                showLength = legacy.showLength,
                summaryLength = legacy.summaryLength,
                autoExtractUrl = legacy.autoExtractUrl,
                sessData = legacy.sessData,
                sessDataExpires = legacy.sessDataExpires,
                hasMigratedFromLegacy = true
            )
        }
    }

    suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    suspend fun setAIProvider(value: String) = updatePreferences { it.copy(aiProvider = value) }

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