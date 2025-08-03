package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.util.regex.Pattern

@Serializable
data class YouTubeTranscript(val url: String) : ToolArgs

class YouTubeTranscriptTool(client: HttpClient) : SimpleTool<YouTubeTranscript>() {
    private val youtubeExtractor = YouTubeExtractor(client)

    override val argsSerializer = serializer<YouTubeTranscript>()
    override val descriptor = ToolDescriptor(
        name = "extract_transcript_from_youtube_url",
        description = "Fetches the transcript for a given YouTube video URL.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "url",
                description = "The full URL of the YouTube video.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun doExecute(args: YouTubeTranscript): String {
        return try {
            val videoId = YouTubeExtractor.extractVideoId(args.url)
                ?: return "Error: Could not extract video ID from URL."
            val detailsResult = youtubeExtractor.getVideoDetails(videoId)
                ?: return "Error: Could not get video details."
            val details = detailsResult.first
            val playerResponse = detailsResult.second
            val transcriptResult = youtubeExtractor.getTranscript(videoId, playerResponse)
                ?: return "Error: Could not get transcript."
            val transcript = transcriptResult.first
            val lang = transcriptResult.second
            "Title: ${details.title}\nAuthor: ${details.author}\nLanguage: $lang\n\n$transcript"
        } catch (e: Exception) {
            "Error getting YouTube transcript: ${e.message}"
        }
    }

    companion object {
        fun isYouTubeLink(input: String): Boolean {
            return YouTubeExtractor.isYouTubeLink(input)
        }
    }
}

private data class VideoDetails(val title: String, val author: String)

private data class CaptionTrack(
    val baseUrl: String,
    val name: Name,
    val languageCode: String,
    val kind: String?, // "asr" for auto-generated
) {
    data class Name(val simpleText: String)
}


/**
 * ref: https://github.com/jdepoix/youtube-transcript-api/blob/d2a409d0ce7a7bd35fe6b911ac698038ebf599cc/youtube_transcript_api/_transcripts.py#L359
 */
private class YouTubeExtractor(private val client: HttpClient) {
    private val gson = Gson()

