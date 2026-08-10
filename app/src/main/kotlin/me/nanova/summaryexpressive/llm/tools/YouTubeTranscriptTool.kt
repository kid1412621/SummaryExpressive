package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.nanova.summaryexpressive.model.ExtractedContent

@Serializable
data class YouTubeTranscript(
    @property:LLMDescription("The full URL of the YouTube video.") val url: String,
)

class YouTubeTranscriptTool(client: HttpClient) : Tool<YouTubeTranscript, ExtractedContent>(
    argsType = typeToken<YouTubeTranscript>(),
    resultType = typeToken<ExtractedContent>(),
    name = "extract_transcript_from_youtube_url",
    description = "Fetches the transcript for a given YouTube video URL."
) {
    private val youtubeExtractor = YouTubeExtractor(client)


    override suspend fun execute(args: YouTubeTranscript): ExtractedContent {
        val videoId = YouTubeExtractor.extractVideoId(args.url)
            ?: throw Exception("Could not extract video ID from URL: ${args.url}")
        val detailsResult = youtubeExtractor.getVideoDetails(videoId)
            ?: throw Exception("Could not get video details.")
        val details = detailsResult.first
        val playerResponse = detailsResult.second
        val transcriptResult = youtubeExtractor.getTranscript(videoId, playerResponse)
            ?: throw Exception("Could not get transcript.")
        val transcript = transcriptResult.first
        return ExtractedContent(details.title, details.author, transcript)
    }

    companion object {
        fun isYouTubeLink(input: String): Boolean {
            return YouTubeExtractor.isYouTubeLink(input)
        }
    }
}

private data class VideoDetails(val title: String, val author: String)

@Serializable
private data class CaptionTrack(
    val baseUrl: String,
    val name: Name,
    val languageCode: String,
    val kind: String? = null, // "asr" for auto-generated
) {
    @Serializable
    data class Name(val simpleText: String = "")
}


/**
 * ref: https://github.com/jdepoix/youtube-transcript-api/blob/d2a409d0ce7a7bd35fe6b911ac698038ebf599cc/youtube_transcript_api/_transcripts.py#L359
 */
