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
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.components.displayEmail
import com.gothwad.accounts.ui.components.displayName
import com.gothwad.accounts.ui.model.AccountTab

@Composable
fun SecurityTabScreen(
    account: GothwadAccount,
    localAccounts: List<GothwadAccount>,
    installedVerifiedPeers: List<String>,
    canManageNatively: Boolean = false,
    webSessions: List<WebSessionInfo> = emptyList(),
    activityFeed: List<AccountActivityEntry> = emptyList(),
    onRevokeOtherWebSessions: () -> Unit = {},
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
                    value = when (account.hasPassword) {
                        true -> "Password configured"
                        false -> "No password set"
                        null -> "Add or update password"
                    },
                    supportingText = account.passwordChangedAt?.let { "Last changed $it" }
                        ?: "Protected with PBKDF2-SHA256 / server session",
                    icon = Icons.Rounded.Password,
                    statusBadge = account.hasPassword?.let { set ->
                        { StatusPill(if (set) "Set" else "Not set", if (set) StatusTone.VERIFIED else StatusTone.WARNING) }
                    },
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "2-Step Verification",
                    value = when (account.mfaEnabled) {
                        true -> "Authenticator app (TOTP) active"
                        false -> "Not enabled yet"
                        null -> "Authenticator app (TOTP) & backup codes"
                    },
                    supportingText = "Configure or manage TOTP recovery codes on Accounts web",
                    icon = Icons.Rounded.PhonelinkLock,
                    statusBadge = account.mfaEnabled?.let { enabled ->
                        { StatusPill(if (enabled) "On" else "Off", if (enabled) StatusTone.VERIFIED else StatusTone.WARNING) }
                    },
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Passkeys",
                    value = account.passkeyCount?.let { count ->
                        if (count > 0) "$count passkey(s) enrolled" else "No passkeys enrolled"
                    } ?: "Biometric & hardware security keys (WebAuthn)",
                    supportingText = "Enroll or remove passkeys for passwordless login",
                    icon = Icons.Rounded.Fingerprint,
                    statusBadge = account.passkeyCount?.let { count ->
                        { StatusPill("$count", if (count > 0) StatusTone.VERIFIED else StatusTone.NEUTRAL) }
                    },
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery email",
                    value = account.recoveryEmail?.takeIf { it.isNotBlank() } ?: "Manage verified backup email",
                    icon = Icons.Rounded.MarkEmailRead,
                    statusBadge = account.recoveryEmailVerified?.let { verified ->
                        { StatusPill(if (verified) "Verified" else "Unverified", if (verified) StatusTone.VERIFIED else StatusTone.WARNING) }
                    },
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
                    value = when {
                        webSessions.isNotEmpty() -> "${webSessions.size} active web session(s)"
                        account.activeSessionsCount != null -> "${account.activeSessionsCount} active web session(s)"
                        else -> "Review active sessions & sign out other browsers"
                    },
                    supportingText = "Opens Security & devices on accounts.gothwadtech.com",
                    icon = Icons.Rounded.Devices,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageSecurityOnWeb,
                )
                if (canManageNatively && webSessions.isNotEmpty()) {
                    webSessions.take(5).forEach { ws ->
                        SectionDivider()
                        val label = listOfNotNull(ws.browser, ws.os).joinToString(" · ")
                            .ifBlank { ws.deviceName ?: "Browser session" }
                        AccountSettingRow(
                            label = if (ws.current) "Current browser session" else "Web session",
                            value = label,
                            supportingText = listOfNotNull(ws.ipAddress, ws.lastActive).joinToString(" · "),
                            icon = Icons.Rounded.Devices,
                            statusBadge = if (ws.current) {
                                { StatusPill("Current", StatusTone.VERIFIED) }
                            } else null,
                        )
                    }
                    if (webSessions.count { !it.current } > 0 || webSessions.size > 1) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            OutlinedButton(
                                onClick = onRevokeOtherWebSessions,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign out all other web sessions")
                            }
                        }
                    }
                }
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
                            label = "${localAccounts.size} local",
                            tone = StatusTone.VERIFIED,
                        )
                    },
                )
                if (localAccounts.size > 1) {
                    localAccounts.forEach { acc ->
                        SectionDivider()
                        AccountSettingRow(
                            label = if (acc.userId == account.userId) "Active local account" else "Signed-in local account",
                            value = "${acc.displayName()} (${acc.displayEmail()})",
                            icon = Icons.Rounded.Person,
                            onClick = { onRequestSignOutOrRemove(acc) },
                        )
                    }
                }
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
                        Text("  Sign out, remove, or disconnect app")
                    }
                }
            }
        }

        // Security activity & Connected apps
        item {
            AccountSectionCard(title = "Activity & app permissions") {
                if (canManageNatively && activityFeed.isNotEmpty()) {
                    activityFeed.take(5).forEachIndexed { idx, entry ->
                        if (idx > 0) SectionDivider()
                        AccountSettingRow(
                            label = entry.title,
                            value = entry.detail ?: entry.type,
                            supportingText = listOfNotNull(entry.ipAddress, entry.createdAt).joinToString(" · "),
                            icon = Icons.Rounded.History,
                        )
                    }
                    SectionDivider()
                } else {
                    AccountSettingRow(
                        label = "Recent security activity",
                        value = "View recent sign-ins and security events",
                        icon = Icons.Rounded.History,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = onManageSecurityOnWeb,
                    )
                    SectionDivider()
                }
                AccountSettingRow(
                    label = "Connected apps & OAuth grants",
                    value = account.authorizationCount?.let { count ->
                        "$count authorized app(s)"
                    } ?: "Manage Gothwad OAuth connections",
                    icon = Icons.Rounded.Apps,
                    onClick = { onNavigateTab(AccountTab.CONNECTED_APPS) },
                )
            }
        }
    }
}
