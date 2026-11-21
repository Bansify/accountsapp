package com.bansify.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bansify.accounts.ui.LocalAccountColors
import com.bansify.accounts.ui.R
import com.bansify.accounts.ui.components.AccountSectionCard
import com.bansify.accounts.ui.components.AccountSettingRow
import com.bansify.accounts.ui.components.SectionDivider
import com.bansify.accounts.ui.components.StatusPill
import com.bansify.accounts.ui.components.StatusTone
import com.bansify.accounts.ui.components.TabIntroHeader
import com.bansify.accounts.ui.model.AccountTab
import com.bansify.accounts.ui.model.NavigationLayoutMode
import com.bansify.accounts.ui.model.ThemePreference

@Composable
fun AppSettingsScreen(
    themePreference: ThemePreference,
    navigationLayout: NavigationLayoutMode,
    packageName: String,
    installedPeersCount: Int,
    onSelectTheme: (ThemePreference) -> Unit,
    onSelectNavigationLayout: (NavigationLayoutMode) -> Unit,
    onOpenHelpLegal: () -> Unit,
) {
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_settings_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_settings_title),
                subtitle = "Choose your theme palette and how you switch sections across Bansify Accounts.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Theme & Color Palette Options
        item {
            AccountSectionCard(
                title = stringResource(R.string.gothwad_theme_section),
                subtitle = "Light uses pure #FFFFFF tones; Dark uses neutral #212121 tones matching your system bars.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeOptionCard(
                        title = stringResource(R.string.gothwad_theme_system),
                        subtitle = "Automatically follows your Android device theme",
                        icon = Icons.Rounded.PhoneAndroid,
                        swatchType = ThemePreference.SYSTEM,
                        selected = themePreference == ThemePreference.SYSTEM,
                        testTag = "settings_theme_system",
                        onClick = { onSelectTheme(ThemePreference.SYSTEM) },
                    )
                    ThemeOptionCard(
                        title = stringResource(R.string.gothwad_theme_light),
                        subtitle = "Pure #FFFFFF canvas with #F8F8F8 layered cards",
                        icon = Icons.Rounded.LightMode,
                        swatchType = ThemePreference.LIGHT,
                        selected = themePreference == ThemePreference.LIGHT,
                        testTag = "settings_theme_light",
                        onClick = { onSelectTheme(ThemePreference.LIGHT) },
                    )
                    ThemeOptionCard(
                        title = stringResource(R.string.gothwad_theme_dark),
                        subtitle = "Neutral #212121 canvas with #282828 layered cards",
                        icon = Icons.Rounded.DarkMode,
                        swatchType = ThemePreference.DARK,
                        selected = themePreference == ThemePreference.DARK,
                        testTag = "settings_theme_dark",
                        onClick = { onSelectTheme(ThemePreference.DARK) },
                    )
                }
            }
        }

        // Navigation Layout Options
        item {
            AccountSectionCard(
                title = stringResource(R.string.gothwad_nav_section),
                subtitle = "Switch between scrollable header pills, a left sidebar, or a 5-tab bottom bar with Others.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NavigationOptionCard(
                        title = stringResource(R.string.gothwad_nav_header_pills),
                        subtitle = "Horizontal pill strip under the top bar for all 6 sections",
                        icon = Icons.Rounded.ViewCarousel,
                        selected = navigationLayout == NavigationLayoutMode.HEADER_PILLS,
                        testTag = "settings_nav_header_pills",
                        onClick = { onSelectNavigationLayout(NavigationLayoutMode.HEADER_PILLS) },
                    )
                    NavigationOptionCard(
                        title = stringResource(R.string.gothwad_nav_left_sidebar),
                        subtitle = "Slide-out left sidebar drawer on phones, pinned rail on tablets",
                        icon = Icons.Rounded.ViewSidebar,
                        selected = navigationLayout == NavigationLayoutMode.LEFT_SIDEBAR,
                        testTag = "settings_nav_left_sidebar",
                        onClick = { onSelectNavigationLayout(NavigationLayoutMode.LEFT_SIDEBAR) },
                    )
                    NavigationOptionCard(
                        title = stringResource(R.string.gothwad_nav_bottom_bar),
                        subtitle = "5 bottom tabs: Home, Personal, Security, Privacy & Others (People + Apps)",
                        icon = Icons.Rounded.ViewAgenda,
                        selected = navigationLayout == NavigationLayoutMode.BOTTOM_BAR,
                        testTag = "settings_nav_bottom_bar",
                        onClick = { onSelectNavigationLayout(NavigationLayoutMode.BOTTOM_BAR) },
                    )
                }
            }
        }

        // Help & App Info
        item {
            AccountSectionCard(
                title = stringResource(R.string.gothwad_about),
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = stringResource(R.string.gothwad_help_title),
                    value = "Privacy, Terms & legal",
                    supportingText = "View Bansify Privacy Policy, Terms of Service, and web help",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    testTag = "settings_help_legal_row",
                    onClick = onOpenHelpLegal,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Application package",
                    value = packageName,
                    supportingText = "Verified sibling peer apps on device: $installedPeersCount",
                    icon = Icons.Rounded.Info,
                )
            }
        }
    }
}

