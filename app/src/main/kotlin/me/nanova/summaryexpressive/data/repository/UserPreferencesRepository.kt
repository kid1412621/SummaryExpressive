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
open class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context? = null,
) {
    open val preferencesFlow: Flow<UserPreferences>
        get() = context?.userPreferencesDataStore?.data?.catch { exception ->
            if (exception is IOException) {
                emit(UserPreferences())
            } else {
                throw exception
            }
        } ?: kotlinx.coroutines.flow.emptyFlow()

    private suspend fun updatePreferences(transform: suspend (UserPreferences) -> UserPreferences) {
        context?.userPreferencesDataStore?.updateData { transform(it) }
    }

    open suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    open suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    open suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    open suspend fun setActiveProvider(value: String?) =
        updatePreferences { it.copy(activeProvider = value) }

    open suspend fun setProviderOrder(value: List<String>) =
        updatePreferences { it.copy(providerOrder = value) }

    open suspend fun setIsOnboarded(value: Boolean) =
        updatePreferences { it.copy(isOnboarded = value) }

    open suspend fun setShowLength(value: Boolean) =
        updatePreferences { it.copy(showLength = value) }

    open suspend fun setSummaryLength(value: String) =
        updatePreferences { it.copy(summaryLength = value) }

    open suspend fun setAutoExtractUrl(value: Boolean) =
        updatePreferences { it.copy(autoExtractUrl = value) }

    open suspend fun setSessData(data: String, expires: Long) =
        updatePreferences { it.copy(sessData = data, sessDataExpires = expires) }

    open suspend fun clearSessData() =
        updatePreferences { it.copy(sessData = "", sessDataExpires = 0L) }

    open suspend fun setIsAppendMode(value: Boolean) =
        updatePreferences { it.copy(isAppendMode = value) }

    open suspend fun setCustomBasePrompt(value: String) =
        updatePreferences { it.copy(customBasePrompt = value) }

    open suspend fun setAdditionalSystemPrompt(value: String) =
        updatePreferences { it.copy(additionalSystemPrompt = value) }
}
