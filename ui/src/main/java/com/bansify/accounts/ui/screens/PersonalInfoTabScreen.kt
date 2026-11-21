package com.bansify.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bansify.accounts.core.GothwadAccount
import com.bansify.accounts.ui.LocalAccountColors
import com.bansify.accounts.ui.R
import com.bansify.accounts.ui.components.AccountAvatar
import com.bansify.accounts.ui.components.AccountSectionCard
import com.bansify.accounts.ui.components.AccountSettingRow
import com.bansify.accounts.ui.components.SectionDivider
import com.bansify.accounts.ui.components.StatusPill
import com.bansify.accounts.ui.components.StatusTone
import com.bansify.accounts.ui.components.TabIntroHeader
import com.bansify.accounts.ui.components.displayEmail
import com.bansify.accounts.ui.components.displayName
import com.bansify.accounts.ui.components.initials

@Composable
fun PersonalInfoTabScreen(
    account: GothwadAccount,
    canEditBasicProfileNatively: Boolean = false,
    onEditAvatarClick: () -> Unit = {},
    onEditBasicProfileClick: () -> Unit = {},
    onEditPhoneClick: () -> Unit = {},
    onEditRecoveryEmailClick: () -> Unit = {},
    onManagePersonalOnWeb: () -> Unit,
) {
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("personal_info_tab_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_personal),
                subtitle = "Basic identity and contact details stored on your Bansify Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Identity preview card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountAvatar(
                        displayName = account.displayName(),
                        initials = account.initials(),
                        avatarUrl = account.avatarUrl,
                        size = 52.dp,
                        showBrandRing = true,
                        onClick = if (canEditBasicProfileNatively) onEditAvatarClick else onManagePersonalOnWeb,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = account.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = account.displayEmail(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        account.username?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "@$it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (canEditBasicProfileNatively) {
                        Button(
                            onClick = onEditBasicProfileClick,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Edit", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Basic info
        item {
            AccountSectionCard(
                title = "Basic info",
                subtitle = if (canEditBasicProfileNatively) {
                    "Manage your photo, name, birthday, and gender right inside this app."
                } else {
                    "Changes saved on accounts.bansify.com sync automatically to this app."
                },
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Profile photo",
                    value = if (account.avatarUrl != null) "Photo set" else "Initials avatar",
                    supportingText = if (canEditBasicProfileNatively) {
                        "Upload a photo from your device or remove your existing photo"
                    } else {
                        "Update or remove your profile photo on Accounts web"
                    },
                    icon = Icons.Rounded.PhotoCamera,
                    trailingIcon = if (canEditBasicProfileNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canEditBasicProfileNatively) onEditAvatarClick else onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Name",
                    value = account.displayName(),
                    icon = Icons.Rounded.Person,
                    trailingIcon = if (canEditBasicProfileNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canEditBasicProfileNatively) onEditBasicProfileClick else onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Username",
                    value = account.username?.let { "@$it" } ?: "Not set",
                    icon = Icons.Rounded.Badge,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Date of birth",
                    value = account.dateOfBirth?.takeIf { it.isNotBlank() } ?: "Not set",
                    supportingText = "Optional personal detail stored on Bansify Accounts",
                    icon = Icons.Rounded.Cake,
                    trailingIcon = if (canEditBasicProfileNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canEditBasicProfileNatively) onEditBasicProfileClick else onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Gender",
                    value = account.gender?.takeIf { it.isNotBlank() } ?: "Not set",
                    supportingText = "Optional personal detail stored on Bansify Accounts",
                    icon = Icons.Rounded.Wc,
                    trailingIcon = if (canEditBasicProfileNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canEditBasicProfileNatively) onEditBasicProfileClick else onManagePersonalOnWeb,
                )
            }
        }

        // Contact info
        item {
            AccountSectionCard(
                title = "Contact info",
                subtitle = "Account identifiers and recovery channels",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Account email",
                    value = account.displayEmail(),
                    supportingText = "Account address at bansify.com; mailbox availability is managed by Mail",
                    icon = Icons.Rounded.AlternateEmail,
                    statusBadge = { StatusPill("Fixed", StatusTone.NEUTRAL) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Phone number",
                    value = account.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set",
                    supportingText = "Used as a sign-in identifier when set",
                    icon = Icons.Rounded.Phone,
                    statusBadge = account.phoneNumberVerified?.takeIf { !account.phoneNumber.isNullOrBlank() }?.let { verified ->
                        {
                            StatusPill(
                                label = if (verified) "Verified" else "Unverified",
                                tone = if (verified) StatusTone.VERIFIED else StatusTone.WARNING,
                            )
                        }
                    },
                    trailingIcon = if (canEditBasicProfileNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canEditBasicProfileNatively) onEditPhoneClick else onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery email",
                    value = account.recoveryEmail?.takeIf { it.isNotBlank() } ?: "Not set",
                    supportingText = "Used for password resets and security alerts",
                    icon = Icons.Rounded.MarkEmailRead,
                    statusBadge = account.recoveryEmailVerified?.let { verified ->
                        {
                            StatusPill(
                                label = if (verified) "Verified" else "Unverified",
                                tone = if (verified) StatusTone.VERIFIED else StatusTone.WARNING,
                            )
                        }
                    },
                    trailingIcon = if (canEditBasicProfileNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canEditBasicProfileNatively) onEditRecoveryEmailClick else onManagePersonalOnWeb,
                )
            }
        }

        // Honest visibility note matching web /me#personal
        item {
            AccountSectionCard(
                title = "Profile visibility",
                subtitle = "How Bansify services receive your profile",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Sharing information",
                    value = "Service-managed visibility",
                    supportingText = "Your name and avatar are shared only with Bansify services and apps you authorize via OAuth.",
                    icon = Icons.Rounded.Visibility,
                )
                if (!canEditBasicProfileNatively) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        OutlinedButton(
                            onClick = onManagePersonalOnWeb,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.gothwad_manage_web))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
