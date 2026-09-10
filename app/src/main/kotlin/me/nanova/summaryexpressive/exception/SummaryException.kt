package me.nanova.summaryexpressive.exception

import androidx.annotation.StringRes
import me.nanova.summaryexpressive.R

import ai.koog.agents.core.agent.exception.AIAgentMaxNumberOfIterationsReachedException
import ai.koog.prompt.executor.clients.LLMClientException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import java.net.UnknownHostException

sealed class SummaryException(message: String) : Exception(message) {

    @StringRes
    open fun getUserMessageResId(apiKey: String? = null): Int? = null

    open fun getUserFriendlyMessage(): String? = null

    class NoInternetException : SummaryException("No internet connection.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.noInternet
    }

    class InvalidLinkException : SummaryException("The provided link is invalid.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.invalidURL
    }

    class NoTranscriptException :
        SummaryException("No transcript or subtitles found for this video.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.noTranscript
    }

    class NoContentException : SummaryException("Could not extract any content.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.noContent
    }

    class TooShortException : SummaryException("The content is too short to summarize.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.tooShort
    }

    class PaywallException : SummaryException("Content is behind a paywall.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.paywallDetected
    }

    class TooLongException : SummaryException("The content is too long to process.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.tooLong
    }

    class IncorrectKeyException(val detail: String? = null) :
        SummaryException(detail ?: "The API key is incorrect or invalid.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.incorrectKeyOpenSource
        override fun getUserFriendlyMessage(): String? = detail
    }

    class RateLimitException(val detail: String? = null) :
        SummaryException(detail ?: "API rate limit exceeded. Please try again later.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.rateLimit
        override fun getUserFriendlyMessage(): String? = detail
    }

    class AccessDeniedException(
        val detail: String? = null,
        val statusCode: Int = 403,
    ) : SummaryException(detail ?: "Access denied (HTTP $statusCode).") {
        override fun getUserMessageResId(apiKey: String?) = R.string.access_denied
        override fun getUserFriendlyMessage(): String? = detail
    }

    class ModelNotFoundException(
        val detail: String? = null,
    ) : SummaryException(detail ?: "Model not found.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.model_not_found
        override fun getUserFriendlyMessage(): String? = detail
    }

    class LlmServerException(
        val statusCode: Int? = null,
        val detail: String? = null,
    ) : SummaryException(detail ?: "AI service is currently unavailable.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.server_error
        override fun getUserFriendlyMessage(): String? = detail
    }

    class LlmApiException(
        val userMessage: String,
        val statusCode: Int? = null,
    ) : SummaryException(userMessage) {
        override fun getUserFriendlyMessage(): String = userMessage
    }

    class NoKeyException : SummaryException("API key is not set.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.noKey
    }

    class BiliBiliLoginRequiredException :
        SummaryException("BiliBili login required. Please log in via settings.")

    class UnknownException(
        message: String,
        val cleanMessage: String? = null,
    ) : SummaryException(message) {
        override fun getUserMessageResId(apiKey: String?) =
            if (cleanMessage.isNullOrBlank()) R.string.unknown_error else null
        override fun getUserFriendlyMessage(): String? = cleanMessage
    }

    companion object {
        fun fromMessage(message: String): SummaryException {
            val parsed = LlmErrorParser.parse(message)
            val clean = parsed.cleanMessage ?: message
            return when {
                // Tool error messages
                message.contains("Paywall detected", ignoreCase = true) -> PaywallException()
                message.contains(
                    "BiliBili login required",
                    ignoreCase = true
                ) -> BiliBiliLoginRequiredException()

                message.contains(
                    "Could not extract video ID",
                    ignoreCase = true
                ) -> InvalidLinkException()

                message.contains("transcript", ignoreCase = true) -> NoTranscriptException()
                message.contains(
                    "Could not extract text from URL",
                    ignoreCase = true
                ) -> NoContentException()

                message.contains(
                    "Unsupported file type",
                    ignoreCase = true
                ) -> InvalidLinkException()

                message.contains(
                    "Extracted text from file is empty",
                    ignoreCase = true
                ) -> NoContentException()

                // LLM error messages
                message.contains("API key", ignoreCase = true) -> IncorrectKeyException(detail = clean)
                message.contains("rate limit", ignoreCase = true) -> RateLimitException(detail = clean)

                else -> UnknownException(message = message, cleanMessage = parsed.cleanMessage)
            }
        }
    }
}

