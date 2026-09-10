package me.nanova.summaryexpressive.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LlmErrorParserTest {

    @Test
    fun `test parse OpenRouter nested error JSON with negative client status code`() {
        val raw = """
            Error from client: OpenRouterLLMClient
            Error from client: OpenRouterLLMClient
            Status code: -1
            Error body:
            {"error":{"message":"thinkingmachines/inkling:free is only available on agentic harnesses. Try plugging it into a coding agent or productivity app listed on https://openrouter.ai/apps","code":403,"metadata":{"routing_funnel":[{"step":"Initial Endpoints","endpoint_count":1}],"failed_routing_step":"Gate Free Endpoints by Agentic Harness"}}}
        """.trimIndent()

        val parsed = LlmErrorParser.parse(raw)
        assertEquals(403, parsed.statusCode)
        assertEquals("403", parsed.errorCode)
        assertEquals(
            "thinkingmachines/inkling:free is only available on agentic harnesses. Try plugging it into a coding agent or productivity app listed on https://openrouter.ai/apps",
            parsed.cleanMessage
        )
    }

    @Test
    fun `test parse OpenAI standard error format`() {
        val raw = """
            Error from client: OpenAILLMClient
            Status code: 401
            Error body:
            {"error":{"message":"Incorrect API key provided: sk-proj-***. You can find your API key at https://platform.openai.com/account/api-keys.","type":"invalid_request_error","param":null,"code":"invalid_api_key"}}
        """.trimIndent()

        val parsed = LlmErrorParser.parse(raw)
        assertEquals(401, parsed.statusCode)
        assertEquals("invalid_api_key", parsed.errorCode)
        assertEquals(
            "Incorrect API key provided: sk-proj-***. You can find your API key at https://platform.openai.com/account/api-keys.",
            parsed.cleanMessage
        )
    }

    @Test
    fun `test parse Anthropic error format`() {
        val raw = """
            Error from client: AnthropicLLMClient
            Status code: 404
            Error body:
            {"type":"error","error":{"type":"not_found_error","message":"model: claude-3-unknown not found"}}
        """.trimIndent()

        val parsed = LlmErrorParser.parse(raw)
        assertEquals(404, parsed.statusCode)
        assertEquals("not_found_error", parsed.errorCode)
        assertEquals("model: claude-3-unknown not found", parsed.cleanMessage)
    }

    @Test
    fun `test parse Gemini error format`() {
        val raw = """
            Error from client: GeminiLLMClient
            Status code: 400
            Error body:
            {"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}
        """.trimIndent()

        val parsed = LlmErrorParser.parse(raw)
        assertEquals(400, parsed.statusCode)
        assertEquals("400", parsed.errorCode)
        assertEquals("API key not valid. Please pass a valid API key.", parsed.cleanMessage)
    }

    @Test
    fun `test parse plaintext error without JSON`() {
        val raw = """
            Error from client: OllamaLLMClient
            Status code: 404
            Error body:
            model 'llama2' not found, try pulling it first
        """.trimIndent()

        val parsed = LlmErrorParser.parse(raw)
        assertEquals(404, parsed.statusCode)
        assertEquals("model 'llama2' not found, try pulling it first", parsed.cleanMessage)
    }

    @Test
    fun `test parse empty or blank string`() {
        val parsed = LlmErrorParser.parse("")
        assertNull(parsed.statusCode)
        assertNull(parsed.cleanMessage)
    }
}