    companion object {
        private const val INNERTUBE_CONTEXT_JSON =
            """{"client": {"clientName": "ANDROID", "clientVersion": "20.10.38"}}"""

        fun extractVideoId(url: String): String? {
            val pattern =
                "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[a-zA-Z0-9_-]+"
            val compiledPattern = Pattern.compile(pattern)
            val matcher = compiledPattern.matcher(url)
            return if (matcher.find()) matcher.group() else null
        }

        fun isYouTubeLink(input: String): Boolean {
            try {
                val urlString = if (!input.matches(Regex("^https?://.*"))) {
                    "https://$input"
                } else {
                    input
                }
                val url = io.ktor.http.Url(urlString)
                val host = url.host.lowercase()
                // Valid hosts: youtu.be, youtube.com, or any subdomain of youtube.com (e.g., www.youtube.com, m.youtube.com)
                return host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
            } catch (e: Exception) {
                Log.e("YouTube", "Exception in isYouTubeLink", e)
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
                Log.e("YouTube", "Failed to fetch watch page: ${watchPageResponse.status}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("YouTube", "Exception in getWatchPageHtml for video $videoId", e)
            return@withContext null
        }
    }

    private suspend fun getPlayerResponseFromApi(videoId: String, html: String): JsonObject? =
        withContext(Dispatchers.IO) {
            val apiKey =
                Regex("\"INNERTUBE_API_KEY\"\\s*:\\s*\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            if (apiKey == null) {
                Log.e("YouTube", "Could not find INNERTUBE_API_KEY in watch page HTML.")
                return@withContext null
            }

            val context = gson.fromJson(INNERTUBE_CONTEXT_JSON, JsonObject::class.java)

            val apiUrl = "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
            val requestBody = JsonObject().apply {
                add("context", context)
                addProperty("videoId", videoId)
            }

            try {
                val response = client.post(apiUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(requestBody))
                }
                if (response.status.isSuccess()) {
                    return@withContext gson.fromJson(response.bodyAsText(), JsonObject::class.java)
                } else {
                    Log.e(
                        "YouTube",
                        "Failed to fetch from /player API: ${response.status}. Body: ${response.bodyAsText()}"
                    )
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("YouTube", "Exception in getPlayerResponseFromApi for video $videoId", e)
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
                val details = playerResponse.getAsJsonObject("videoDetails")
                val title = details?.get("title")?.asString
                val author = details?.get("author")?.asString

                if (title != null && author != null) {
                    return@withContext Pair(VideoDetails(title, author), playerResponse)
                } else {
                    Log.w(
                        "YouTube",
                        "Could not extract video details from 'videoDetails' for $videoId"
                    )
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("YouTube", "Exception in getVideoDetails for video $videoId", e)
                return@withContext null
            }
        }

    suspend fun getTranscript(
        videoId: String,
        playerResponse: JsonObject,
        preferredLanguage: String = "en",
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val playabilityStatus = playerResponse.getAsJsonObject("playabilityStatus")
            if (playabilityStatus != null) {
                val status = playabilityStatus.get("status")?.asString
                if (status == "LOGIN_REQUIRED" || status == "UNPLAYABLE") {
                    val reason = playabilityStatus.get("reason")?.asString ?: status
                    Log.e(
                        "YouTube",
                        "Cannot get transcript for $videoId. Status: $status, Reason: $reason"
                    )
                    return@withContext null
                }
            }

            val captionTracksJson = playerResponse
                .getAsJsonObject("captions")
                ?.getAsJsonObject("playerCaptionsTracklistRenderer")
                ?.getAsJsonArray("captionTracks")

            if (captionTracksJson == null) {
                Log.w("YouTube", "No caption tracks found for video $videoId.")
                return@withContext null
            }

            val captionTrackListType = object : TypeToken<List<CaptionTrack>>() {}.type
            val tracks = gson.fromJson<List<CaptionTrack>>(captionTracksJson, captionTrackListType)

            if (tracks.isEmpty()) {
                Log.w("YouTube", "No caption tracks found for video $videoId.")
                return@withContext null
            }

            // Select the best track (prefer manual over auto, prefer specified language over english)
            val track =
                tracks.firstOrNull { it.languageCode == preferredLanguage && it.kind != "asr" } // Manual, preferred lang
                    ?: tracks.firstOrNull { it.languageCode == "en" && it.kind != "asr" } // Manual, English
                    ?: tracks.firstOrNull { it.languageCode.startsWith(preferredLanguage) } // Auto, preferred lang
                    ?: tracks.firstOrNull { it.languageCode.startsWith("en") } // Auto, English
                    ?: tracks.firstOrNull() // Any available track

            if (track != null) {
                val transcriptUrl = if (track.baseUrl.contains("fmt=srv3")) {
                    track.baseUrl.replace("fmt=srv3", "fmt=json3")
                } else if (track.baseUrl.contains("?")) {
                    "${track.baseUrl}&fmt=json3"
                } else {
                    "${track.baseUrl}?fmt=json3"
                }
                val transcriptResponse = client.get(transcriptUrl) {
                    headers {
                        // Mimic browser behavior; this is required for the request to succeed
                        append("Accept-Language", "$preferredLanguage,en;q=0.9")
                    }
                }
                if (transcriptResponse.status.isSuccess()) {
                    val transcriptJson = transcriptResponse.bodyAsText()
                    if (transcriptJson.isEmpty()) {
                        Log.e("YouTube", "Transcript response body is empty for $transcriptUrl")
                        return@withContext null
                    }
                    val transcriptText = parseJsonTranscript(transcriptJson)
                    return@withContext Pair(transcriptText, track.languageCode)
                } else {
                    Log.e(
                        "YouTube",
                        "Failed to download transcript from $transcriptUrl: ${transcriptResponse.status}"
                    )
                    return@withContext null
                }
            } else {
                Log.w("YouTube", "Could not select a suitable caption track for video $videoId.")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("YouTube", "Exception in getTranscript for video $videoId", e)
            return@withContext null
        }
    }

    private fun parseJsonTranscript(json: String): String {
        return try {
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            val events = jsonObject.getAsJsonArray("events") ?: return ""
            val texts = mutableListOf<String>()

            for (event in events) {
                val eventObj = event.asJsonObject
                if (eventObj.has("segs")) {
                    val segs = eventObj.getAsJsonArray("segs")
                    for (seg in segs) {
                        val segObj = seg.asJsonObject
                        if (segObj.has("utf8")) {
                            texts.add(segObj.get("utf8").asString)
                        }
                    }
                }
            }
            // The JSON can contain newlines which should be spaces, and segments may or may not have spaces.
            // Joining with empty string and then replacing newline with space is the safest bet.
            texts.joinToString("").replace('\n', ' ').trim()
        } catch (e: Exception) {
            Log.e("YouTube", "Failed to parse JSON transcript", e)
            ""
        }
    }
}