@Composable
fun OthersHubScreen(
    themePreference: ThemePreference,
    installedPeersCount: Int,
    onOpenTab: (AccountTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelpLegal: () -> Unit,
) {
    val themeSummary = when (themePreference) {
        ThemePreference.SYSTEM -> stringResource(R.string.gothwad_theme_system)
        ThemePreference.LIGHT -> stringResource(R.string.gothwad_theme_light)
        ThemePreference.DARK -> stringResource(R.string.gothwad_theme_dark)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("others_hub_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_others),
                subtitle = "People & sharing, connected Bansify apps, and appearance settings.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        item {
            AccountSectionCard(
                title = "More account sections",
                subtitle = "Kept neatly here so the bottom bar stays clean with 5 tabs.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Directory & shared identity",
                    value = stringResource(R.string.gothwad_tab_people),
                    supportingText = "Bansify Contacts status and how others see your profile",
                    icon = Icons.Rounded.Group,
                    testTag = "others_people_sharing_row",
                    onClick = { onOpenTab(AccountTab.PEOPLE_SHARING) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Ecosystem & OAuth permissions",
                    value = stringResource(R.string.gothwad_tab_apps),
                    supportingText = "Authorized apps, OAuth grants, and $installedPeersCount verified device app(s)",
                    icon = Icons.Rounded.Apps,
                    testTag = "others_connected_apps_row",
                    onClick = { onOpenTab(AccountTab.CONNECTED_APPS) },
                )
            }
        }

        item {
            AccountSectionCard(
                title = "Preferences & support",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = stringResource(R.string.gothwad_settings_title),
                    value = "Theme & navigation layout",
                    supportingText = "Theme: $themeSummary · Layout: Bottom bar",
                    icon = Icons.Rounded.Settings,
                    statusBadge = { StatusPill("Customize", StatusTone.NEUTRAL) },
                    testTag = "others_settings_row",
                    onClick = onOpenSettings,
                )
                SectionDivider()
                AccountSettingRow(
                    label = stringResource(R.string.gothwad_help_title),
                    value = "Privacy, Terms & about",
                    supportingText = "Open Bansify Privacy Policy, Terms of Service & device details",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    testTag = "others_help_row",
                    onClick = onOpenHelpLegal,
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    swatchType: ThemePreference,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else semantic.cardBorder

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemePaletteSwatch(swatchType = swatchType, borderColor = semantic.cardBorder)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun ThemePaletteSwatch(
    swatchType: ThemePreference,
    borderColor: Color,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (swatchType) {
            ThemePreference.LIGHT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B57D0)),
                )
            }

            ThemePreference.DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121)),
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA8C7FA)),
                )
            }

            ThemePreference.SYSTEM -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.White),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF212121)),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else semantic.cardBorder

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
