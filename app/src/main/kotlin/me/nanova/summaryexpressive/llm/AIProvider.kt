package me.nanova.summaryexpressive.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.model.ProviderConfig

enum class AIProvider(
    val id: LLMProvider,
    val isMandatoryBaseUrl: Boolean,
    val isRequiredApiKey: Boolean,
    val icon: Int,
    val isMonochromeIcon: Boolean = false,
    val models: List<LLModel> = emptyList(),
) {
    OPENAI(
        LLMProvider.OpenAI,
        false,
        true,
        R.drawable.chatgpt,
        true,
        OpenAIModels.models
            .filter {
                it.supports(LLMCapability.Completion)
                        && !it.supports(LLMCapability.Audio)
            }
            .distinct()
            .sortedBy { it.id }
    ),
    GEMINI(
        LLMProvider.Google,
        false,
        true,
        R.drawable.gemini,
        false,
        GoogleModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    CLAUDE(
        LLMProvider.Anthropic,
        false,
        true,
        R.drawable.claude,
        false,
        AnthropicModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedWith(compareBy<LLModel> { model ->
                val versionRegex = Regex("(\\d[\\d.-]*\\d|\\d)")
                val match = versionRegex.find(model.id)
                match?.value?.replace('-', '.')?.toFloatOrNull() ?: Float.MAX_VALUE
            }.thenBy { it.id })
    ),
    DEEPSEEK(
        LLMProvider.DeepSeek,
        false,
        true,
        R.drawable.deepseek,
        false,
        DeepSeekModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    MISTRAL(
        LLMProvider.MistralAI,
        false,
        true,
        R.drawable.mistral,
        false,
        MistralAIModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    QWEN(
        LLMProvider.Alibaba,
        false,
        true,
        R.drawable.qwen,
        false,
        DashscopeModels.models.filter { it.supports(LLMCapability.Completion) }
    ),
    OLLAMA(
        LLMProvider.Ollama,
        true,
        false,
        R.drawable.ollama,
        true,
        // just name a few most popular models
        listOf(
            OllamaModels.Meta.LLAMA_3_2,
            OllamaModels.Meta.LLAMA_4,
            OllamaModels.Alibaba.QWEN_2_5_05B,
            OllamaModels.Alibaba.QWEN_3_06B,
            LLModel(
                provider = LLMProvider.Google,
                id = "gemma3n",
                capabilities = listOf(LLMCapability.Completion),
                contextLength = 32_768,
            )
        )
    ),
    OPEN_ROUTER(
        LLMProvider.OpenRouter,
        false,
        true,
        R.drawable.openrouter,
        false,
        OpenRouterModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    );

    val defaultModelIds: List<String>
        get() = models.map { it.id }

    fun getEffectiveModels(providerConfig: ProviderConfig?): List<String> =
        providerConfig?.models?.takeIf { it.isNotEmpty() } ?: defaultModelIds

    fun getEffectiveModel(providerConfig: ProviderConfig?): String =
        providerConfig?.activeModel?.takeIf { it.isNotBlank() } ?: defaultModelIds.firstOrNull()
        ?: ""

    companion object {
        fun getEffectiveProviders(order: List<String>): List<AIProvider> {
            if (order.isEmpty()) return entries
            val ordered = order.mapNotNull { name ->
                runCatching { valueOf(name) }.getOrNull()
            }
            val missing = entries.filter { it !in ordered }
            return ordered + missing
        }
    }
}
