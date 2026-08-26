package me.nanova.summaryexpressive.ui.page.home

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    val hasText = remember(urlOrText) { urlOrText.isNotBlank() }
    val textToShow = documentFilename ?: urlOrText

    OutlinedTextField(
        value = textToShow,
        onValueChange = onUrlChange,
        label = { Text("URL/Text") },
        enabled = !isLoading,
        readOnly = isDocument,
        isError = error != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSummarize() }),
        supportingText = {
            if (error != null) {
                ErrorMessage(error = error, apiKey = apiKey)
            } else if (hasText && !isDocument && !isUrl) {
                Column {
                    // Based on the rule of thumb that 100 tokens is about 75 words.
                    // ref: https://platform.openai.com/tokenizer
                    val wordCount = urlOrText.trim().split(Regex("\\s+")).size
                    val tokenCount = (wordCount * 4) / 3
                    Text(
                        text = "Approximate tokens: $tokenCount",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.tertiaryFixedDim
                    )
                }
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
                            contentDescription = "Clear",
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
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
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
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
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
                urlOrText = "A very long text to test the multiline feature. This text is intentionally made long to exceed the one hundred character limit that is used to trigger the visibility of the expand and collapse button. It also includes\na line break.",
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
