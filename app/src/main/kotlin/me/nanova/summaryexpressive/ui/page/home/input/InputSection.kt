package me.nanova.summaryexpressive.ui.page.home.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

@Composable
fun InputSection(
    urlOrText: String,
    onUrlChange: (String) -> Unit,
    onSummarize: () -> Unit,
    documentFilename: String?,
    error: Throwable?,
    apiKey: String?,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDocument = documentFilename != null
    val isUrl = urlOrText.startsWith("http://", ignoreCase = true)
            || urlOrText.startsWith("https://", ignoreCase = true)

    val isExpandable = !isDocument && (urlOrText.length >= 100 || urlOrText.contains('\n'))
    var isExpanded by rememberSaveable(isExpandable) { mutableStateOf(isExpandable) }

    val hasText = remember(urlOrText, documentFilename) {
        documentFilename != null || urlOrText.isNotBlank()
    }
    val textToShow = documentFilename ?: urlOrText

    val (badgeContainerColor, badgeContentColor) = HomeBadges.badgeColorsFor(
        urlOrText = urlOrText,
        documentFilename = documentFilename
    )

    OutlinedTextField(
        value = textToShow,
        onValueChange = onUrlChange,
        label = { Text(stringResource(id = R.string.url_or_text)) },
        enabled = !isLoading,
        readOnly = isDocument,
        isError = error != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSummarize() }),
        leadingIcon = {
            HomeIconBadge(
                containerColor = badgeContainerColor,
                contentColor = badgeContentColor,
                modifier = Modifier.padding(start = 6.dp, end = 2.dp)
            ) {
                val lower = urlOrText.trim().lowercase()
                when {
                    isDocument -> Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = stringResource(id = R.string.document),
                        modifier = Modifier.size(20.dp)
                    )

                    lower.contains("youtube.com") || lower.contains("youtu.be") -> Icon(
                        painter = painterResource(id = R.drawable.youtube),
                        contentDescription = "YouTube",
                        modifier = Modifier.size(20.dp)
                    )

                    lower.contains("bilibili.com") || lower.contains("b23.tv") -> Icon(
                        painter = painterResource(id = R.drawable.bilibili),
                        contentDescription = "BiliBili",
                        modifier = Modifier.size(20.dp)
                    )

                    isUrl -> Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "Web Article",
                        modifier = Modifier.size(20.dp)
                    )

                    else -> Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Text",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        supportingText = {
            if (error != null) {
                ErrorMessage(error = error, apiKey = apiKey)
            } else if (hasText && !isDocument && !isUrl) {
                // Based on the rule of thumb that 100 tokens is about 75 words.
                // ref: https://platform.openai.com/tokenizer
                val wordCount = urlOrText.trim().split(Regex("\\s+")).size
                val tokenCount = (wordCount * 4) / 3
                Text(
                    text = stringResource(id = R.string.approximate_tokens, tokenCount),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.tertiaryFixedDim
                )
            }
        },
        trailingIcon = {
            val clearButton = @Composable {
                AnimatedVisibility(
                    visible = hasText,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = onClear, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = stringResource(id = R.string.clear),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            val expandButton = @Composable {
                AnimatedVisibility(
                    visible = isExpandable,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (isExpanded) {
                                Icons.Rounded.KeyboardArrowUp
                            } else {
                                Icons.Rounded.KeyboardArrowDown
                            },
                            contentDescription = stringResource(
                                if (isExpanded) R.string.collapse else R.string.expand
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = isExpanded,
                label = "trailing-icon-swap",
            ) { targetIsExpanded ->
                if (!targetIsExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        clearButton()
                        expandButton()
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        clearButton()
                        expandButton()
                    }
                }
            }
        },
        maxLines = if (isExpanded) 7 else 1,
        singleLine = !isExpanded,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .animateContentSize()
    )
}

@Composable
fun ErrorMessage(
    error: Throwable?,
    apiKey: String?,
    modifier: Modifier = Modifier,
) {
    val errMsg = when (error) {
        is SummaryException -> {
            val resId = error.getUserMessageResId(apiKey)
            if (resId != null) stringResource(id = resId) else error.message ?: "unknown error"
        }
        else -> error?.message ?: "unknown error"
    }
    Text(
        text = errMsg,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.fillMaxWidth()
    )
}

@Preview
@Composable
private fun InputSectionPreview() {
    val focusRequester = remember { FocusRequester() }

    SummaryExpressiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            InputSection(
                urlOrText = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                onUrlChange = {},
                onSummarize = {},
                error = null,
                apiKey = "test_api_key",
                onClear = {},
                focusRequester = focusRequester,
                documentFilename = null,
                isLoading = false,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            InputSection(
                urlOrText = "uri://for/some/file",
                onUrlChange = {},
                onSummarize = {},
                error = SummaryException.InvalidLinkException(),
                apiKey = "test_api_key",
                onClear = {},
                focusRequester = focusRequester,
                documentFilename = "sample_document.pdf",
                isLoading = false,
            )
        }
    }
}
