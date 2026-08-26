package me.nanova.summaryexpressive.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Nav : NavKey {
    @Serializable
    data object Home : Nav

    @Serializable
    data object Onboarding : Nav

    @Serializable
    data object History : Nav

    @Serializable
    data class Settings(val highlight: String? = null) : Nav

    @Serializable
    data object AdvancedSummarySetup : Nav
}