package me.nanova.summaryexpressive.ui.page.settings.section

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
import me.nanova.summaryexpressive.vm.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Group 1: Display & Interface Settings
 */
@Composable
fun DisplaySettingsGroup(
    state: SettingsUiState,
    actions: SettingsActions,
    onShowThemeDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val themeSubtitle = when (state.theme) {
        1 -> stringResource(id = R.string.darkTheme)
        2 -> stringResource(id = R.string.lightTheme)
        else -> stringResource(id = R.string.systemTheme)
    }

    SettingsGroup(modifier = modifier) {
        // Choose Language
        SettingItem(
            title = stringResource(id = R.string.chooseLanguage),
            subtitle = stringResource(id = R.string.chooseLanguageDescription),
            position = GroupPosition.TOP,
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
        )

        // Theme
        SettingItem(
            title = stringResource(id = R.string.theme),
            subtitle = themeSubtitle,
            position = GroupPosition.MIDDLE,
            iconBadge = {
                val (bg, fg) = SettingBadges.themeColors()
                SettingIconBadge(bg, fg) {
                    Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            onClick = onShowThemeDialog
        )

        // Dynamic Color
        SettingItem(
            title = stringResource(id = R.string.useDynamicColor),
            subtitle = stringResource(id = R.string.useDynamicColorDescription),
            position = GroupPosition.BOTTOM,
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
        )
    }
}

/**
 * Group 2: AI & Models Settings
 */
@Composable
fun AISettingsGroup(
    state: SettingsUiState,
    highlighted: Boolean,
    onShowAIProviderDialog: () -> Unit,
    onShowModelDialog: () -> Unit,
    onNav: (Nav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val aiProviderSubtitle = state.activeProvider?.name
        ?: stringResource(id = R.string.setAIProviderDescription)

    val modelSubtitle = state.activeModel
        ?: stringResource(id = R.string.setModelDescription)

    SettingsGroup(modifier = modifier) {
        // AI Provider
        SettingItem(
            title = stringResource(id = R.string.setAIProvider),
            subtitle = aiProviderSubtitle,
            position = GroupPosition.TOP,
            highlighted = highlighted,
            iconBadge = {
                val (bg, fg) = SettingBadges.aiColors()
                SettingIconBadge(bg, fg) {
                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            onClick = onShowAIProviderDialog
        )

        // LLM Model
        SettingItem(
            title = stringResource(id = R.string.setModel),
            subtitle = modelSubtitle,
            position = GroupPosition.MIDDLE,
            highlighted = highlighted,
            iconBadge = {
                val (bg, fg) = SettingBadges.modelColors()
                SettingIconBadge(bg, fg) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            onClick = onShowModelDialog
        )

        // Advanced Summary Setup
        SettingItem(
            title = stringResource(id = R.string.advancedSummarySetup),
            subtitle = stringResource(id = R.string.advancedSummarySetupDescription),
            position = GroupPosition.BOTTOM,
            highlighted = highlighted,
            iconBadge = {
                val (bg, fg) = SettingBadges.advancedColors()
                SettingIconBadge(bg, fg) {
                    Icon(Icons.AutoMirrored.Rounded.ShortText, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            onClick = { onNav(Nav.AdvancedSummarySetup) }
        )
    }
}

/**
 * Group 3: Services & Tools Settings
 */
@Composable
fun ServicesSettingsGroup(
    state: SettingsUiState,
    actions: SettingsActions,
    highlighted: Boolean,
    onShowBiliBiliLoginSheet: () -> Unit,
    onShowClearSessDataDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

    SettingsGroup(modifier = modifier) {
        // BiliBili Account
        SettingItem(
            title = "BiliBili Account",
            subtitle = bilibiliSubtitle,
            position = GroupPosition.TOP,
            highlighted = highlighted,
            iconBadge = {
                val (bg, fg) = SettingBadges.bilibiliColors()
                SettingIconBadge(bg, fg) {
                    Icon(painterResource(id = R.drawable.bilibili), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            enabled = true,
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
        )

        // Auto Extract Link
        SettingItem(
            title = stringResource(id = R.string.useAutoExtractLink),
            subtitle = stringResource(id = R.string.useAutoExtractLinkDescription),
            position = GroupPosition.BOTTOM,
            highlighted = highlighted,
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
        )
    }
}

/**
 * Group 4: Help & Community Settings
 */
@Composable
fun HelpSettingsGroup(
    onNav: (Nav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    SettingsGroup(modifier = modifier) {
        // Tutorial
        SettingItem(
            title = stringResource(id = R.string.tutorial),
            subtitle = stringResource(id = R.string.tutorialDescription),
            position = GroupPosition.TOP,
            iconBadge = {
                val (bg, fg) = SettingBadges.tutorialColors()
                SettingIconBadge(bg, fg) {
                    Icon(Icons.AutoMirrored.Rounded.HelpCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            },
            onClick = { onNav(Nav.Onboarding) }
        )

        // Rate on Google Play
        SettingItem(
            title = stringResource(id = R.string.googlePlay),
            subtitle = stringResource(id = R.string.googlePlayDescription),
            position = GroupPosition.MIDDLE,
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
        )

        // Discord
        SettingItem(
            title = stringResource(id = R.string.discord),
            subtitle = stringResource(id = R.string.discordDescription),
            position = GroupPosition.MIDDLE,
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
        )

        // Open Source Code (GitHub)
        SettingItem(
            title = stringResource(id = R.string.repository),
            subtitle = stringResource(id = R.string.githubDescription),
            position = GroupPosition.BOTTOM,
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
    }
}
