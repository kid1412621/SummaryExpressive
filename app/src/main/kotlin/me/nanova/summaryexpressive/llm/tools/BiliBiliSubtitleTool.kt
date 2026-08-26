package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.data.repository.UserPreferencesRepository
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.model.ExtractedContent

@Serializable
private data class BiliVideoInfoResponse(
    val code: Int,
    val message: String,
    val data: BiliVideoInfoData?,
)

@Serializable
private data class BiliVideoInfoData(val cid: Long, val title: String, val owner: BiliOwner)

@Serializable
private data class BiliOwner(val name: String)

@Serializable
private data class BiliPlayerResponse(val code: Int, val message: String, val data: BiliPlayerData?)

@Serializable
private data class BiliPlayerData(val subtitle: BiliSubtitleInfo?)

@Serializable
private data class BiliSubtitleInfo(val subtitles: List<BiliSubtitleItem>?)

@Serializable
private data class BiliSubtitleItem(
    // zh-CN, ai-zh
    val lan: String,
    // 0: user upload, 1: AI generated
    val type: Int,
    @SerialName("subtitle_url") val subtitleUrl: String,
//    @SerialName("subtitle_url_v2") val subtitleUrlV2: String,
)

@Serializable
private data class BiliSubtitleContentResponse(val body: List<BiliSubtitleContentItem>)

@Serializable
private data class BiliSubtitleContentItem(val content: String)

@Serializable
data class BiliBiliVideo(
    @property:LLMDescription("The full URL of the BiliBili video.")
    val url: String,
)

/**
 * ref: https://github.com/Nemo2011/bilibili-api/blob/49b47197adb29f5ae9a974f090165dfe69ed0bba/bilibili_api/video.py#L1526C15-L1526C27
 * */
