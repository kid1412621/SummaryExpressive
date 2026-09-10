package me.nanova.summaryexpressive.exception

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ParsedLlmError(
    val statusCode: Int? = null,
    val errorCode: String? = null,
    val cleanMessage: String? = null,
    val rawMessage: String,
)

object LlmErrorParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val statusCodeRegex = Regex(
        """(?:status\s*code|status|code|HTTP)[:\s=]+(\d{3})""",
        RegexOption.IGNORE_CASE
    )
    private val standaloneCodeRegex = Regex(
        """\b(400|401|402|403|404|408|429|500|502|503|504)\b"""
    )

    fun parse(throwable: Throwable): ParsedLlmError {
        val rawMessage = throwable.message.orEmpty()
        return parse(rawMessage)
    }

    fun parse(rawMessage: String): ParsedLlmError {
        if (rawMessage.isBlank()) {
            return ParsedLlmError(rawMessage = rawMessage)
        }

        val jsonPayload = extractJsonSubstring(rawMessage)
        var parsedStatusCode: Int? = null
        var parsedErrorCode: String? = null
        var parsedMessage: String? = null

        if (jsonPayload != null) {
            runCatching {
                val element = json.parseToJsonElement(jsonPayload)
                if (element is JsonObject) {
                    val errorElement = element["error"]
                    if (errorElement is JsonObject) {
                        parsedMessage = errorElement["message"]?.jsonPrimitive?.contentOrNull
                        parsedErrorCode = errorElement["code"]?.jsonPrimitive?.contentOrNull
                            ?: errorElement["type"]?.jsonPrimitive?.contentOrNull
                    } else if (errorElement != null) {
                        parsedMessage = runCatching { errorElement.jsonPrimitive.contentOrNull }.getOrNull()
                    }

                    if (parsedMessage.isNullOrBlank()) {
                        parsedMessage = element["message"]?.jsonPrimitive?.contentOrNull
                            ?: element["detail"]?.jsonPrimitive?.contentOrNull
                            ?: element["description"]?.jsonPrimitive?.contentOrNull
                    }

                    if (parsedErrorCode.isNullOrBlank()) {
                        parsedErrorCode = element["code"]?.jsonPrimitive?.contentOrNull
                            ?: element["status"]?.jsonPrimitive?.contentOrNull
                    }
                }
            }
        }

        parsedStatusCode = parsedErrorCode?.toIntOrNull()
            ?: statusCodeRegex.find(rawMessage)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: standaloneCodeRegex.find(rawMessage)?.value?.toIntOrNull()

        val cleanMsg = cleanMessageText(parsedMessage ?: rawMessage)

        return ParsedLlmError(
            statusCode = parsedStatusCode,
            errorCode = parsedErrorCode,
            cleanMessage = cleanMsg.takeIf { it.isNotBlank() },
            rawMessage = rawMessage,
        )
    }

    private fun extractJsonSubstring(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start in 0 until end) {
            return text.substring(start, end + 1)
        }
        return null
    }

    private fun cleanMessageText(text: String): String {
        var cleaned = text
            .lines()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("Error from client:", ignoreCase = true) ||
                        trimmed.startsWith("Status code:", ignoreCase = true) ||
                        trimmed.startsWith("Error body:", ignoreCase = true) ||
                        trimmed.startsWith("Client request(", ignoreCase = true) ||
                        trimmed.startsWith("Client request:", ignoreCase = true) ||
                        trimmed.startsWith("Client response:", ignoreCase = true) ||
                        trimmed.startsWith("Headers:", ignoreCase = true)
            }
            .joinToString(" ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
            cleaned = cleaned.substring(1, cleaned.length - 1)
        }

        return cleaned
    }
}
