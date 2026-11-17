package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.LocalAccountColors
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityTabScreen(
    account: GothwadAccount,
    localAccounts: List<GothwadAccount>,
    installedVerifiedPeers: List<String>,
    canManageNatively: Boolean = false,
    canWriteNatively: Boolean = canManageNatively,
    webSessions: List<WebSessionInfo> = emptyList(),
    activityFeed: List<AccountActivityEntry> = emptyList(),
    onEditPasswordClick: () -> Unit = {},
    onManageTwoFactorClick: () -> Unit = {},
    onEditRecoveryEmailClick: () -> Unit = {},
    onRevokeWebSession: (String) -> Unit = {},
    onRevokeOtherWebSessions: () -> Unit = {},
    onManageSecurityOnWeb: () -> Unit,
    onRequestSignOutOrRemove: (GothwadAccount) -> Unit,
    onNavigateTab: (AccountTab) -> Unit,
) {
    val semantic = LocalAccountColors.current
    val attentionCount = listOfNotNull(
        account.hasPassword?.let { !it },
        account.mfaEnabled?.let { !it },
        account.recoveryEmailVerified?.let { !it },
    ).count { it }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("security_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_security),
                subtitle = "Sign-in credentials, multi-factor verification, passkeys, and active sessions for ${account.displayEmail()}.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Dynamic Security Status Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                            StatusPill(
                                label = when {
                                    attentionCount > 0 -> "$attentionCount item${if (attentionCount > 1) "s" else ""} to review"
                                    account.mfaEnabled == true -> "Strong protection"
                                    else -> "Security status"
                                },
                                tone = when {
                                    attentionCount > 0 -> StatusTone.WARNING
                                    account.mfaEnabled == true -> StatusTone.VERIFIED
                                    else -> StatusTone.NEUTRAL
                                },
                            )
                            Text(
                                text = if (attentionCount == 0 && account.mfaEnabled == true) {
                                    "Your Gothwad Account is protected"
                                } else {
                                    "Security checkup & sign-in"
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Manage password, TOTP 2-Step Verification, passkeys, and active sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_security_shield),
                            contentDescription = "Security checkup illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        account.hasPassword?.let { hasPwd ->
                            StatusPill(
                                label = if (hasPwd) "Password set" else "Add password",
                                tone = if (hasPwd) StatusTone.VERIFIED else StatusTone.WARNING,
                            )
                        }
                        account.mfaEnabled?.let { mfa ->
                            StatusPill(
                                label = if (mfa) "2-Step On" else "2-Step Off",
                                tone = if (mfa) StatusTone.VERIFIED else StatusTone.WARNING,
                            )
                        }
                        account.passkeyCount?.let { count ->
                            StatusPill(
                                label = if (count > 0) "$count Passkey${if (count > 1) "s" else ""}" else "No passkeys",
                                tone = if (count > 0) StatusTone.VERIFIED else StatusTone.NEUTRAL,
                            )
                        }
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = onManageSecurityOnWeb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.gothwad_open_on_web))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // How you sign in
        item {
            AccountSectionCard(
                title = "How you sign in to Gothwad",
                subtitle = if (canManageNatively && canWriteNatively) {
                    "Manage password, 2-Step Verification, and recovery email directly in this app."
                } else {
                    "Manage sign-in credentials on accounts.gothwadtech.com."
                },
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Password",
                    value = when (account.hasPassword) {
                        true -> "Password configured"
                        false -> "No password set"
                        null -> "Add or update password"
                    },
                    supportingText = account.passwordChangedAt?.let { "Last changed $it" }
                        ?: "Protected with PBKDF2-SHA256",
                    icon = Icons.Rounded.Password,
                    statusBadge = account.hasPassword?.let { set ->
                        { StatusPill(if (set) "Set" else "Not set", if (set) StatusTone.VERIFIED else StatusTone.WARNING) }
                    },
                    trailingIcon = if (canManageNatively && canWriteNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively && canWriteNatively) onEditPasswordClick else onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "2-Step Verification",
                    value = when (account.mfaEnabled) {
                        true -> "Authenticator app (TOTP) active"
                        false -> "Not enabled yet"
                        null -> "Authenticator app (TOTP) & backup codes"
                    },
                    supportingText = if (canManageNatively && canWriteNatively) {
                        "Set up, regenerate backup codes, or disable authenticator verification"
                    } else {
                        "Configure or manage TOTP recovery codes on Accounts web"
                    },
                    icon = Icons.Rounded.PhonelinkLock,
                    statusBadge = account.mfaEnabled?.let { enabled ->
                        { StatusPill(if (enabled) "On" else "Off", if (enabled) StatusTone.VERIFIED else StatusTone.WARNING) }
                    },
                    trailingIcon = if (canManageNatively && canWriteNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively && canWriteNatively) onManageTwoFactorClick else onManageSecurityOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Passkeys",
                    value = account.passkeyCount?.let { count ->
                        if (count > 0) "$count passkey(s) enrolled" else "No passkeys enrolled"
                    } ?: "Biometric & hardware security keys (WebAuthn)",
                    supportingText = "Enroll or remove browser WebAuthn passkeys on Accounts web",
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
                    value = account.recoveryEmail?.takeIf { it.isNotBlank() } ?: "Not set",
                    icon = Icons.Rounded.MarkEmailRead,
                    statusBadge = account.recoveryEmailVerified?.let { verified ->
                        { StatusPill(if (verified) "Verified" else "Unverified", if (verified) StatusTone.VERIFIED else StatusTone.WARNING) }
                    },
                    trailingIcon = if (canManageNatively && canWriteNatively) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively && canWriteNatively) onEditRecoveryEmailClick else onManageSecurityOnWeb,
                )
            }
        }

        // Devices & sessions
        item {
            AccountSectionCard(
                title = "Your devices & sessions",
                subtitle = "Each Gothwad app holds its own hardware-backed OAuth tokens on this phone.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Active account sessions",
                    value = when {
                        webSessions.isNotEmpty() -> "${webSessions.size} active session(s)"
                        account.activeSessionsCount != null -> "${account.activeSessionsCount} active session(s)"
                        else -> "Review active sessions & sign out other devices"
                    },
                    supportingText = if (canManageNatively) {
                        "Sign out individual sessions below or clear all other sessions"
                    } else {
                        "Opens Security & devices on accounts.gothwadtech.com"
                    },
                    icon = Icons.Rounded.Devices,
                    trailingIcon = if (canManageNatively) null else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively) null else onManageSecurityOnWeb,
                )
                if (canManageNatively && webSessions.isNotEmpty()) {
                    webSessions.take(8).forEach { ws ->
                        SectionDivider()
                        val label = listOfNotNull(ws.browser, ws.os).joinToString(" · ")
                            .ifBlank { ws.deviceName ?: "Browser session" }
                        AccountSettingRow(
                            label = if (ws.current) "Current session" else "Other session",
                            value = label,
                            supportingText = listOfNotNull(ws.ipAddress, ws.lastActive).joinToString(" · "),
                            icon = Icons.Rounded.Devices,
                            trailingIcon = null,
                            statusBadge = if (ws.current) {
                                { StatusPill("Current", StatusTone.VERIFIED) }
                            } else if (canWriteNatively) {
                                {
                                    TextButton(
                                        onClick = { onRevokeWebSession(ws.id) },
                                        modifier = Modifier.testTag("revoke_session_${ws.id}"),
                                    ) {
                                        Text("Sign out", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else null,
                        )
                    }
                    if (canWriteNatively && (webSessions.count { !it.current } > 0 || webSessions.size > 1)) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            OutlinedButton(
                                onClick = onRevokeOtherWebSessions,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign out all other sessions")
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
                    trailingIcon = null,
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
            AccountSectionCard(
                title = "Activity & app permissions",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                if (canManageNatively && activityFeed.isNotEmpty()) {
                    activityFeed.take(8).forEachIndexed { idx, entry ->
                        if (idx > 0) SectionDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(semantic.verifiedContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = semantic.onVerifiedContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = entry.detail ?: entry.type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val meta = listOfNotNull(entry.ipAddress, entry.createdAt).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
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
