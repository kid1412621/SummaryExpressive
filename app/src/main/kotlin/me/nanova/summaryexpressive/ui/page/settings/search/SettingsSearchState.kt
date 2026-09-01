package me.nanova.summaryexpressive.ui.page.settings.search

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpCenter
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.page.settings.SettingsActions
import me.nanova.summaryexpressive.ui.page.settings.section.SettingBadges
import me.nanova.summaryexpressive.ui.page.settings.section.SettingIconBadge
import me.nanova.summaryexpressive.vm.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Date

@Stable
class SettingsSearchState(
    initialQuery: String = "",
    val allItems: List<SearchableSetting>,
) {
    var query by mutableStateOf(initialQuery)
        private set

    val isSearching: Boolean
        get() = query.trim().isNotEmpty()

    val filteredItems: List<SearchableSetting>
        get() {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return emptyList()
            return allItems.filter { item ->
                item.title.contains(trimmed, ignoreCase = true) ||
                        (item.subtitle?.contains(trimmed, ignoreCase = true) == true) ||
                        item.group.contains(trimmed, ignoreCase = true)
            }
        }

    fun onQueryChange(newQuery: String) {
        query = newQuery
    }

    fun onClear() {
        query = ""
    }
}

@Composable
fun rememberSettingsSearchState(
    state: SettingsUiState,
    actions: SettingsActions,
    onNav: (Nav) -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowAIProviderDialog: () -> Unit,
    onShowModelDialog: () -> Unit,
    onShowBiliBiliLoginSheet: () -> Unit,
    onShowClearSessDataDialog: () -> Unit,
): SettingsSearchState {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    // Configuration-aware string resources
    val chooseLanguageTitle = stringResource(R.string.chooseLanguage)
    val chooseLanguageSubtitle = stringResource(R.string.chooseLanguageDescription)
    val themeTitle = stringResource(R.string.theme)
    val useDynamicColorTitle = stringResource(R.string.useDynamicColor)
    val useDynamicColorSubtitle = stringResource(R.string.useDynamicColorDescription)
    val setAIProviderTitle = stringResource(R.string.setAIProvider)
    val setModelTitle = stringResource(R.string.setModel)
    val advancedSummaryTitle = stringResource(R.string.advancedSummarySetup)
    val advancedSummarySubtitle = stringResource(R.string.advancedSummarySetupDescription)
    val useAutoExtractLinkTitle = stringResource(R.string.useAutoExtractLink)
    val useAutoExtractLinkSubtitle = stringResource(R.string.useAutoExtractLinkDescription)
    val tutorialTitle = stringResource(R.string.tutorial)
    val tutorialSubtitle = stringResource(R.string.tutorialDescription)
    val googlePlayTitle = stringResource(R.string.googlePlay)
    val googlePlaySubtitle = stringResource(R.string.googlePlayDescription)
    val discordTitle = stringResource(R.string.discord)
    val discordSubtitle = stringResource(R.string.discordDescription)
    val repoTitle = stringResource(R.string.repository)
    val repoSubtitle = stringResource(R.string.githubDescription)

    // BiliBili login details
    val sessDataValid =
        (state.sessData.isNotBlank() && state.sessDataExpires > System.currentTimeMillis())
    val bilibiliSubtitle = if (sessDataValid) {
        val expiryDate = SimpleDateFormat(
            "yyyy-MM-dd",
            LocalLocale.current.platformLocale
        ).format(Date(state.sessDataExpires))
        "Logged in, expires on $expiryDate. Long press to clear."
    } else {
        "BiliBili required login to get transcripts which used for video summary"
    }

    val themeSubtitle = when (state.theme) {
        1 -> stringResource(id = R.string.darkTheme)
        2 -> stringResource(id = R.string.lightTheme)
        else -> stringResource(id = R.string.systemTheme)
    }

    val aiProviderSubtitle = state.activeProvider?.name
        ?: stringResource(id = R.string.setAIProviderDescription)

    val modelSubtitle = state.activeModel
        ?: stringResource(id = R.string.setModelDescription)

    val allItems = remember(
        state,
        sessDataValid,
        bilibiliSubtitle,
        themeSubtitle,
        aiProviderSubtitle,
        modelSubtitle,
        chooseLanguageTitle,
        chooseLanguageSubtitle,
        themeTitle,
        useDynamicColorTitle,
        useDynamicColorSubtitle,
        setAIProviderTitle,
        setModelTitle,
        advancedSummaryTitle,
        advancedSummarySubtitle,
        useAutoExtractLinkTitle,
        useAutoExtractLinkSubtitle,
        tutorialTitle,
        tutorialSubtitle,
        googlePlayTitle,
        googlePlaySubtitle,
        discordTitle,
        discordSubtitle,
        repoTitle,
        repoSubtitle
    ) {
        listOf(
            SearchableSetting(
                id = "language",
                title = chooseLanguageTitle,
                subtitle = chooseLanguageSubtitle,
                group = "Display & Interface",
                iconBadge = {
                    val (bg, fg) = SettingBadges.languageColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
            ),
            SearchableSetting(
                id = "theme",
                title = themeTitle,
                subtitle = themeSubtitle,
                group = "Display & Interface",
                iconBadge = {
                    val (bg, fg) = SettingBadges.themeColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = onShowThemeDialog
            ),
            SearchableSetting(
                id = "dynamic_color",
                title = useDynamicColorTitle,
                subtitle = useDynamicColorSubtitle,
                group = "Display & Interface",
                iconBadge = {
                    val (bg, fg) = SettingBadges.dynamicColorColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                trailingContent = {
                    Switch(
                        checked = state.dynamicColor,
                        onCheckedChange = { actions.onDynamicColorChange(it) }
                    )
                },
                onClick = { actions.onDynamicColorChange(!state.dynamicColor) }
            ),
            SearchableSetting(
                id = "ai_provider",
                title = setAIProviderTitle,
                subtitle = aiProviderSubtitle,
                group = "AI & Models",
                iconBadge = {
                    val (bg, fg) = SettingBadges.aiColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = onShowAIProviderDialog
            ),
            SearchableSetting(
                id = "model",
                title = setModelTitle,
                subtitle = modelSubtitle,
                group = "AI & Models",
                iconBadge = {
                    val (bg, fg) = SettingBadges.modelColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = onShowModelDialog
            ),
            SearchableSetting(
                id = "advanced_summary",
                title = advancedSummaryTitle,
                subtitle = advancedSummarySubtitle,
                group = "AI & Models",
                iconBadge = {
                    val (bg, fg) = SettingBadges.advancedColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.AutoMirrored.Rounded.ShortText, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = { onNav(Nav.AdvancedSummarySetup) }
            ),
            SearchableSetting(
                id = "bilibili",
                title = "BiliBili Account",
                subtitle = bilibiliSubtitle,
                group = "Services & Tools",
                iconBadge = {
                    val (bg, fg) = SettingBadges.bilibiliColors()
                    SettingIconBadge(bg, fg) {
                        Icon(painterResource(id = R.drawable.bilibili), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = {
                    if (!sessDataValid) {
                        onShowBiliBiliLoginSheet()
                    }
                },
                onLongClick = {
                    if (sessDataValid) {
                        onShowClearSessDataDialog()
                    }
                }
            ),
            SearchableSetting(
                id = "auto_extract_url",
                title = useAutoExtractLinkTitle,
                subtitle = useAutoExtractLinkSubtitle,
                group = "Services & Tools",
                iconBadge = {
                    val (bg, fg) = SettingBadges.linkColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                trailingContent = {
                    Switch(
                        checked = state.autoExtractUrl,
                        onCheckedChange = { actions.onAutoExtractUrlChange(it) }
                    )
                },
                onClick = { actions.onAutoExtractUrlChange(!state.autoExtractUrl) }
            ),
            SearchableSetting(
                id = "tutorial",
                title = tutorialTitle,
                subtitle = tutorialSubtitle,
                group = "Help & Community",
                iconBadge = {
                    val (bg, fg) = SettingBadges.tutorialColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.AutoMirrored.Rounded.HelpCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = { onNav(Nav.Onboarding) }
            ),
            SearchableSetting(
                id = "google_play",
                title = googlePlayTitle,
                subtitle = googlePlaySubtitle,
                group = "Help & Community",
                iconBadge = {
                    val (bg, fg) = SettingBadges.playStoreColors()
                    SettingIconBadge(bg, fg) {
                        Icon(Icons.Rounded.StarRate, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = {
                    val url = "https://play.google.com/store/apps/details?id=${context.packageName}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            ),
            SearchableSetting(
                id = "discord",
                title = discordTitle,
                subtitle = discordSubtitle,
                group = "Help & Community",
                iconBadge = {
                    val (bg, fg) = SettingBadges.discordColors()
                    SettingIconBadge(bg, fg) {
                        Icon(ImageVector.vectorResource(id = R.drawable.discord), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = {
                    val url = "https://discord.gg/WjN73wKTqd"
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            ),
            SearchableSetting(
                id = "github",
                title = repoTitle,
                subtitle = repoSubtitle,
                group = "Help & Community",
                iconBadge = {
                    val (bg, fg) = SettingBadges.githubColors()
                    SettingIconBadge(bg, fg) {
                        Icon(ImageVector.vectorResource(id = R.drawable.github), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = {
                    val url = "https://github.com/kid1412621/SummaryExpressive"
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            )
        )
    }

    val searchState = remember(allItems) {
        SettingsSearchState(initialQuery = query, allItems = allItems)
    }

    return searchState
}
