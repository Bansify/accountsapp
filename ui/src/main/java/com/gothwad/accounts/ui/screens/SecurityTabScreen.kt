package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TabletAndroid
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
import com.gothwad.accounts.ui.model.DeviceType
import com.gothwad.accounts.ui.model.EditableField

@Composable
fun SecurityTabScreen(
    state: AccountDashboardUiState,
    onOpenSubScreen: (AccountSubScreen) -> Unit,
    onEditField: (EditableField) -> Unit,
    onOpenChangePassword: () -> Unit,
    onToggleEnhancedSafeBrowsing: (Boolean) -> Unit,
) {
    val semantic = LocalAccountColors.current
    val account = state.activeAccount
    val unresolvedCount = state.unresolvedSecurityCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Security",
                subtitle = "Settings and recommendations to help keep your Account secure.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Security Checkup Hero Status Card
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatusPill(
                                label = if (unresolvedCount == 0) "All checks passed" else "$unresolvedCount recommendation",
                                tone = if (unresolvedCount == 0) StatusTone.VERIFIED else StatusTone.WARNING,
                            )
                            Text(
                                text = if (unresolvedCount == 0) "Your account is protected" else "Review security recommendations",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = if (unresolvedCount == 0) {
                                    "Your devices, passkeys, and recovery methods are verified."
                                } else {
                                    "Check recent sign-in activity and ensure 2-Step Verification and Safe Browsing stay enabled."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_security_shield),
                            contentDescription = "Security checkup illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = { onOpenSubScreen(AccountSubScreen.SecurityCheckup) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("security_open_checkup_button"),
                    ) {
                        Text(
                            text = "Open Security Checkup",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Recent security activity
        item {
            AccountSectionCard(
                title = "Recent security activity",
                subtitle = "Latest security events and sign-ins on your Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                state.securityEvents.take(3).forEachIndexed { index, event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(semantic.verifiedContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = semantic.onVerifiedContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${event.device} • ${event.location} • ${event.timestamp}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < 2 && index < state.securityEvents.lastIndex) {
                        SectionDivider()
                    }
                }
                SectionDivider()
                TextButton(
                    onClick = { onOpenSubScreen(AccountSubScreen.SecurityCheckup) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                ) {
                    Text("Review security activity", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // How you sign in to your Account
        item {
            AccountSectionCard(
                title = "How you sign in to your Account",
                subtitle = "Make sure you can always access your Account by keeping this information up to date.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "2-Step Verification",
                    value = if (state.twoStepEnabled) "On since Jan 2024" else "Off",
                    supportingText = if (state.twoStepEnabled) "Phone prompts, Authenticator & ${state.backupCodesRemaining} backup codes" else "Recommended for stronger sign-in protection",
                    icon = Icons.Rounded.PhonelinkLock,
                    statusBadge = {
                        StatusPill(
                            label = if (state.twoStepEnabled) "On" else "Off",
                            tone = if (state.twoStepEnabled) StatusTone.VERIFIED else StatusTone.CRITICAL,
                        )
                    },
                    testTag = "security_2fa_row",
                    onClick = { onOpenSubScreen(AccountSubScreen.TwoStepVerification) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Passkeys and security keys",
                    value = "${state.passkeyCount} passkeys",
                    supportingText = "Biometric & hardware-backed sign-in",
                    icon = Icons.Rounded.Key,
                    testTag = "security_passkeys_row",
                    onClick = { onOpenSubScreen(AccountSubScreen.TwoStepVerification) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Password",
                    value = "Last changed ${state.passwordLastChanged}",
                    icon = Icons.Rounded.Lock,
                    testTag = "security_password_row",
                    onClick = onOpenChangePassword,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery phone",
                    value = account.phone,
                    icon = Icons.Rounded.PhoneAndroid,
                    onClick = { onEditField(EditableField.RECOVERY_PHONE) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Recovery email",
                    value = account.recoveryEmail,
                    icon = Icons.Rounded.AlternateEmail,
                    onClick = { onEditField(EditableField.RECOVERY_EMAIL) },
                )
            }
        }

        // Your devices
        item {
            AccountSectionCard(
                title = "Your devices",
                subtitle = "Where you're currently signed in.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                state.devices.take(3).forEachIndexed { index, device ->
                    val icon = when (device.deviceType) {
                        DeviceType.PHONE -> Icons.Rounded.Smartphone
                        DeviceType.TABLET -> Icons.Rounded.TabletAndroid
                        DeviceType.LAPTOP, DeviceType.DESKTOP -> Icons.Rounded.Computer
                    }
                    AccountSettingRow(
                        label = device.platform,
                        value = device.name,
                        supportingText = "${device.location} • ${device.lastActive}",
                        icon = icon,
                        statusBadge = if (device.isCurrentDevice) {
                            { StatusPill("This device", StatusTone.VERIFIED) }
                        } else null,
                        onClick = { onOpenSubScreen(AccountSubScreen.DeviceManagement) },
                    )
                    if (index < 2 && index < state.devices.lastIndex) {
                        SectionDivider()
                    }
                }
                SectionDivider()
                TextButton(
                    onClick = { onOpenSubScreen(AccountSubScreen.DeviceManagement) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("security_manage_devices_button"),
                ) {
                    Text(
                        text = "Manage all devices (${state.devices.size})",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Third-party connections & Password Manager & Safe Browsing
        item {
            AccountSectionCard(
                title = "Connections, passwords & browsing protection",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Your connections to third-party apps",
                    value = "${state.thirdPartyApps.size} connected services",
                    supportingText = "Keep track of apps with access to your Account",
                    icon = Icons.Rounded.Apps,
                    testTag = "security_third_party_row",
                    onClick = { onOpenSubScreen(AccountSubScreen.ThirdPartyApps) },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Password Manager",
                    value = "${state.savedPasswords.size} saved passwords",
                    supportingText = "Check saved credentials and compromised alerts",
                    icon = Icons.Rounded.Password,
                    testTag = "security_password_manager_row",
                    onClick = { onOpenSubScreen(AccountSubScreen.PasswordManager) },
                )
                SectionDivider()
                AccountToggleRow(
                    title = "Enhanced Safe Browsing for your account",
                    subtitle = "Faster, proactive protection against dangerous sites, downloads, and phishing",
                    icon = Icons.Rounded.EnhancedEncryption,
                    checked = state.enhancedSafeBrowsing,
                    testTag = "security_safe_browsing_switch",
                    onCheckedChange = onToggleEnhancedSafeBrowsing,
                )
            }
        }
    }
}