private class YouTubeExtractor(private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "YouTubeExtractor"
        private const val INNERTUBE_CONTEXT_JSON =
            """{"client": {"clientName": "ANDROID", "clientVersion": "20.10.38"}}"""

        private fun ensureScheme(url: String): String {
            return if (!url.matches(Regex("^https?://.*"))) {
                "https://$url"
            } else {
                url
            }
        }

        fun extractVideoId(urlInput: String): String? {
            try {
                val urlString = ensureScheme(urlInput)
                val url = Url(urlString)
                val host = url.host.lowercase()
                val pathSegments = url.segments

                val vid = when {
                    host == "youtu.be" -> pathSegments.firstOrNull()

                    host.endsWith("youtube.com") -> {
                        when (pathSegments.firstOrNull()) {
                            "watch" -> url.parameters["v"]
                            "live", "embed", "v", "shorts" -> pathSegments.getOrNull(1)
                            else -> {
                                // Fallback for URLs like youtube.com/VIDEO_ID or youtube.com/VIDEO_ID?foo=bar
                                if (pathSegments.isNotEmpty()
                                    && pathSegments.first().matches(Regex("[a-zA-Z0-9_-]{11}"))
                                ) {
                                    pathSegments.first()
                                } else {
                                    null
                                }
                            }
                        }
                    }

                    else -> null
                }

                // Standard YouTube video IDs are 11 characters long and use alphanumeric characters, underscores, and hyphens.
                return vid?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]{11}")) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing URL to extract video ID: $urlInput", e)
                return null
            }
        }

        fun isYouTubeLink(input: String): Boolean {
            try {
                val urlString = ensureScheme(input)
                val url = Url(urlString)
                val host = url.host.lowercase()
                // Valid hosts: youtu.be, youtube.com, or any subdomain of youtube.com (e.g., www.youtube.com, m.youtube.com)
                return host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
            } catch (e: Exception) {
                Log.e(TAG, "Exception in isYouTubeLink", e)
                return false
            }
        }
    }

    private suspend fun getWatchPageHtml(videoId: String): String? = withContext(Dispatchers.IO) {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        try {
            val watchPageResponse = client.get(watchUrl) {
                headers {
                    append("Accept-Language", "en-US,en;q=0.9")
                }
            }
            if (watchPageResponse.status.isSuccess()) {
                return@withContext watchPageResponse.bodyAsText()
            } else {
                Log.e(TAG, "Failed to fetch watch page: ${watchPageResponse.status}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getWatchPageHtml for video $videoId", e)
            return@withContext null
        }
    }

    private suspend fun getPlayerResponseFromApi(videoId: String, html: String): JsonObject? =
        withContext(Dispatchers.IO) {
            val apiKey =
                Regex("\"INNERTUBE_API_KEY\"\\s*:\\s*\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            if (apiKey == null) {
                Log.e(TAG, "Could not find INNERTUBE_API_KEY in watch page HTML.")
                return@withContext null
            }

            val context = json.parseToJsonElement(INNERTUBE_CONTEXT_JSON)

            val apiUrl = "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
            val requestBody = buildJsonObject {
                put("context", context)
                put("videoId", videoId)
            }

            try {
                val response = client.post(apiUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }
                if (response.status.isSuccess()) {
                    return@withContext json.parseToJsonElement(response.bodyAsText()).jsonObject
                } else {
                    Log.e(
                        TAG,
                        "Failed to fetch from /player API: ${response.status}. Body: ${response.bodyAsText()}"
                    )
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getPlayerResponseFromApi for video $videoId", e)
                return@withContext null
            }
        }

    private suspend fun getPlayerResponse(videoId: String): JsonObject? =
        withContext(Dispatchers.IO) {
            val watchPageHtml = getWatchPageHtml(videoId) ?: return@withContext null
            // Always use the /player API to fetch the player response, as it's more reliable
            // than parsing the inline JSON from the watch page, which might be incomplete.
            return@withContext getPlayerResponseFromApi(videoId, watchPageHtml)
        }

    suspend fun getVideoDetails(videoId: String): Pair<VideoDetails, JsonObject>? =
        withContext(Dispatchers.IO) {
            try {
                val playerResponse = getPlayerResponse(videoId) ?: return@withContext null
                val details = playerResponse["videoDetails"]?.jsonObject
                val title = details?.get("title")?.jsonPrimitive?.contentOrNull
                val author = details?.get("author")?.jsonPrimitive?.contentOrNull

                if (title != null && author != null) {
                    return@withContext Pair(VideoDetails(title, author), playerResponse)
                } else {
                    Log.w(
                        TAG,
                        "Could not extract video details from 'videoDetails' for $videoId"
                    )
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getVideoDetails for video $videoId", e)
                return@withContext null
            }
        }

    suspend fun getTranscript(
        videoId: String,
        playerResponse: JsonObject,
        preferredLanguage: String = "en",
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val playabilityStatus = playerResponse["playabilityStatus"]?.jsonObject
            if (playabilityStatus != null) {
                val status = playabilityStatus["status"]?.jsonPrimitive?.contentOrNull
                if (status == "LOGIN_REQUIRED" || status == "UNPLAYABLE") {
                    val reason =
                        playabilityStatus["reason"]?.jsonPrimitive?.contentOrNull ?: status
                    Log.e(
                        TAG,
                        "Cannot get transcript for $videoId. Status: $status, Reason: $reason"
                    )
                    return@withContext null
                }
            }

            val captionTracksJson = playerResponse["captions"]?.jsonObject
                ?.get("playerCaptionsTracklistRenderer")?.jsonObject
                ?.get("captionTracks")?.jsonArray

            if (captionTracksJson == null) {
                Log.w(TAG, "No caption tracks found for video $videoId.")
                return@withContext null
            }

            val tracks = json.decodeFromJsonElement<List<CaptionTrack>>(captionTracksJson)

            if (tracks.isEmpty()) {
                Log.w(TAG, "No caption tracks found for video $videoId.")
                return@withContext null
            }

            // Select the best track (prefer manual to auto, prefer specified language over English)
            val track =
                tracks.firstOrNull { it.languageCode == preferredLanguage && it.kind != "asr" } // Manual, preferred lang
                    ?: tracks.firstOrNull { it.languageCode == "en" && it.kind != "asr" } // Manual, English
                    ?: tracks.firstOrNull { it.languageCode.startsWith(preferredLanguage) } // Auto, preferred lang
                    ?: tracks.firstOrNull { it.languageCode.startsWith("en") } // Auto, English
                    ?: tracks.firstOrNull() // Any available track

            if (track == null) {
                Log.w(TAG, "Could not select a suitable caption track for video $videoId.")
                return@withContext null
            }

            val transcriptUrl = if (track.baseUrl.contains("fmt=srv3")) {
                track.baseUrl.replace("fmt=srv3", "fmt=json3")
            } else if (track.baseUrl.contains("?")) {
                "${track.baseUrl}&fmt=json3"
            } else {
                "${track.baseUrl}?fmt=json3"
            }
            val transcriptResponse = client.get(transcriptUrl) {
                // Mimic browser behavior; this is required for the request to succeed
                headers { append("Accept-Language", "$preferredLanguage,en;q=0.9") }
            }

            if (!transcriptResponse.status.isSuccess()) {
                Log.e(
                    TAG,
                    "Failed to download transcript from $transcriptUrl: ${transcriptResponse.status}"
                )
                return@withContext null
            }

            val transcriptJson = transcriptResponse.bodyAsText()
            if (transcriptJson.isEmpty()) {
                Log.e(TAG, "Transcript response body is empty for $transcriptUrl")
                return@withContext null
            }

            val transcriptText = parseJsonTranscript(transcriptJson)
            return@withContext Pair(transcriptText, track.languageCode)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getTranscript for video $videoId", e)
            return@withContext null
        }
    }

    private fun parseJsonTranscript(jsonString: String): String {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val events = jsonObject["events"]?.jsonArray ?: return ""
            val texts = mutableListOf<String>()

            for (event in events) {
                val eventObj = event.jsonObject
                if ("segs" in eventObj) {
                    val segs = eventObj["segs"]!!.jsonArray
                    for (seg in segs) {
                        val segObj = seg.jsonObject
                        if ("utf8" in segObj) {
                            texts.add(segObj["utf8"]!!.jsonPrimitive.content)
                        }
                    }
                }
            }
            // The JSON can contain newlines which should be spaces, and segments may or may not have spaces.
            // Joining with empty string and then replacing newline with space is the safest bet.
            texts.joinToString("").replace('\n', ' ').trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON transcript", e)
            ""
        }
    }
}
