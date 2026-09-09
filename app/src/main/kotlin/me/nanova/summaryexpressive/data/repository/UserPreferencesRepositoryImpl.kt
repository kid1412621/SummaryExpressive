package me.nanova.summaryexpressive.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import me.nanova.summaryexpressive.data.local.datastore.userPreferencesDataStore
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    override val preferencesFlow: Flow<UserPreferences>
        get() = context.userPreferencesDataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(UserPreferences())
            } else {
                throw exception
            }
        }

    private suspend fun updatePreferences(transform: suspend (UserPreferences) -> UserPreferences) {
        context.userPreferencesDataStore.updateData { transform(it) }
    }

    override suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    override suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    override suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    override suspend fun setActiveProvider(value: String?) =
        updatePreferences { it.copy(activeProvider = value) }

    override suspend fun setProviderOrder(value: List<String>) =
        updatePreferences { it.copy(providerOrder = value) }

    override suspend fun setIsOnboarded(value: Boolean) =
        updatePreferences { it.copy(isOnboarded = value) }

    override suspend fun setShowLength(value: Boolean) =
        updatePreferences { it.copy(showLength = value) }

    override suspend fun setSummaryLength(value: String) =
        updatePreferences { it.copy(summaryLength = value) }

    override suspend fun setAutoExtractUrl(value: Boolean) =
        updatePreferences { it.copy(autoExtractUrl = value) }

    override suspend fun setBilibiliSessData(data: String, expires: Long) =
        updatePreferences { it.copy(bilibiliSessData = data, bilibiliSessDataExpires = expires) }

    override suspend fun clearBilibiliSessData() =
        updatePreferences { it.copy(bilibiliSessData = "", bilibiliSessDataExpires = 0L) }

    override suspend fun setIsAppendMode(value: Boolean) =
        updatePreferences { it.copy(isAppendMode = value) }

    override suspend fun setCustomBasePrompt(value: String) =
        updatePreferences { it.copy(customBasePrompt = value) }

    override suspend fun setAdditionalSystemPrompt(value: String) =
        updatePreferences { it.copy(additionalSystemPrompt = value) }
}
