package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.AccountToggleRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.AccountDashboardUiState

@Composable
fun PrivacyCheckupScreen(
    state: AccountDashboardUiState,
    onToggleStepDone: (Int) -> Unit,
    onToggleActivityControl: (String) -> Unit,
    onTogglePersonalizedAds: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val steps = listOf(
        Triple("Web & App Activity", "Choose whether searches and app usage are saved to your Account.", "web_app"),
        Triple("Timeline & Location", "Keep location history paused or auto-deleted.", "timeline"),
        Triple("YouTube History", "Control video watch and search history retention.", "youtube"),
        Triple("Personalized Ads", "Decide if your interests personalize ads in My Ad Center.", "ads"),
    )
    val progress = state.privacyCheckupStepsDone.size / steps.size.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TabIntroHeader(
                    title = "Privacy Checkup",
                    subtitle = "${state.privacyCheckupStepsDone.size} of ${steps.size} privacy steps reviewed.",
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
        }

        items(steps.indices.toList()) { index ->
            val (title, desc, key) = steps[index]
            val reviewed = index in state.privacyCheckupStepsDone
            val isEnabled = when (key) {
                "ads" -> state.personalizedAdsEnabled
                else -> state.activityControls.firstOrNull { it.id == key }?.isEnabled == true
            }

            AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = reviewed,
                        onCheckedChange = { onToggleStepDone(index) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        label = if (isEnabled) "On" else "Paused",
                        tone = if (isEnabled) StatusTone.VERIFIED else StatusTone.PAUSED,
                    )
                }
                SectionDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        onClick = {
                            if (key == "ads") onTogglePersonalizedAds(!isEnabled)
                            else onToggleActivityControl(key)
                            if (!reviewed) onToggleStepDone(index)
                        },
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(if (isEnabled) "Pause setting" else "Turn on")
                    }
                }
            }
        }

        item {
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Text("Finish Privacy Checkup")
            }
        }
    }
}

