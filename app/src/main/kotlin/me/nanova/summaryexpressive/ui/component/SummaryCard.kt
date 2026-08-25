package me.nanova.summaryexpressive.ui.component

import android.content.ClipData
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PauseCircleFilled
import androidx.compose.material.icons.outlined.PlayCircleFilled
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.util.getLanguageCode
import java.util.Locale
import java.util.UUID

enum class PlaybackSpeed(val rate: Float, val label: String) {
    HALF(0.5f, "0.5x"),
    NORMAL(1.0f, "1x"),
    FAST(1.25f, "1.25x"),
    FASTER(1.5f, "1.5x"),
    FASTEST(1.75f, "1.75x"),
    DOUBLE(2.0f, "2x");

    fun next(): PlaybackSpeed {
        val values = entries.toTypedArray()
        val nextOrdinal = (ordinal + 1) % values.size
        return values[nextOrdinal]
    }
}

private const val MAX_LINES_WHEN_COLLAPSE = 7

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    summary: SummaryOutput,
    cardColors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    isExpandedByDefault: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onShowSnackbar: (String) -> Unit,
    isPlaying: Boolean = false,
    onPlayRequest: () -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(isExpandedByDefault) }
    var isTextOverflowing by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { if (isTextOverflowing) isExpanded = !isExpanded },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable { if (isTextOverflowing) isExpanded = !isExpanded }
                }
            ),
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier.animateContentSize()
        ) {
            if (summary.title.isNotBlank()) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (summary.author.isNotBlank()) {
                        Text(
                            text = summary.author,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    if (summary.isYoutubeLink) {
                        Icon(
                            painter = painterResource(id = R.drawable.youtube),
                            contentDescription = "YouTube Icon",
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    } else if (summary.isBiliBiliLink) {
                        Icon(
                            painter = painterResource(id = R.drawable.bilibili),
                            contentDescription = "BiliBili Icon",
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val aiProvider = summary.provider?.let { providerName ->
                        AIProvider.entries.find { it.name == providerName }
                    }
                    if (aiProvider != null) {
                        LlmIndicator(
                            provider = aiProvider,
                            model = summary.model,
                            boxSize = 22.dp,
                            fontSize = 6.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            }

            Text(
                text = summary.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else MAX_LINES_WHEN_COLLAPSE,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    isTextOverflowing =
                        textLayoutResult.lineCount > MAX_LINES_WHEN_COLLAPSE || textLayoutResult.hasVisualOverflow
                },
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = if (isTextOverflowing) 0.dp else 12.dp
                    )
            )

            AnimatedVisibility(visible = isTextOverflowing) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp)
                ) {
                    Text(if (isExpanded) "Show Less" else "Show More")
                }
            }
        }

        SummaryActionButtons(
            summary = summary,
            onShowSnackbar = onShowSnackbar,
            isPlaying = isPlaying,
            onPlayRequest = onPlayRequest
        )
    }
}

@Preview
@Composable
fun SummaryCardPreview() {
    val summary = SummaryOutput(
        title = "Sample Title",
        author = "Sample Author",
        summary = "This is a sample summary for preview purposes. It should be long enough to test the TTS functionality and also the layout of the card.",
        isYoutubeLink = true,
        length = SummaryLength.SHORT,
        sourceLink = "",
        isBiliBiliLink = false
    )
    SummaryCard(
        summary = summary,
        onShowSnackbar = {}
    )
}


private const val TAG = "TTS"

