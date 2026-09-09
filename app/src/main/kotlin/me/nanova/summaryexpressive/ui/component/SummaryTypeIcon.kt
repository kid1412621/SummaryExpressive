package me.nanova.summaryexpressive.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import me.nanova.summaryexpressive.model.SummaryType

val SummaryType.icon: ImageVector
    get() = when (this) {
        SummaryType.VIDEO -> Icons.Outlined.Videocam
        SummaryType.ARTICLE -> Icons.AutoMirrored.Outlined.Article
        SummaryType.DOCUMENT -> Icons.AutoMirrored.Outlined.InsertDriveFile
        SummaryType.TEXT -> Icons.AutoMirrored.Outlined.Notes
    }
