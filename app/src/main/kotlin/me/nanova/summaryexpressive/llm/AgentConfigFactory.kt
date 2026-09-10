package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLModel

object AgentConfigFactory {

    fun resolveModel(provider: AIProvider, modelName: String?): LLModel {
        return modelName?.takeIf { it.isNotBlank() }?.let { name ->
            provider.models.find { it.id == name } ?: CustomLLModel(provider, name).toLLModel()
        } ?: when (provider) {
            AIProvider.OPENAI -> OpenAIModels.Chat.GPT5Nano
            AIProvider.GEMINI -> GoogleModels.Gemini3_5Flash
            AIProvider.CLAUDE -> AnthropicModels.Sonnet_4_6
            AIProvider.DEEPSEEK -> DeepSeekModels.DeepSeekV4Flash
            AIProvider.MISTRAL -> MistralAIModels.Chat.MistralMedium31
            AIProvider.QWEN -> DashscopeModels.QWEN_FLASH
            AIProvider.OLLAMA -> OllamaModels.Alibaba.QWQ
            AIProvider.OPEN_ROUTER -> OpenRouterModels.Claude3Sonnet
            AIProvider.KIMI -> AIProvider.KIMI.models.first()
            AIProvider.MINIMAX -> AIProvider.MINIMAX.models.first()
            AIProvider.ZHIPU -> AIProvider.ZHIPU.models.first()
        }
    }

    fun createAgentConfig(
        provider: AIProvider,
        modelName: String?,
        systemPrompt: String,
        maxAgentIterations: Int = 10,
    ): AIAgentConfig {
        val llmModel = resolveModel(provider, modelName)
        return AIAgentConfig(
            prompt = prompt("summarizer-prompt") {
                system(systemPrompt)
            },
            model = llmModel,
            maxAgentIterations = maxAgentIterations,
        )
    }
}
