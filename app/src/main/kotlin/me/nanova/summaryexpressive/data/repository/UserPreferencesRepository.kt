package me.nanova.summaryexpressive.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import me.nanova.summaryexpressive.data.local.datastore.userPreferencesDataStore
import me.nanova.summaryexpressive.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val preferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(UserPreferences())
            } else {
                throw exception
            }
        }

    private suspend fun updatePreferences(transform: suspend (UserPreferences) -> UserPreferences) {
        context.userPreferencesDataStore.updateData { transform(it) }
    }

    suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    suspend fun setAIProvider(value: String?) = updatePreferences { it.copy(aiProvider = value) }

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

    suspend fun setIsAppendMode(value: Boolean) =
        updatePreferences { it.copy(isAppendMode = value) }

    suspend fun setCustomBasePrompt(value: String) =
        updatePreferences { it.copy(customBasePrompt = value) }

    suspend fun setAdditionalSystemPrompt(value: String) =
        updatePreferences { it.copy(additionalSystemPrompt = value) }
}