class BiliBiliSubtitleTool(
    private val client: HttpClient,
    private val userPreferencesRepository: UserPreferencesRepository,
) : Tool<BiliBiliVideo, ExtractedContent>(
    argsType = typeToken<BiliBiliVideo>(),
    resultType = typeToken<ExtractedContent>(),
    name = "extract_subtitle_from_bilibili_url",
    description = "Fetches the subtitle for a given BiliBili video URL."
) {
    private val bvidRegex = "(BV[1-9A-HJ-NP-Za-km-z]{10})".toRegex()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "BiliBiliSubtitleTool"
        private fun ensureScheme(url: String): String {
            return if (!url.matches(Regex("^https?://.*"))) "https://$url" else url
        }

        fun isBiliBiliLink(input: String): Boolean {
            try {
                val urlString = ensureScheme(input)
                val url = Url(urlString)
                val host = url.host.lowercase()
                return host == "b23.tv" || host == "bilibili.com" || host.endsWith(".bilibili.com")
            } catch (e: Exception) {
                Log.e(TAG, "Exception in isBiliBiliLink", e)
                return false
            }
        }
    }

    override suspend fun execute(args: BiliBiliVideo): ExtractedContent {
        return withContext(Dispatchers.IO) {
            val bvid = extractBvid(args.url)
                ?: throw SummaryException.InvalidLinkException()

            val sessData = userPreferencesRepository.preferencesFlow.first().sessData
            if (sessData.isBlank()) {
                throw SummaryException.BiliBiliLoginRequiredException()
            }

            val videoDetails = getVideoDetails(bvid, sessData)
            val cid = videoDetails.cid
            val title = videoDetails.title
            val author = videoDetails.owner.name

            val subtitles = getSubtitlesUrl(bvid, cid, sessData)
            var transcript: String? = null
            // there's no other way, trust me
            val failedSubtitle =
                "友情提示：如果视频本身没有添加字幕的，是无法使用此方法打开字幕选项的！"
            for (subtitle in subtitles) {
                val subtitleContent = getSubtitleContent(subtitle.subtitleUrl)
                val currentTranscript = subtitleContent.body.joinToString("\n") { it.content }
                if (!currentTranscript.contains(failedSubtitle)) {
                    transcript = currentTranscript
                    break
                }
            }

            if (transcript == null) {
                throw SummaryException.NoTranscriptException()
            }


            ExtractedContent(title, author, transcript)
        }
    }

    private suspend fun extractBvid(url: String): String? {
        val urlString = ensureScheme(url)
        val parsedUrl = Url(urlString)
        val finalUrl = if (parsedUrl.host == "b23.tv") {
            // For b23.tv short links, we need to resolve the redirect to get the full URL.
            // Ktor HttpClient follows redirects by default.
            val response = client.head(parsedUrl)
            response.request.url
        } else {
            parsedUrl
        }

        val pathSegments = finalUrl.segments.filter { it.isNotEmpty() }

        // Prioritize finding the BVID in path segments that follow /video/
        val bvidFromPath =
            if (pathSegments.size >= 2 && pathSegments.first().equals("video", ignoreCase = true)) {
                pathSegments[1]
            } else {
                // Otherwise, check if the first segment is a BVID (common after redirects)
                pathSegments.firstOrNull()
            }

        // Verify that the extracted string matches the BVID format.
        return bvidFromPath?.takeIf { bvidRegex.matches(it) }
    }

    private suspend fun getVideoDetails(bvid: String, sessData: String): BiliVideoInfoData {
        val url = "https://api.bilibili.com/x/web-interface/view"
        val response = client.get(url) {
            header("Cookie", "SESSDATA=$sessData")
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            )
            parameter("bvid", bvid)
        }

        if (!response.status.isSuccess()) {
            Log.e(TAG, "Failed to fetch video info for bvid $bvid: ${response.status}")
            throw SummaryException.NoInternetException()
        }

        val responseBody = response.bodyAsText()
        val videoInfo = json.decodeFromString<BiliVideoInfoResponse>(responseBody)

        if (videoInfo.code != 0) {
            Log.e(
                TAG,
                "BiliBili API error for video info for bvid $bvid: ${videoInfo.code} ${videoInfo.message}"
            )
            throw SummaryException.NoContentException()
        }

        return videoInfo.data
            ?: throw SummaryException.NoContentException()
    }

    private suspend fun getSubtitlesUrl(
        bvid: String,
        cid: Long,
        sessData: String,
    ): List<BiliSubtitleItem> {
        val url = "https://api.bilibili.com/x/player/wbi/v2"
        val response = client.get(url) {
            header("Cookie", "SESSDATA=$sessData")
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            )
            parameter("bvid", bvid)
            parameter("cid", cid)
            parameter("isGaiaAvoided", false)
            parameter("web_location", 1315873)
        }

        if (!response.status.isSuccess()) {
            Log.e(TAG, "Failed to fetch player info for bvid $bvid, cid $cid: ${response.status}")
            throw SummaryException.NoInternetException()
        }

        val responseBody = response.bodyAsText()
        val playerInfo = json.decodeFromString<BiliPlayerResponse>(responseBody)

        if (playerInfo.code != 0) {
            Log.e(
                TAG,
                "BiliBili API error for player info for bvid $bvid, cid $cid: ${playerInfo.code} ${playerInfo.message}"
            )
            throw SummaryException.NoTranscriptException()
        }

        val subtitles = playerInfo.data?.subtitle?.subtitles
        if (subtitles.isNullOrEmpty()) {
            Log.w(TAG, "No subtitles available for bvid $bvid, cid $cid.")
            throw SummaryException.NoTranscriptException()
        }

        val subtitlesUrl = subtitles
            .filter { it.subtitleUrl.isNotBlank() }
            .map { it.copy(subtitleUrl = prependHttps(it.subtitleUrl)) }
            // try user uploaded subtitle or Chinese subtitle first
            .sortedByDescending { it.type == 0 || it.lan.contains("zh", ignoreCase = true) }

        if (subtitlesUrl.isEmpty()) {
            Log.w(TAG, "No subtitles available for bvid $bvid, cid $cid.")
            throw SummaryException.NoTranscriptException()
        }
        return subtitlesUrl
    }

    // BiliBili subtitle URLs sometimes start with "//", which needs "https:" prefix
    private fun prependHttps(url: String): String {
        return if (url.startsWith("//")) "https:${url}" else url
    }

    private suspend fun getSubtitleContent(subtitleUrl: String): BiliSubtitleContentResponse {
        val response = client.get(subtitleUrl) {
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            )
        }

        if (!response.status.isSuccess()) {
            Log.e(TAG, "Failed to fetch subtitle content from $subtitleUrl: ${response.status}")
            throw SummaryException.NoInternetException()
        }

        val responseBody = response.bodyAsText()
        return json.decodeFromString<BiliSubtitleContentResponse>(responseBody)
    }
}
