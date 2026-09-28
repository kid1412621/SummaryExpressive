package me.nanova.summaryexpressive.ui.component

import android.content.ClipData
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PauseCircleFilled
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleFilled
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
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

/**
 * Material 3 Expressive Summary Card.
 * Adheres to M3 Expressive visual design:
 * - 28dp extra-large corner radius with tonal surface container
 * - 8dp spacing system with clear typography hierarchy
 * - Deduplicated overview and strict author validation
 * - Expressive spring physics on expand/collapse and TTS playback controls
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
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

    val hasTitle = summary.title.isNotBlank()
    val knownAuthor = summary.author.takeIf { isKnownAuthor(it) }
    val hasMeta = knownAuthor != null || summary.isYoutubeLink || summary.isBiliBiliLink || summary.provider != null
    val showOverview = remember(summary.overview, summary.summary) {
        shouldShowOverview(summary.overview, summary.summary)
    }

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
        shape = MaterialTheme.shapes.extraLarge,
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            if (hasTitle) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Meta row: Author, Source Icon, Provider/Model Indicator
            if (hasMeta) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (knownAuthor != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = knownAuthor,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (summary.isYoutubeLink) {
                            Icon(
                                painter = painterResource(id = R.drawable.youtube),
                                contentDescription = "YouTube",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (summary.isBiliBiliLink) {
                            Icon(
                                painter = painterResource(id = R.drawable.bilibili),
                                contentDescription = "BiliBili",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val aiProvider = summary.provider?.let { providerName ->
                        AIProvider.entries.find { it.name.equals(providerName, ignoreCase = true) }
                    }
                    if (aiProvider != null) {
                        LlmIndicator(
                            provider = aiProvider,
                            model = summary.model,
                            style = LlmIndicatorStyle.PILL,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Topic Tags
            if (summary.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    summary.tags.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Expressive Overview Box (only if distinct and non-duplicated)
            if (showOverview) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 14.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = summary.overview!!.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Key Points
            if (summary.keyPoints.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Key Takeaways",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    summary.keyPoints.forEach { point ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Summary Body
            if (summary.summary.isNotBlank()) {
                Text(
                    text = summary.summary.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else MAX_LINES_WHEN_COLLAPSE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        isTextOverflowing =
                            textLayoutResult.lineCount > MAX_LINES_WHEN_COLLAPSE || textLayoutResult.hasVisualOverflow
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = isTextOverflowing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show More",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        SummaryActionButtons(
            summary = summary,
            onShowSnackbar = onShowSnackbar,
            isPlaying = isPlaying,
            onPlayRequest = onPlayRequest
        )
    }
}

/**
 * Validates that an author string is genuine and not an accidental paragraph, summary text, or placeholder.
 */
private fun isKnownAuthor(author: String?): Boolean {
    if (author.isNullOrBlank()) return false
    val trimmed = author.trim()
    if (trimmed.length > 60 || trimmed.contains('\n') || trimmed.contains('\r')) return false
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith(
            "https://",
            ignoreCase = true
        )
    ) return false
    val lower = trimmed.lowercase(Locale.ROOT)
    return lower !in setOf(
        "unknown",
        "unknown author",
        "n/a",
        "none",
        "null",
        "article",
        "author",
        "creator",
        "speaker"
    )
}

/**
 * Determines whether the overview block should be displayed.
 * Prevents redundant display when overview is identical to or contained in the summary body.
 */
private fun shouldShowOverview(overview: String?, summary: String): Boolean {
    if (overview.isNullOrBlank()) return false
    val o = overview.trim()
    val s = summary.trim()
    if (o.isEmpty() || s.isEmpty()) return false
    if (s.equals(o, ignoreCase = true)) return false
    if (s.startsWith(o, ignoreCase = true)) return false
    if (s.contains(o, ignoreCase = true)) return false
    if (s.length < o.length * 1.25) return false
    return true
}

private const val TAG = "TTS"

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
    val haptics = LocalHapticFeedback.current
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TTS Play / Stop
        FilledTonalIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onPlayRequest()
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (isPlaying) "Stop" else "Read",
                modifier = Modifier.size(20.dp)
            )
        }

        // Animated In-Flight TTS Controls (Pause & Speed)
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isPaused) {
                            resumeOffset = currentPosition
                            val remainingText = summaryText.substring(currentPosition)
                            utteranceId = UUID.randomUUID().toString()
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
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Outlined.PlayCircleFilled else Icons.Outlined.PauseCircleFilled,
                        contentDescription = if (isPaused) "Continue" else "Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AssistChip(
                    onClick = {
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
                    },
                    label = {
                        Text(
                            text = playbackSpeed.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = CircleShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Original Source Link
        summary.sourceLink?.takeIf { it.isNotBlank() }?.let { link ->
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = "Original Link",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Copy Button
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText("Summary", summaryText).toClipEntry()
                    )
                    onShowSnackbar(copied)
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Share Button
        IconButton(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, summaryText)
                }
                val chooserIntent = Intent.createChooser(shareIntent, null)
                context.startActivity(chooserIntent)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview
@Composable
fun SummaryCardPreview() {
    val summary = SummaryOutput(
        title = "Nvidia Acquires Hugging Face: The End of Frontier AI Monopolies?",
        author = "Tech Reporter",
        overview = "Nvidia combines leading hardware with the primary open-source model hub in a blockbuster deal.",
        keyPoints = listOf(
            "Acquisition provides enterprise turnkey hardware-software stack.",
            "Challenges proprietary API moats from OpenAI and Anthropic."
        ),
        tags = listOf("AI", "OpenSource", "Nvidia", "HuggingFace"),
        summary = "Nvidia's acquisition of the open-source AI platform Hugging Face for $12.9 billion combines the leading GPU maker with a central repository for machine learning models. The integration enables enterprises to deploy private hardware and software alternatives to closed frontier labs.",
        isYoutubeLink = true,
        length = SummaryLength.SHORT,
        sourceLink = "https://youtube.com/watch?v=123",
        isBiliBiliLink = false,
        provider = AIProvider.GEMINI.name,
        model = "gemini-2.0-flash"
    )
    SummaryCard(
        summary = summary,
        onShowSnackbar = {}
    )
}