@Composable
private fun SummaryActionButtons(
    summary: SummaryOutput,
    onShowSnackbar: (String) -> Unit,
    isPlaying: Boolean,
    onPlayRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val summaryText = summary.summary

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isPaused by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var resumeOffset by remember { mutableIntStateOf(0) }
    var utteranceId by remember { mutableStateOf("") }
    val copied = stringResource(id = R.string.copied)
    var playbackSpeed by remember { mutableStateOf(PlaybackSpeed.NORMAL) }

    val updatedOnPlayRequest by rememberUpdatedState(onPlayRequest)
    val updatedIsPlaying by rememberUpdatedState(isPlaying)

    val utteranceProgressListener =
        remember {
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}

                override fun onDone(utteranceId: String) {
                    scope.launch {
                        if (updatedIsPlaying) {
                            updatedOnPlayRequest()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    onShowSnackbar("Failed to play")
                }

                override fun onRangeStart(
                    utteranceId: String,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    // Is called when a new range of text is being spoken
                    currentPosition = resumeOffset + end
                }
            }
        }

    DisposableEffect(Unit) {
        var textToSpeech: TextToSpeech? = null
        val onInitListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS engine initialized.")
                textToSpeech?.setOnUtteranceProgressListener(utteranceProgressListener)
                tts = textToSpeech
            } else {
                Log.e(TAG, "TTS engine init error.")
                onShowSnackbar("Text-to-speech engine failed to initialize.")
            }
        }
        textToSpeech = TextToSpeech(context, onInitListener)

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(isPlaying) {
        if (tts == null) return@LaunchedEffect

        if (isPlaying) {
            scope.launch {
                val langCode = getLanguageCode(context, summaryText)
                val locale = langCode?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language '$locale' is not supported. Falling back to default.")
                    val defaultResult = tts?.setLanguage(Locale.getDefault())
                    if (defaultResult == TextToSpeech.LANG_MISSING_DATA || defaultResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Default language is also not supported.")
                        onShowSnackbar("Text-to-speech language not supported.")
                    } else {
                        Log.d(TAG, "Language set to default: ${Locale.getDefault()}")
                    }
                } else {
                    Log.d(TAG, "Language set successfully to: $locale")
                }

                resumeOffset = 0
                utteranceId = UUID.randomUUID().toString()
                tts?.setSpeechRate(playbackSpeed.rate)
                tts?.speak(summaryText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        } else {
            tts?.stop()
            isPaused = false
            currentPosition = 0
            resumeOffset = 0
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPlayRequest) {
            Icon(
                Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (isPlaying) "Finish" else "Read",
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(visible = isPlaying) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (isPaused) {
                            resumeOffset = currentPosition
                            val remainingText =
                                summaryText.substring(currentPosition)
                            utteranceId =
                                UUID.randomUUID().toString()
                            tts?.setSpeechRate(playbackSpeed.rate)
                            tts?.speak(
                                remainingText,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                utteranceId
                            )
                            isPaused = false
                        } else {
                            tts?.stop()
                            isPaused = true
                        }
                    }
                ) {
                    Icon(
                        if (isPaused) Icons.Outlined.PlayCircleFilled else Icons.Outlined.PauseCircleFilled,
                        contentDescription = if (isPaused) "Continue" else "Pause",
                        modifier = Modifier.size(24.dp)
                    )
                }
                TextButton(onClick = {
                    val newSpeed = playbackSpeed.next()
                    playbackSpeed = newSpeed
                    if (!isPaused) {
                        tts?.stop()
                        resumeOffset = currentPosition
                        val remainingText = summaryText.substring(currentPosition)
                        utteranceId = UUID.randomUUID().toString()
                        tts?.setSpeechRate(newSpeed.rate)
                        tts?.speak(
                            remainingText,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                        )
                    }
                }) {
                    Text(text = playbackSpeed.label)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        summary.sourceLink?.let {
            if (it.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = "Original Link",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText(
                            "User Input",
                            summaryText
                        ).toClipEntry()
                    )
                    onShowSnackbar(copied)
                }
            }
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        summaryText
                    )
                }
                val chooserIntent =
                    Intent.createChooser(shareIntent, null)
                context.startActivity(chooserIntent)
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun SummaryActionButtonsPreview() {
    val summary = SummaryOutput(
        title = "Sample Title",
        author = "Sample Author",
        summary = "This is a sample summary for preview purposes. It should be long enough to test the TTS functionality and also the layout of the card.",
        sourceLink = "https://xxx.yyy",
        isYoutubeLink = true,
        length = SummaryLength.SHORT,
        isBiliBiliLink = false
    )
    SummaryActionButtons(
        summary = summary,
        onShowSnackbar = {},
        isPlaying = true,
        onPlayRequest = {})
}