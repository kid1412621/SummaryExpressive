package me.nanova.summaryexpressive.data.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.nanova.summaryexpressive.domain.provider.AppLocaleProvider
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppLocaleProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppLocaleProvider {
    override fun getCurrentLocale(): Locale {
        return context.resources.configuration.locales[0]
    }
}
