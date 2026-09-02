package me.nanova.summaryexpressive.ui.page.history.section

import android.text.format.DateUtils
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Section header for grouped date intervals in History
 */
@Composable
fun HistorySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * Helper to compute human-friendly date group keys
 */
fun getDateSectionTitle(timestamp: Long): String {
    val now = Calendar.getInstance()
    val itemTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameYear = now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR)
    val isSameDay = isSameYear && now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) return "Today"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "Yesterday"

    val diffDays = (now.timeInMillis - timestamp) / DateUtils.DAY_IN_MILLIS
    if (diffDays in 2..7) return "Previous 7 Days"

    return if (isSameYear) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
