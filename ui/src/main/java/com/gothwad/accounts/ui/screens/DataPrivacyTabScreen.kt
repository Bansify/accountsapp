package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.AccountToggleRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab

@Composable
fun DataPrivacyTabScreen(
    state: AccountDashboardUiState,
    onOpenSubScreen: (AccountSubScreen) -> Unit,
    onSelectTab: (AccountTab) -> Unit,
    onTogglePersonalizedAds: (Boolean) -> Unit,
    onToggleLocationSharing: (Boolean) -> Unit,
    onTogglePartnerSharing: (Boolean) -> Unit,
    onOpenTakeout: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
) {
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Data & privacy",
                subtitle = "Key privacy options to help you choose the data saved in your Account, the ads you see, and info you share.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Privacy Checkup Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusPill(
                                label = "${state.privacyCheckupStepsDone.size}/4 steps done",
                                tone = if (state.privacyCheckupStepsDone.size == 4) StatusTone.VERIFIED else StatusTone.WARNING,
                            )
                            Text(
                                text = "Privacy Checkup",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Walk through your activity controls, ad settings, and sharing options in 2 minutes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_privacy_checkup),
                            contentDescription = "Privacy checkup illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = { onOpenSubScreen(AccountSubScreen.PrivacyCheckup) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("privacy_start_checkup_button"),
                    ) {
                        Text(
                            text = "Start Privacy Checkup",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // History settings (Things you've done and places you've been)
        item {
            AccountSectionCard(
                title = "Things you've done and places you've been",
                subtitle = "Your activity helps deliver faster searches, smarter maps, and better recommendations.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                state.activityControls.forEachIndexed { index, control ->
                    val icon = when (control.id) {
                        "web_app" -> Icons.Rounded.History
                        "timeline" -> Icons.Rounded.LocationOn
                        else -> Icons.Rounded.PlayCircle
                    }
                    AccountSettingRow(
                        label = control.title,
                        value = if (control.isEnabled) "Saving activity" else "Paused",
                        supportingText = control.autoDeleteLabel,
                        icon = icon,
                        statusBadge = {
                            StatusPill(
                                label = if (control.isEnabled) "On" else "Paused",
                                tone = if (control.isEnabled) StatusTone.VERIFIED else StatusTone.PAUSED,
                            )
                        },
                        testTag = "privacy_control_${control.id}",
                        onClick = { onOpenSubScreen(AccountSubScreen.ActivityControlDetail(control.id)) },
                    )
                    if (index < state.activityControls.lastIndex) {
                        SectionDivider()
                    }
                }
            }
        }

        // Personalized ads
        item {
            AccountSectionCard(
                title = "Personalized ads",
                subtitle = "Choose whether your activity and profile info can be used to personalize ads.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountToggleRow(
                    title = "My Ad Center",
                    subtitle = if (state.personalizedAdsEnabled) {
                        "Personalized ads are On • Tailored topics & brands"
                    } else {
                        "Personalized ads are Off • You will still see general ads"
                    },
                    icon = Icons.Rounded.AdsClick,
                    checked = state.personalizedAdsEnabled,
                    testTag = "privacy_ads_switch",
                    onCheckedChange = onTogglePersonalizedAds,
                )
            }
        }

        // Info you can share with others
        item {
            AccountSectionCard(
                title = "Info you can share with others",
                subtitle = "Control what others can see across services and shared albums.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Profile",
                    value = if (state.activeAccount.profilePubliclyVisible) "Visible to anyone" else "Restricted",
                    icon = Icons.Rounded.Visibility,
                    onClick = { onSelectTab(AccountTab.PERSONAL_INFO) },
                )
                SectionDivider()
                AccountToggleRow(
                    title = "Location sharing",
                    subtitle = if (state.locationSharingEnabled) "Sharing real-time location with family" else "Not sharing location with anyone",
                    icon = Icons.Rounded.ShareLocation,
                    checked = state.locationSharingEnabled,
                    testTag = "privacy_location_sharing_switch",
                    onCheckedChange = onToggleLocationSharing,
                )
                SectionDivider()
                AccountToggleRow(
                    title = "Partner sharing in Photos",
                    subtitle = if (state.partnerSharingEnabled) "Sharing recent photos with Ananya Gothwad" else "Paused",
                    icon = Icons.Rounded.PhotoLibrary,
                    checked = state.partnerSharingEnabled,
                    testTag = "privacy_partner_sharing_switch",
                    onCheckedChange = onTogglePartnerSharing,
                )
            }
        }

        // Data from apps and services you use
        item {
            AccountSectionCard(
                title = "Data from apps and services you use",
                subtitle = "Manage connected third-party apps, export a copy of your data, or delete services.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Third-party apps & services",
                    value = "${state.thirdPartyApps.size} connected apps",
                    icon = Icons.Rounded.Apps,
                    onClick = { onOpenSubScreen(AccountSubScreen.ThirdPartyApps) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Download or delete your data",
                    value = "Download your data (Takeout)",
                    supportingText = "Create an archive of Drive, Mail, Photos, and Contacts",
                    icon = Icons.Rounded.Download,
                    testTag = "privacy_takeout_row",
                    onClick = onOpenTakeout,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Account lifecycle",
                    value = "Delete your Account",
                    supportingText = "Permanently remove your Account and all data",
                    icon = Icons.Rounded.DeleteForever,
                    testTag = "privacy_delete_account_row",
                    onClick = onOpenDeleteAccount,
                )
            }
        }
    }
}
