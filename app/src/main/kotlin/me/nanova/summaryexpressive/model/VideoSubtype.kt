package me.nanova.summaryexpressive.model

enum class VideoSubtype {
    YOUTUBE,
    BILIBILI;

    companion object {
        fun isYouTube(input: String?): Boolean = isYouTubeLink(input)

        fun isBiliBili(input: String?): Boolean = isBiliBiliLink(input)

        fun fromUrl(input: String?): VideoSubtype? = when {
            isYouTubeLink(input) -> YOUTUBE
            isBiliBiliLink(input) -> BILIBILI
            else -> null
        }
    }
}

private val YOUTUBE_REGEX = Regex(
    """(?<![a-zA-Z0-9_/@.-])(?:https?://)?(?:[a-zA-Z0-9-]+\.)*(?:youtube\.com|youtu\.be)(?:[:/?#\s]|$)""",
    RegexOption.IGNORE_CASE
)

private val BILIBILI_REGEX = Regex(
    """(?<![a-zA-Z0-9_/@.-])(?:https?://)?(?:[a-zA-Z0-9-]+\.)*(?:bilibili\.com|b23\.tv|b23\.ms|bili2233\.cn)(?:[:/?#\s]|$)""",
    RegexOption.IGNORE_CASE
)

fun isYouTubeLink(input: String?): Boolean {
    if (input.isNullOrBlank()) return false
    return YOUTUBE_REGEX.containsMatchIn(input)
}

fun isBiliBiliLink(input: String?): Boolean {
    if (input.isNullOrBlank()) return false
    return BILIBILI_REGEX.containsMatchIn(input)
}