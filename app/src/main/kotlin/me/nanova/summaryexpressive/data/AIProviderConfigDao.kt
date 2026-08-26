package me.nanova.summaryexpressive.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AIProviderConfigDao {
    @Query("SELECT * FROM ai_provider_config WHERE provider = :provider")
    fun getConfigFlow(provider: String): Flow<AIProviderConfigEntity?>

    @Query("SELECT * FROM ai_provider_config WHERE provider = :provider")
    suspend fun getConfig(provider: String): AIProviderConfigEntity?

    @Query("SELECT * FROM ai_provider_config")
    fun getAllConfigsFlow(): Flow<List<AIProviderConfigEntity>>

    @Query("SELECT * FROM ai_provider_config")
    suspend fun getAllConfigs(): List<AIProviderConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AIProviderConfigEntity)
}
