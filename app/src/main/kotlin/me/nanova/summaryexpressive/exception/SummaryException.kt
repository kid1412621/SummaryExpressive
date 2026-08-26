package me.nanova.summaryexpressive.exception

import androidx.annotation.StringRes
import me.nanova.summaryexpressive.R

sealed class SummaryException(message: String) : Exception(message) {

    @StringRes
    open fun getUserMessageResId(apiKey: String? = null): Int? = null

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

    class IncorrectKeyException : SummaryException("The API key is incorrect or invalid.") {
        override fun getUserMessageResId(apiKey: String?): Int {
            return if (apiKey.isNullOrBlank()) {
                R.string.incorrectKeyOpenSource
            } else {
                R.string.incorrectKey
            }
        }
    }

    class RateLimitException :
        SummaryException("API rate limit exceeded. Please try again later.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.rateLimit
    }

    class NoKeyException : SummaryException("API key is not set.") {
        override fun getUserMessageResId(apiKey: String?) = R.string.noKey
    }

    class BiliBiliLoginRequiredException :
        SummaryException("BiliBili login required. Please log in via settings.")

    class UnknownException(message: String) : SummaryException(message)

    companion object {
        fun fromMessage(message: String): SummaryException {
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

                message.contains("Unsupported file type", ignoreCase = true) -> InvalidLinkException()
                message.contains(
                    "Extracted text from file is empty",
                    ignoreCase = true
                ) -> NoContentException()

                // LLM error messages
                message.contains("API key", ignoreCase = true) -> IncorrectKeyException()
                message.contains("rate limit", ignoreCase = true) -> RateLimitException()

                else -> UnknownException(message)
            }
        }
    }
}
