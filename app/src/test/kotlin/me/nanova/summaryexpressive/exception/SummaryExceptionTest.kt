package me.nanova.summaryexpressive.exception

import ai.koog.agents.core.agent.exception.AIAgentMaxNumberOfIterationsReachedException
import ai.koog.prompt.executor.clients.LLMClientException
import io.ktor.client.network.sockets.ConnectTimeoutException
import me.nanova.summaryexpressive.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class SummaryExceptionTest {

    @Test
    fun `test network exceptions map to NoInternetException`() {
        val unknownHost = UnknownHostException("Unable to resolve host")
        assertTrue(unknownHost.toSummaryException() is SummaryException.NoInternetException)

        val timeout = ConnectTimeoutException("Connection timed out")
        assertTrue(timeout.toSummaryException() is SummaryException.NoInternetException)

        val wrapped = RuntimeException("Wrapper", UnknownHostException("nested"))
        assertTrue(wrapped.toSummaryException() is SummaryException.NoInternetException)
    }

    @Test
    fun `test agent max iterations maps to TooLongException`() {
        val iterationEx = AIAgentMaxNumberOfIterationsReachedException(10)
        assertTrue(iterationEx.toSummaryException() is SummaryException.TooLongException)
    }

    @Test
    fun `test LLMClientException maps 401 to IncorrectKeyException`() {
        val clientEx = LLMClientException("Error from client: 401 Unauthorized")
        val result = clientEx.toSummaryException()
        assertTrue(result is SummaryException.IncorrectKeyException)
        assertEquals(R.string.incorrectKeyOpenSource, result.getUserMessageResId("test-key"))
        assertEquals(R.string.incorrectKeyOpenSource, result.getUserMessageResId(null))
    }

    @Test
    fun `test OpenRouter 403 model restriction maps to AccessDeniedException with clean message`() {
        val rawError = """
            Error from client: OpenRouterLLMClient
            Error from client: OpenRouterLLMClient
            Status code: -1
            Error body:
            {"error":{"message":"thinkingmachines/inkling:free is only available on agentic harnesses. Try plugging it into a coding agent or productivity app listed on https://openrouter.ai/apps","code":403,"metadata":{"routing_funnel":[{"step":"Initial Endpoints","endpoint_count":1}],"failed_routing_step":"Gate Free Endpoints by Agentic Harness"}}}
        """.trimIndent()
        val clientEx = LLMClientException(rawError)
        val result = clientEx.toSummaryException()

        assertTrue(result is SummaryException.AccessDeniedException)
        val accessDenied = result as SummaryException.AccessDeniedException
        assertEquals(403, accessDenied.statusCode)
        assertEquals(
            "thinkingmachines/inkling:free is only available on agentic harnesses. Try plugging it into a coding agent or productivity app listed on https://openrouter.ai/apps",
            accessDenied.getUserFriendlyMessage()
        )
        assertEquals(R.string.access_denied, accessDenied.getUserMessageResId())
    }

    @Test
    fun `test Anthropic 404 maps to ModelNotFoundException`() {
        val rawError = """
            Error from client: AnthropicLLMClient
            Status code: 404
            Error body:
            {"type":"error","error":{"type":"not_found_error","message":"model: claude-non-existent not found"}}
        """.trimIndent()
        val clientEx = LLMClientException(rawError)
        val result = clientEx.toSummaryException()

        assertTrue(result is SummaryException.ModelNotFoundException)
        assertEquals("model: claude-non-existent not found", result.getUserFriendlyMessage())
        assertEquals(R.string.model_not_found, result.getUserMessageResId())
    }

    @Test
    fun `test HTTP 503 maps to LlmServerException`() {
        val rawError = """
            Error from client: MistralLLMClient
            Status code: 503
            Error body:
            Service Unavailable
        """.trimIndent()
        val clientEx = LLMClientException(rawError)
        val result = clientEx.toSummaryException()

        assertTrue(result is SummaryException.LlmServerException)
        val serverEx = result as SummaryException.LlmServerException
        assertEquals(503, serverEx.statusCode)
        assertEquals("Service Unavailable", serverEx.getUserFriendlyMessage())
        assertEquals(R.string.server_error, serverEx.getUserMessageResId())
    }

    @Test
    fun `test LLMClientException maps 429 to RateLimitException`() {
        val clientEx = LLMClientException("Error from client: 429 Too Many Requests: quota exceeded")
        val result = clientEx.toSummaryException()
        assertTrue(result is SummaryException.RateLimitException)
        assertEquals(R.string.rateLimit, result.getUserMessageResId())
    }

    @Test
    fun `test LLMClientException maps context length exceeded to TooLongException`() {
        val clientEx = LLMClientException("context_length_exceeded: maximum context length is 8192")
        val result = clientEx.toSummaryException()
        assertTrue(result is SummaryException.TooLongException)
        assertEquals(R.string.tooLong, result.getUserMessageResId())
    }

    @Test
    fun `test existing SummaryException is preserved`() {
        val noKey = SummaryException.NoKeyException()
        assertEquals(noKey, noKey.toSummaryException())
    }

    @Test
    fun `test resolveUserErrorMessage prioritizes userFriendlyMessage`() {
        val ex = SummaryException.AccessDeniedException(detail = "Custom restriction reason")
        val resolved = ex.resolveUserErrorMessage { "String-$it" }
        assertEquals("Custom restriction reason", resolved)
    }

    @Test
    fun `test resolveUserErrorMessage falls back to stringResolver`() {
        val ex = SummaryException.NoInternetException()
        val resolved = ex.resolveUserErrorMessage { resId ->
            if (resId == R.string.noInternet) "No Internet" else "Other"
        }
        assertEquals("No Internet", resolved)
    }

    @Test
    fun `test resolveUserErrorMessage with generic exception`() {
        val ex = RuntimeException("A generic failure")
        val resolved = ex.resolveUserErrorMessage { "String-$it" }
        assertEquals("A generic failure", resolved)
    }
}
