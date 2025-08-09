package me.nanova.summaryexpressive

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    private val useOriginalLanguage = booleanPreferencesKey("use_original_language")
    private val dynamicColor = booleanPreferencesKey("dynamic_color")
    private val theme = intPreferencesKey("theme")
    private val baseUrl = stringPreferencesKey("base_url")
    private val apiKey = stringPreferencesKey("api_key")
    private val model = stringPreferencesKey("model")
    private val showOnboarding = booleanPreferencesKey("show_onboarding")
    private val showLengthSelection = booleanPreferencesKey("show_length")
    private val summaryLength = stringPreferencesKey("summary_length")
    private val history = stringPreferencesKey("text_summaries_json")

    fun getUseOriginalLanguage(): Flow<Boolean> =
        context.dataStore.data.map { it[useOriginalLanguage] ?: true }

    suspend fun setUseOriginalLanguage(value: Boolean) =
        context.dataStore.edit { it[useOriginalLanguage] = value }

    fun getDynamicColor(): Flow<Boolean> =
        context.dataStore.data.map { it[dynamicColor] ?: true }

    suspend fun setDynamicColor(value: Boolean) =
        context.dataStore.edit { it[dynamicColor] = value }

    fun getTheme(): Flow<Int> =
        context.dataStore.data.map { it[theme] ?: 0 }

    suspend fun setTheme(value: Int) =
        context.dataStore.edit { it[theme] = value }

    fun getBaseUrl(): Flow<String> =
        context.dataStore.data.map { it[baseUrl] ?: "" }

    suspend fun setBaseUrl(value: String) =
        context.dataStore.edit { it[baseUrl] = value }

    fun getApiKey(): Flow<String> = context.dataStore.data.map { it[apiKey] ?: "" }
    suspend fun setApiKey(value: String) =
        context.dataStore.edit { it[apiKey] = value }

    fun getModel(): Flow<String> =
        // fixme handle default value
        context.dataStore.data.map { it[model] ?: AIProvider.OPENAI.name }

    suspend fun setModel(value: String) =
        context.dataStore.edit { it[model] = value }

    fun getShowOnboarding(): Flow<Boolean> =
        context.dataStore.data.map { it[showOnboarding] ?: false }

    suspend fun setShowOnboarding(value: Boolean) =
        context.dataStore.edit { it[showOnboarding] = value }

    fun getShowLength(): Flow<Boolean> =
        context.dataStore.data.map { it[showLengthSelection] ?: true }

    suspend fun setShowLength(value: Boolean) =
        context.dataStore.edit { it[showLengthSelection] = value }

    fun getSummaryLength(): Flow<String> =
        context.dataStore.data.map { it[summaryLength] ?: SummaryLength.MEDIUM.name }

    suspend fun setSummaryLength(value: String) =
        context.dataStore.edit { it[summaryLength] = value }

    fun getTextSummaries(): Flow<String> =
        context.dataStore.data.map { it[history] ?: "[]" }

    suspend fun setTextSummaries(json: String) =
        context.dataStore.edit { it[history] = json }
}