@Composable
fun SecurityCheckupScreen(
    state: AccountDashboardUiState,
    onResolveRecommendation: (String) -> Unit,
    onEnableTwoStep: () -> Unit,
    onEnableSafeBrowsing: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val unresolved = state.unresolvedSecurityCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (unresolved == 0) semantic.verifiedContainer else semantic.warningContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (unresolved == 0) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = if (unresolved == 0) semantic.onVerifiedContainer else semantic.onWarningContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (unresolved == 0) "No recommended actions" else "$unresolved recommended action${if (unresolved > 1) "s" else ""}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (unresolved == 0) semantic.onVerifiedContainer else semantic.onWarningContainer,
                        )
                        Text(
                            text = if (unresolved == 0) "Your Account security status is strong." else "Resolve the items below to strengthen your Account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unresolved == 0) semantic.onVerifiedContainer else semantic.onWarningContainer,
                        )
                    }
                }
            }
        }

        if (!state.twoStepEnabled) {
            item {
                AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("2-Step Verification is turned off", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Re-enable 2-Step Verification to stop unauthorized sign-ins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onEnableTwoStep, shape = RoundedCornerShape(50)) {
                            Text("Turn on")
                        }
                    }
                }
            }
        }

        if (!state.enhancedSafeBrowsing) {
            item {
                AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enhanced Safe Browsing is off", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Protect against phishing links and risky downloads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onEnableSafeBrowsing, shape = RoundedCornerShape(50)) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        items(state.securityRecommendations, key = { it.id }) { rec ->
            AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (rec.isResolved) semantic.verifiedContainer else semantic.warningContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (rec.isResolved) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = if (rec.isResolved) semantic.onVerifiedContainer else semantic.onWarningContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rec.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            rec.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!rec.isResolved) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onResolveRecommendation(rec.id) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("resolve_${rec.id}"),
                            ) {
                                Text(rec.actionLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityControlDetailScreen(
    controlId: String,
    state: AccountDashboardUiState,
    onToggleControl: (String) -> Unit,
    onUpdateAutoDelete: (String, String) -> Unit,
) {
    val control = state.activityControls.firstOrNull { it.id == controlId } ?: return
    val autoDeleteOptions = listOf(
        "Auto-delete (3 months)",
        "Auto-delete (18 months)",
        "Auto-delete (36 months)",
        "Keep until I delete manually",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = control.title,
                subtitle = control.subtitle,
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        item {
            AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                AccountToggleRow(
                    title = control.title,
                    subtitle = if (control.isEnabled) "Currently saving new activity" else "Paused • New activity is not saved",
                    icon = Icons.Rounded.History,
                    checked = control.isEnabled,
                    onCheckedChange = { onToggleControl(control.id) },
                )
            }
        }

        item {
            AccountSectionCard(
                title = "Auto-delete",
                subtitle = "Automatically remove older activity from your Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                autoDeleteOptions.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateAutoDelete(control.id, option) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = control.autoDeleteLabel == option,
                            onClick = { onUpdateAutoDelete(control.id, option) },
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Rounded.AutoDelete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(option, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (index < autoDeleteOptions.lastIndex) {
                        SectionDivider()
                    }
                }
            }
        }

        item {
            AccountSectionCard(
                title = "Recent activity",
                subtitle = "Sample of what is currently stored in ${control.title}.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                control.recentEntries.forEachIndexed { index, entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (index < control.recentEntries.lastIndex) {
                        SectionDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun TwoStepVerificationScreen(
    state: AccountDashboardUiState,
    onToggleTwoStep: (Boolean) -> Unit,
    onAddPasskey: () -> Unit,
    onRegenerateBackupCodes: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "2-Step Verification & Passkeys",
                subtitle = "Protect your Account with both your passwordless passkeys and a second verification step.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        item {
            AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                AccountToggleRow(
                    title = "2-Step Verification",
                    subtitle = if (state.twoStepEnabled) "Required when signing in on new devices" else "Turned off",
                    icon = Icons.Rounded.PhonelinkLock,
                    checked = state.twoStepEnabled,
                    testTag = "two_step_master_switch",
                    onCheckedChange = onToggleTwoStep,
                )
            }
        }

        item {
            AccountSectionCard(
                title = "Passkeys & biometrics",
                subtitle = "Use fingerprint, face, or hardware security keys instead of passwords.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Active passkeys",
                    value = "${state.passkeyCount} passkeys synced",
                    supportingText = "Pixel 9 Pro (Biometric) • MacBook Pro (Touch ID)",
                    icon = Icons.Rounded.Fingerprint,
                    statusBadge = { StatusPill("Active", StatusTone.VERIFIED) },
                    onClick = { onShowMessage("${state.passkeyCount} passkeys are ready for passwordless sign-in") },
                )
                SectionDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onAddPasskey,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("create_passkey_button"),
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create a passkey")
                    }
                }
            }
        }

        item {
            AccountSectionCard(
                title = "Second steps",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Device prompts",
                    value = "Pixel 9 Pro",
                    supportingText = "Default tap-to-approve sign-in prompt",
                    icon = Icons.Rounded.PhonelinkLock,
                    statusBadge = { StatusPill("Default", StatusTone.VERIFIED) },
                    onClick = { onShowMessage("Device prompts sent to Pixel 9 Pro") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Authenticator app",
                    value = if (state.authenticatorActive) "Configured on Pixel 9 Pro" else "Not set up",
                    supportingText = "6-digit time-based verification codes",
                    icon = Icons.Rounded.QrCode2,
                    onClick = { onShowMessage("Authenticator TOTP codes are synced") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Backup codes",
                    value = "${state.backupCodesRemaining} unused one-time codes",
                    supportingText = "Tap to generate 10 fresh single-use codes",
                    icon = Icons.Rounded.Pin,
                    onClick = onRegenerateBackupCodes,
                )
            }
        }
    }
}
