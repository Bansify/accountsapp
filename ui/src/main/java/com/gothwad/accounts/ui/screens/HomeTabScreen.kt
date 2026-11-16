package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountAvatar
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.SegmentedStorageBar
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab

@Composable
fun HomeTabScreen(
    state: AccountDashboardUiState,
    onAvatarEditClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectTab: (AccountTab) -> Unit,
    onOpenSubScreen: (AccountSubScreen) -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val account = state.activeAccount
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Profile Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AccountAvatar(
                    account = account,
                    size = 86.dp,
                    showGoogleRing = true,
                    showEditCameraBadge = true,
                    onClick = onAvatarEditClick,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Welcome, ${account.name}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${account.email}  •  Manage your info, privacy, and security",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Quick search pill bar
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenSearch)
                        .testTag("home_search_bar"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search Account",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Privacy & personalization hero card
        item {
            HeroFeatureCard(
                title = "Privacy & personalization",
                description = "See the data in your Account and choose what activity is saved to personalize your experience.",
                actionLabel = "Manage your data & privacy",
                imageRes = R.drawable.img_privacy_checkup,
                imageDescription = "Privacy checkup illustration",
                testTag = "home_privacy_card_button",
                onActionClick = { onSelectTab(AccountTab.DATA_PRIVACY) },
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Security tips hero card
        item {
            val unresolved = state.unresolvedSecurityCount
            HeroFeatureCard(
                title = if (unresolved == 0) "Your account is protected" else "We found $unresolved security recommendation${if (unresolved > 1) "s" else ""}",
                description = if (unresolved == 0) {
                    "Security Checkup found no issues. Passkeys and 2-Step Verification are actively protecting ${account.email}."
                } else {
                    "The Security Checkup keeps your Account safe with personalized recommendations and sign-in alerts."
                },
                actionLabel = "Review security tips",
                imageRes = R.drawable.img_security_shield,
                imageDescription = "Account security shield illustration",
                badge = {
                    StatusPill(
                        label = if (unresolved == 0) "Protected" else "$unresolved action needed",
                        tone = if (unresolved == 0) StatusTone.VERIFIED else StatusTone.WARNING,
                    )
                },
                testTag = "home_security_card_button",
                onActionClick = { onOpenSubScreen(AccountSubScreen.SecurityCheckup) },
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Account Storage card
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Account storage",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${String.format("%.1f", state.storageUsedGb)} GB of ${state.storageTotalGb.toInt()} GB used",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            SegmentedStorageBar(
                                driveGb = state.driveStorageGb,
                                mailGb = state.mailStorageGb,
                                photosGb = state.photosStorageGb,
                                totalGb = state.storageTotalGb,
                            )
                            Text(
                                text = "Shared across Drive, Mail, and Photos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_cloud_storage),
                            contentDescription = "Cloud storage illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = { onOpenSubScreen(AccountSubScreen.StorageDetails) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("home_manage_storage_button"),
                    ) {
                        Text(
                            text = "Manage storage",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Privacy Checkup + Password Manager quick links
        item {
            AccountSectionCard(
                title = "Take the Privacy Checkup",
                subtitle = "Step-by-step guide to choose the privacy settings that are right for you.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Privacy Checkup",
                    value = "${state.privacyCheckupStepsDone.size} of 4 steps reviewed",
                    icon = Icons.Rounded.VerifiedUser,
                    testTag = "home_privacy_checkup_row",
                    onClick = { onOpenSubScreen(AccountSubScreen.PrivacyCheckup) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Password Manager",
                    value = "${state.savedPasswords.size} saved passwords",
                    icon = Icons.Rounded.Security,
                    supportingText = "All saved passwords are strong and secure",
                    testTag = "home_password_manager_row",
                    onClick = { onOpenSubScreen(AccountSubScreen.PasswordManager) },
                )
            }
        }

        // Looking for something else
        item {
            AccountSectionCard(
                title = "Looking for something else?",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Quick finder",
                    value = "Search Account",
                    icon = Icons.Rounded.Search,
                    onClick = onOpenSearch,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Support",
                    value = "See help options",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    onClick = { onOpenSubScreen(AccountSubScreen.HelpSupport) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Improve Accounts",
                    value = "Send feedback",
                    icon = Icons.Rounded.Feedback,
                    onClick = onOpenFeedback,
                )
            }
        }

        // Bottom privacy reassurance footer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Only you can see your settings. You can also review your settings for Maps, Search, or whichever services you use most. Your data stays encrypted and private.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroFeatureCard(
    title: String,
    description: String,
    actionLabel: String,
    imageRes: Int,
    imageDescription: String,
    testTag: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
) {
    val semantic = LocalAccountColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (badge != null) {
                        badge()
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = imageDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
            }
            HorizontalDivider(color = semantic.cardBorder)
            TextButton(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag(testTag),
            ) {
                Text(
                    text = actionLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
