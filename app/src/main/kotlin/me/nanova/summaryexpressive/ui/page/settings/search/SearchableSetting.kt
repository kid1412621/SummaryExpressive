package me.nanova.summaryexpressive.ui.page.settings.search

import androidx.compose.runtime.Composable

/**
 * Model representing a searchable setting entry
 */
data class SearchableSetting(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val group: String,
    val iconBadge: @Composable () -> Unit,
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
)
