package me.nanova.summaryexpressive

import me.nanova.summaryexpressive.data.AIProviderConfigEntity
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.model.ProviderConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AIProviderConfigTest {

    @Test
    fun `test AIProviderConfigEntity to and from ProviderConfig`() {
        val models = listOf("gpt-4o", "gpt-4o-mini", "o3-mini")
        val config = ProviderConfig(
            apiKey = "",
            baseUrl = "https://api.openai.com/v1",
            activeModel = "gpt-4o",
            models = models
        )

        val entity = AIProviderConfigEntity.fromProviderConfig("OPENAI", config)
        assertEquals("OPENAI", entity.provider)
        assertEquals(models, entity.models)
        assertEquals("gpt-4o", entity.activeModel)
        assertEquals("https://api.openai.com/v1", entity.baseUrl)

        val convertedConfig = entity.toProviderConfig()
        assertEquals(config.apiKey, convertedConfig.apiKey)
        assertEquals(config.baseUrl, convertedConfig.baseUrl)
        assertEquals(config.activeModel, convertedConfig.activeModel)
        assertEquals(config.models, convertedConfig.models)
    }

    @Test
    fun `test AIProvider default models fallback`() {
        val provider = AIProvider.OPENAI
        val defaultModels = provider.defaultModelIds
        assertTrue(defaultModels.isNotEmpty())
        assertTrue(defaultModels.contains("gpt-4o-mini") || defaultModels.any { it.contains("gpt") })

        // When provider config has empty models, getEffectiveModels should return defaults
        val emptyConfig =
            ProviderConfig(apiKey = "key", baseUrl = "", activeModel = "", models = emptyList())
        assertEquals(defaultModels, provider.getEffectiveModels(emptyConfig))
        assertEquals(defaultModels.first(), provider.getEffectiveModel(emptyConfig))

        // When null config
        assertEquals(defaultModels, provider.getEffectiveModels(null))
        assertEquals(defaultModels.first(), provider.getEffectiveModel(null))
    }

    @Test
    fun `test AIProvider custom models and reordered list`() {
        val provider = AIProvider.OPENAI
        val customModels = listOf("my-custom-model-2", "my-custom-model-1", "gpt-4o")
        val customConfig = ProviderConfig(
            apiKey = "key",
            baseUrl = "",
            activeModel = "my-custom-model-2",
            models = customModels
        )

        val effectiveModels = provider.getEffectiveModels(customConfig)
        assertEquals(customModels, effectiveModels)
        assertEquals("my-custom-model-2", provider.getEffectiveModel(customConfig))
    }

    @Test
    fun `test AIProvider getEffectiveProviders with custom and empty order`() {
        // Empty order returns default AIProvider.entries
        val defaultProviders = AIProvider.getEffectiveProviders(emptyList())
        assertEquals(AIProvider.entries, defaultProviders)

        // Custom order places specified providers first and appends remaining
        val customOrder = listOf("CLAUDE", "DEEPSEEK", "GEMINI")
        val effectiveProviders = AIProvider.getEffectiveProviders(customOrder)
        assertEquals(AIProvider.CLAUDE, effectiveProviders[0])
        assertEquals(AIProvider.DEEPSEEK, effectiveProviders[1])
        assertEquals(AIProvider.GEMINI, effectiveProviders[2])
        assertEquals(AIProvider.entries.size, effectiveProviders.size)
        assertTrue(effectiveProviders.containsAll(AIProvider.entries))
    }
}

