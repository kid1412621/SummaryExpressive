package me.nanova.summaryexpressive.domain.provider

import java.util.Locale

interface AppLocaleProvider {
    fun getCurrentLocale(): Locale
}
