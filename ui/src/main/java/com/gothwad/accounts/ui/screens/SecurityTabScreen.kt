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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.gothwad.accounts.ui.model.AccountTab

@Composable
fun SecurityTabScreen(
    account: GothwadAccount,
    localAccountsCount: Int,
    installedVerifiedPeers: List<String>,
    onManageSecurityOnWeb: () -> Unit,
    onRequestSignOutOrRemove: (GothwadAccount) -> Unit,
    onNavigateTab: (AccountTab) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("security_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_security),
                subtitle = "Sign-in credentials, multi-factor verification, passkeys, and active sessions for ${account.displayEmail()}.",
            )
        }

        // How you sign in (exact parity with /me#security)
        item {
            AccountSectionCard(
                title = "How you sign in to Gothwad",
                subtitle = "Credentials stay in your browser session on accounts.gothwadtech.com and are never stored in native apps.",
            ) {
                AccountSettingRow(
                    label = "Password",
                    value = "Add or update password",
                    supportingText = "Protected with PBKDF2-SHA256 / server session",
                    icon = Icons.Rounded.Password,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "2-Step Verification",
                    value = "Authenticator app (TOTP) & backup codes",
                    supportingText = "Configure or manage TOTP recovery codes on Accounts web",
                    icon = Icons.Rounded.PhonelinkLock,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Passkeys",
                    value = "Biometric & hardware security keys (WebAuthn)",
                    supportingText = "Enroll or remove passkeys for passwordless login",
                    icon = Icons.Rounded.Fingerprint,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Google accounts",
                    value = "Linked Google sign-in accounts (up to 3)",
                    supportingText = "Link or unlink Google login methods on Accounts web",
                    icon = Icons.Rounded.Link,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery email",
                    value = "Manage verified backup email",
                    icon = Icons.Rounded.MarkEmailRead,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
            }
        }

        // Devices & sessions (separating browser sessions vs this app vs this device)
        item {
            AccountSectionCard(
                title = "Your devices & sessions",
                subtitle = "Each Gothwad app holds its own hardware-backed OAuth tokens on this phone.",
            ) {
                AccountSettingRow(
                    label = "Web & browser sessions",
                    value = "Review active sessions & sign out other browsers",
                    supportingText = "Opens Security & devices on accounts.gothwadtech.com",
                    icon = Icons.Rounded.Devices,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Verified Gothwad apps on this device",
                    value = if (installedVerifiedPeers.isEmpty()) {
                        "This app only"
                    } else {
                        "${installedVerifiedPeers.size + 1} verified apps on device"
                    },
                    supportingText = if (installedVerifiedPeers.isEmpty()) {
                        "No other verified sibling Gothwad apps detected"
                    } else {
                        installedVerifiedPeers.joinToString(", ")
                    },
                    icon = Icons.Rounded.Smartphone,
                    statusBadge = {
                        StatusPill(
                            label = "$localAccountsCount local",
                            tone = StatusTone.VERIFIED,
                        )
                    },
                )
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    OutlinedButton(
                        onClick = { onRequestSignOutOrRemove(account) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_sign_out_or_remove_button"),
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Text("  Sign out of app or remove from device")
                    }
                }
            }
        }

        // Security activity & Connected apps
        item {
            AccountSectionCard(title = "Activity & app permissions") {
                AccountSettingRow(
                    label = "Recent security activity",
                    value = "View recent sign-ins and security events",
                    icon = Icons.Rounded.History,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Signing in with other sites",
                    value = "Manage Gothwad OAuth connections",
                    icon = Icons.Rounded.Apps,
                    onClick = { onNavigateTab(AccountTab.CONNECTED_APPS) },
                )
            }
        }
    }
}
