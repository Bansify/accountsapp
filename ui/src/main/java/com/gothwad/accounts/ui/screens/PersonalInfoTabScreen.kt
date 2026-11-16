package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.components.AccountAvatar
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.AccountToggleRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.EditableField

@Composable
fun PersonalInfoTabScreen(
    state: AccountDashboardUiState,
    onOpenPhotoPicker: () -> Unit,
    onEditField: (EditableField) -> Unit,
    onToggleProfileVisibility: (Boolean) -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenPasskeys: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val account = state.activeAccount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Personal info",
                subtitle = "Info about you and your preferences across services. Some info may be visible to other people.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Basic info
        item {
            AccountSectionCard(
                title = "Basic info",
                subtitle = "Some info may be visible to other people using Gothwad services.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountSettingRow(
                        label = "Profile picture",
                        value = "Add a profile picture to personalize your account",
                        icon = Icons.Rounded.CameraAlt,
                        trailingIcon = null,
                        testTag = "personal_photo_row",
                        onClick = onOpenPhotoPicker,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 76.dp, end = 20.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountAvatar(
                        account = account,
                        size = 52.dp,
                        showGoogleRing = true,
                        showEditCameraBadge = true,
                        onClick = onOpenPhotoPicker,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tap to update photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SectionDivider()
                AccountSettingRow(
                    label = "Name",
                    value = account.name,
                    icon = Icons.Rounded.Person,
                    testTag = "personal_name_row",
                    onClick = { onEditField(EditableField.NAME) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Birthday",
                    value = account.birthday,
                    icon = Icons.Rounded.Cake,
                    testTag = "personal_birthday_row",
                    onClick = { onEditField(EditableField.BIRTHDAY) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Gender",
                    value = account.gender,
                    icon = Icons.Rounded.Wc,
                    testTag = "personal_gender_row",
                    onClick = { onEditField(EditableField.GENDER) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Pronouns",
                    value = account.pronouns,
                    icon = Icons.Rounded.Badge,
                    testTag = "personal_pronouns_row",
                    onClick = { onEditField(EditableField.PRONOUNS) },
                )
            }
        }

        // Contact info
        item {
            AccountSectionCard(
                title = "Contact info",
                subtitle = "Manage your primary email, recovery email, and phone numbers.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Primary email",
                    value = account.email,
                    icon = Icons.Rounded.Email,
                    supportingText = "Primary Account email • Cannot be changed",
                    statusBadge = { StatusPill("Verified", StatusTone.VERIFIED) },
                    trailingIcon = null,
                    onClick = { onShowMessage("${account.email} is your primary account email") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery email",
                    value = account.recoveryEmail,
                    icon = Icons.Rounded.AlternateEmail,
                    testTag = "personal_recovery_email_row",
                    onClick = { onEditField(EditableField.RECOVERY_EMAIL) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Phone",
                    value = account.phone,
                    icon = Icons.Rounded.Phone,
                    supportingText = "Verified for security alerts & 2-Step Verification",
                    testTag = "personal_phone_row",
                    onClick = { onEditField(EditableField.RECOVERY_PHONE) },
                )
            }
        }

        // Addresses
        item {
            AccountSectionCard(
                title = "Addresses",
                subtitle = "Your home and work addresses are used to personalize your experiences across Maps and Calendar.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Home",
                    value = account.homeAddress,
                    icon = Icons.Rounded.Home,
                    testTag = "personal_home_address_row",
                    onClick = { onEditField(EditableField.HOME_ADDRESS) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Work",
                    value = account.workAddress,
                    icon = Icons.Rounded.Work,
                    testTag = "personal_work_address_row",
                    onClick = { onEditField(EditableField.WORK_ADDRESS) },
                )
            }
        }

        // Profiles & visibility
        item {
            AccountSectionCard(
                title = "Your profiles",
                subtitle = "Control what others see about you across services.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountToggleRow(
                    title = "Public profile visibility",
                    subtitle = if (account.profilePubliclyVisible) {
                        "Anyone can see your name, photo, and handle (${account.handle})"
                    } else {
                        "Only you and people you interact with see your profile"
                    },
                    icon = Icons.Rounded.Public,
                    checked = account.profilePubliclyVisible,
                    testTag = "personal_profile_visibility_switch",
                    onCheckedChange = onToggleProfileVisibility,
                )
            }
        }

        // Password & sign-in options
        item {
            AccountSectionCard(
                title = "Password & sign-in preferences",
                subtitle = "Manage your password and passwordless keys.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Password",
                    value = "••••••••••••",
                    icon = Icons.Rounded.Lock,
                    supportingText = "Last changed ${state.passwordLastChanged}",
                    testTag = "personal_password_row",
                    onClick = onOpenChangePassword,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Passkeys",
                    value = "${state.passkeyCount} passkeys active",
                    icon = Icons.Rounded.Key,
                    supportingText = "Sign in faster using fingerprint or face unlock",
                    onClick = onOpenPasskeys,
                )
            }
        }

        // General preferences for the web
        item {
            AccountSectionCard(
                title = "General preferences for the web",
                subtitle = "Manage settings for language and accessibility across services.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Language",
                    value = account.language,
                    icon = Icons.Rounded.Language,
                    testTag = "personal_language_row",
                    onClick = { onEditField(EditableField.LANGUAGE) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Accessibility",
                    value = "High-contrast colors & screen-reader hints",
                    icon = Icons.Rounded.AccessibilityNew,
                    onClick = { onShowMessage("System accessibility preferences are automatically followed") },
                )
            }
        }
    }
}
