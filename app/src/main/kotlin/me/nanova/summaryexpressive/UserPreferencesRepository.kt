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
import me.nanova.summaryexpressive.model.CustomPrompt
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class UserPreferences(
    // state
    val isOnboarded: Boolean = false,
    // settings
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val baseUrl: String = "",
    val apiKey: String = "",
    val aiProvider: String = AIProvider.OPENAI.name,
    val model: String = "",
    val showLength: Boolean = true,
    val summaryLength: String = SummaryLength.MEDIUM.name,
    val autoExtractUrl: Boolean = true,
    val sessData: String = "",
    val sessDataExpires: Long = 0L,
    val savedPrompts: List<CustomPrompt> = emptyList(),
    val selectedPromptId: String? = null
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

    suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    suspend fun setBaseUrl(value: String) = updatePreferences { it.copy(baseUrl = value) }

    suspend fun setApiKey(value: String) = updatePreferences { it.copy(apiKey = value) }

    suspend fun setAIProvider(value: String) = updatePreferences { it.copy(aiProvider = value) }

    suspend fun setModel(value: String) = updatePreferences { it.copy(model = value) }

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

    suspend fun savePrompt(prompt: CustomPrompt) = updatePreferences { prefs ->
        val updatedPrompts = prefs.savedPrompts.filter { it.id != prompt.id } + prompt
        prefs.copy(savedPrompts = updatedPrompts)
    }

    suspend fun deletePrompt(promptId: String) = updatePreferences { prefs ->
        val updatedPrompts = prefs.savedPrompts.filter { it.id != promptId }
        val newSelectedId = if (prefs.selectedPromptId == promptId) null else prefs.selectedPromptId
        prefs.copy(savedPrompts = updatedPrompts, selectedPromptId = newSelectedId)
    }

    suspend fun selectPrompt(promptId: String?) =
        updatePreferences { it.copy(selectedPromptId = promptId) }
}