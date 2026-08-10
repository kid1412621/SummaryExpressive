package me.nanova.summaryexpressive.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.nanova.summaryexpressive.ProviderConfig
import me.nanova.summaryexpressive.util.SecurityUtil

@Entity(tableName = "ai_provider_config")
data class AIProviderConfigEntity(
    @PrimaryKey val provider: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String
) {
    fun toProviderConfig(): ProviderConfig {
        return ProviderConfig(
            apiKey = SecurityUtil.decrypt(apiKey),
            baseUrl = baseUrl,
            model = model
        )
    }

    companion object {
        fun fromProviderConfig(provider: String, config: ProviderConfig): AIProviderConfigEntity {
            return AIProviderConfigEntity(
                provider = provider,
                apiKey = SecurityUtil.encrypt(config.apiKey),
                baseUrl = config.baseUrl,
                model = config.model
            )
        }
    }
}