fun Throwable.toSummaryException(): SummaryException {
    if (this is SummaryException) return this

    val msg = message ?: ""

    // Handle Ktor or Java network exceptions
    if (this is UnknownHostException ||
        this is ConnectTimeoutException ||
        this is SocketTimeoutException ||
        cause is UnknownHostException ||
        cause is ConnectTimeoutException
    ) {
        return SummaryException.NoInternetException()
    }

    // Handle Koog agent limit exceptions
    if (this is AIAgentMaxNumberOfIterationsReachedException) {
        return SummaryException.TooLongException()
    }

    val parsed = LlmErrorParser.parse(this)
    val statusCode = parsed.statusCode
    val errorCode = parsed.errorCode?.lowercase() ?: ""
    val cleanMsg = parsed.cleanMessage
    val rawLower = msg.lowercase()

    // Handle Koog LLM client exceptions, HTTP errors, or parsed status codes
    if (this is LLMClientException || msg.contains("Error from client", ignoreCase = true) || statusCode != null) {
        return when {
            statusCode == 401 ||
                    errorCode == "invalid_api_key" ||
                    rawLower.contains("invalid api key") ||
                    rawLower.contains("incorrect api key") ||
                    rawLower.contains("unauthorized") ||
                    (rawLower.contains("api key") && rawLower.contains("not valid")) -> {
                SummaryException.IncorrectKeyException(detail = cleanMsg)
            }

            statusCode == 403 ||
                    errorCode == "permission_denied" ||
                    errorCode == "access_denied" ||
                    rawLower.contains("access denied") ||
                    rawLower.contains("forbidden") -> {
                SummaryException.AccessDeniedException(detail = cleanMsg, statusCode = statusCode ?: 403)
            }

            statusCode == 404 ||
                    errorCode == "model_not_found" ||
                    errorCode == "not_found_error" ||
                    (rawLower.contains("model") && rawLower.contains("not found")) ||
                    rawLower.contains("does not exist") -> {
                SummaryException.ModelNotFoundException(detail = cleanMsg)
            }

            statusCode == 429 ||
                    errorCode == "rate_limit_exceeded" ||
                    errorCode == "insufficient_quota" ||
                    rawLower.contains("rate limit") ||
                    rawLower.contains("quota") ||
                    rawLower.contains("resource exhausted") -> {
                SummaryException.RateLimitException(detail = cleanMsg)
            }

            errorCode in listOf("context_length_exceeded", "string_above_max_length") ||
                    rawLower.contains("context_length_exceeded") ||
                    rawLower.contains("maximum context length") ||
                    rawLower.contains("too many tokens") -> {
                SummaryException.TooLongException()
            }

            statusCode != null && statusCode in 500..599 ||
                    rawLower.contains("bad gateway") ||
                    rawLower.contains("service unavailable") ||
                    rawLower.contains("internal server error") -> {
                SummaryException.LlmServerException(statusCode = statusCode, detail = cleanMsg)
            }

            !cleanMsg.isNullOrBlank() -> {
                SummaryException.LlmApiException(userMessage = cleanMsg, statusCode = statusCode)
            }

            else -> SummaryException.fromMessage(msg)
        }
    }

    return SummaryException.fromMessage(msg)
}

inline fun Throwable.resolveUserErrorMessage(
    apiKey: String? = null,
    stringResolver: (Int) -> String,
): String {
    val summaryException = (this as? SummaryException) ?: this.toSummaryException()
    val userFriendly = summaryException.getUserFriendlyMessage()
    val resId = summaryException.getUserMessageResId(apiKey)
    return when {
        !userFriendly.isNullOrBlank() -> userFriendly
        resId != null -> stringResolver(resId)
        !summaryException.message.isNullOrBlank() -> summaryException.message!!
        else -> stringResolver(R.string.unknown_error)
    }
}


