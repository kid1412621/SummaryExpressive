package me.nanova.summaryexpressive.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import me.nanova.summaryexpressive.data.converters.StringListConverter
import me.nanova.summaryexpressive.model.ProviderConfig
import me.nanova.summaryexpressive.util.SecurityUtil

@Entity(tableName = "ai_provider_config")
@TypeConverters(StringListConverter::class)
data class AIProviderConfigEntity(
    @PrimaryKey val provider: String,
    val apiKey: String,
    val baseUrl: String,
    @ColumnInfo(name = "active_model") val activeModel: String,
    val models: List<String>? = null,
) {
    fun toProviderConfig(): ProviderConfig {
        return ProviderConfig(
            apiKey = SecurityUtil.decrypt(apiKey),
            baseUrl = baseUrl,
            activeModel = activeModel,
            models = models ?: emptyList()
        )
    }

    companion object {
        fun fromProviderConfig(provider: String, config: ProviderConfig): AIProviderConfigEntity {
            return AIProviderConfigEntity(
                provider = provider,
                apiKey = SecurityUtil.encrypt(config.apiKey),
                baseUrl = config.baseUrl,
                activeModel = config.activeModel,
                models = config.models.takeIf { it.isNotEmpty() }
            )
        }
    }
}
