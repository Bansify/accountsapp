package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.components.displayEmail
import com.gothwad.accounts.ui.components.displayName

@Composable
fun PersonalInfoTabScreen(
    account: GothwadAccount,
    onManagePersonalOnWeb: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("personal_info_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_personal),
                subtitle = "Basic identity and contact details stored on your Gothwad Account.",
            )
        }

        // Basic info
        item {
            AccountSectionCard(
                title = "Basic info",
                subtitle = "Changes saved on accounts.gothwadtech.com sync automatically to this app.",
            ) {
                AccountSettingRow(
                    label = "Profile photo",
                    value = if (account.avatarUrl != null) "Photo set" else "Initials avatar",
                    supportingText = "Update or remove your profile photo on Accounts web",
                    icon = Icons.Rounded.PhotoCamera,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Name",
                    value = account.displayName(),
                    icon = Icons.Rounded.Person,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Username",
                    value = account.username?.let { "@$it" } ?: "Not set",
                    icon = Icons.Rounded.Badge,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Date of birth",
                    value = "Managed on Accounts web",
                    supportingText = "Optional personal detail stored on Gothwad Accounts",
                    icon = Icons.Rounded.Cake,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Gender",
                    value = "Managed on Accounts web",
                    supportingText = "Optional personal detail stored on Gothwad Accounts",
                    icon = Icons.Rounded.Wc,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
            }
        }

        // Contact info
        item {
            AccountSectionCard(
                title = "Contact info",
                subtitle = "Account identifiers and recovery channels",
            ) {
                AccountSettingRow(
                    label = "Account email",
                    value = account.displayEmail(),
                    supportingText = "Account address at gothwadtech.com; mailbox availability is managed by Mail",
                    icon = Icons.Rounded.AlternateEmail,
                    statusBadge = { StatusPill("Fixed", StatusTone.NEUTRAL) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Phone number",
                    value = "View or update on Accounts web",
                    supportingText = "Used as a sign-in identifier when set",
                    icon = Icons.Rounded.Phone,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery email",
                    value = "View or verify on Accounts web",
                    supportingText = "Used for password resets and security alerts",
                    icon = Icons.Rounded.MarkEmailRead,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePersonalOnWeb,
                )
            }
        }

        // Honest visibility note matching web /me#personal
        item {
            AccountSectionCard(
                title = "Profile visibility",
                subtitle = "How Gothwad services receive your profile",
            ) {
                AccountSettingRow(
                    label = "Sharing information",
                    value = "Service-managed visibility",
                    supportingText = "Your name and avatar are shared only with Gothwad services and apps you authorize via OAuth.",
                    icon = Icons.Rounded.Visibility,
                )
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    OutlinedButton(
                        onClick = onManagePersonalOnWeb,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.gothwad_manage_web))
                    }
                }
            }
        }
    }
}
