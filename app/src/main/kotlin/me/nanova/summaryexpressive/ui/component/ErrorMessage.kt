package me.nanova.summaryexpressive.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.exception.toSummaryException

@Composable
fun Throwable?.resolveUserErrorMessage(apiKey: String? = null): String {
    if (this == null) return ""
    val summaryException = (this as? SummaryException) ?: this.toSummaryException()
    val userFriendly = summaryException.getUserFriendlyMessage()
    val resId = summaryException.getUserMessageResId(apiKey)
    return when {
        !userFriendly.isNullOrBlank() -> userFriendly
        resId != null -> stringResource(id = resId)
        !summaryException.message.isNullOrBlank() -> summaryException.message!!
        else -> stringResource(id = R.string.unknown_error)
    }
}

@Composable
fun ErrorMessage(
    error: Throwable?,
    modifier: Modifier = Modifier,
    apiKey: String? = null,
) {
    if (error == null) return
    val errMsg = error.resolveUserErrorMessage(apiKey)
    Text(
        text = errMsg,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
