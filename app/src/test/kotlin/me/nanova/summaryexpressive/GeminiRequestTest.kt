package me.nanova.summaryexpressive

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.runBlocking
import me.nanova.summaryexpressive.llm.GeminiSanitizingHttpClientEngine
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.llm.createSummarizationPrompt
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

@OptIn(InternalAPI::class)
class GeminiRequestTest {

    @Test
    fun testGeminiSanitizingHttpClientEngineRemovesInvalidSchemas() = runBlocking {
        var capturedWireBody = ""

        val rawMockEngine = object : HttpClientEngineBase("raw-mock-engine") {
            override val config: HttpClientEngineConfig = HttpClientEngineConfig()
            override val supportedCapabilities = setOf(io.ktor.client.plugins.HttpTimeoutCapability)
            override val coroutineContext: CoroutineContext = kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO

            override suspend fun execute(data: HttpRequestData): HttpResponseData {
                val body = data.body
                capturedWireBody = when (body) {
                    is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
                    is OutgoingContent.ReadChannelContent -> "ReadChannelContent"
                    else -> body.toString()
                }

                val responseJson = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    { "text": "This is a test summary." }
                                ],
                                "role": "model"
                            },
                            "finishReason": "STOP",
                            "index": 0
                        }
                    ]
                }
                """.trimIndent()
                return HttpResponseData(
                    statusCode = HttpStatusCode.OK,
                    requestTime = GMTDate(),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    version = HttpProtocolVersion.HTTP_1_1,
                    body = ByteReadChannel(responseJson.toByteArray()),
                    callContext = callContext()
                )
            }
        }

        val sanitizingEngine = GeminiSanitizingHttpClientEngine(rawMockEngine)
        val httpClient = HttpClient(sanitizingEngine)
        val factory = KtorKoogHttpClient.Factory(httpClient)
        val client = GoogleLLMClient("dummy_api_key", httpClientFactory = factory)
        val prompt = createSummarizationPrompt(
            length = SummaryLength.MEDIUM,
            showLength = true,
            useContentLanguage = false,
            appLanguage = "English",
        )

        client.execute(prompt, GoogleModels.Gemini2_5Flash, emptyList())

        println("Sanitized wire body:")
        println(capturedWireBody)

        assertFalse(capturedWireBody.contains("toolConfig"), "toolConfig should be removed")
        assertFalse(capturedWireBody.contains("generationConfig"), "empty generationConfig should be removed")
        assertTrue(capturedWireBody.contains("contents"), "contents should remain")
        assertTrue(capturedWireBody.contains("systemInstruction"), "systemInstruction should remain")
    }
}
