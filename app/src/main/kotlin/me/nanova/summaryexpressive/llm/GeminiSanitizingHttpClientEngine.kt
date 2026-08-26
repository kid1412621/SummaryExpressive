package me.nanova.summaryexpressive.llm

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext

/**
 * A custom [HttpClientEngine] decorator dedicated to Google Gemini requests.
 *
 * ### Rationale:
 * Koog's `GoogleLLMClient` unconditionally serializes a `GoogleToolConfig` object into the request body:
 * ```kotlin
 * toolConfig = GoogleToolConfig(functionCallingConfig)
 * ```
 * When no tool choice is configured (`functionCallingConfig == null`), Kotlinx Serialization outputs an
 * empty object `"toolConfig": {}` in the JSON payload.
 *
 * When the Google Gemini API receives `"toolConfig": {}` without valid tool definitions or function configs,
 * Google's OpenAPI/JSON schema validator rejects the request with HTTP 400:
 * `{"error": {"code": 400, "message": "schema at top-level must be a boolean or an object", "status": "INVALID_ARGUMENT"}}`.
 *
 * This engine sanitizes outgoing request bodies by stripping out empty `"toolConfig": {}`, empty `"generationConfig": {}`,
 * and empty `"tools": []` before sending the request to the Gemini API.
 */
@OptIn(InternalAPI::class)
class GeminiSanitizingHttpClientEngine(
    private val delegate: HttpClientEngine,
) : HttpClientEngineBase("gemini-sanitizing-engine") {

    override val config: HttpClientEngineConfig = delegate.config
    override val supportedCapabilities = delegate.supportedCapabilities
    override val coroutineContext: CoroutineContext = delegate.coroutineContext

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val body = data.body
        val sanitizedBody = when (body) {
            is OutgoingContent.ByteArrayContent -> {
                val original = body.bytes().decodeToString()
                val sanitized = sanitizeGeminiRequestBody(original)
                val bytes = sanitized.toByteArray(Charsets.UTF_8)
                object : OutgoingContent.ByteArrayContent() {
                    override val contentType: ContentType =
                        body.contentType ?: ContentType.Application.Json
                    override val contentLength: Long = bytes.size.toLong()
                    override fun bytes(): ByteArray = bytes
                }
            }

            else -> body
        }

        val sanitizedData = HttpRequestData(
            url = data.url,
            method = data.method,
            headers = data.headers,
            body = sanitizedBody,
            executionContext = data.executionContext,
            attributes = data.attributes
        )

        return delegate.execute(sanitizedData)
    }

    /**
     * Parses the JSON request payload and removes empty/malformed schema properties that cause Gemini API 400 errors.
     */
    private fun sanitizeGeminiRequestBody(jsonString: String): String {
        val element = try {
            json.parseToJsonElement(jsonString)
        } catch (_: Exception) {
            return jsonString
        }
        if (element !is JsonObject) return jsonString

        val modified = element.toMutableMap()

        // 1. Remove tools if null or empty array
        val tools = modified["tools"]
        val hasTools = tools is JsonArray && tools.isNotEmpty()
        if (tools is JsonArray && tools.isEmpty()) {
            modified.remove("tools")
        }

        // 2. Remove toolConfig if empty, functionCallingConfig is missing/empty, or tools is absent
        val toolConfig = modified["toolConfig"]
        if (toolConfig is JsonObject) {
            val functionCallingConfig = toolConfig["functionCallingConfig"]
            val isFunctionCallingConfigEmpty = functionCallingConfig == null ||
                    (functionCallingConfig is JsonObject && functionCallingConfig.isEmpty())
            if (!hasTools || toolConfig.isEmpty() || isFunctionCallingConfigEmpty) {
                modified.remove("toolConfig")
            }
        }

        // 3. Remove generationConfig if empty
        val generationConfig = modified["generationConfig"]
        if (generationConfig is JsonObject && generationConfig.isEmpty()) {
            modified.remove("generationConfig")
        }

        return json.encodeToString(JsonObject(modified))
    }
}
