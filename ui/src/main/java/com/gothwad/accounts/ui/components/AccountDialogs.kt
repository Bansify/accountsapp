package com.gothwad.accounts.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.ProfileValidation
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.model.SignOutDialogTarget
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSwitcherDialog(
    activeAccount: GothwadAccount,
    otherLocalAccounts: List<GothwadAccount>,
    peerAccountsToAuthorize: List<DiscoveredAccount>,
    isRefreshing: Boolean = false,
    onRefreshClick: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onContinueWithPeerAccount: (DiscoveredAccount) -> Unit,
    onAddAnotherAccount: () -> Unit,
    onRequestSignOut: (GothwadAccount) -> Unit,
    onManageOnWeb: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val context = LocalContext.current
    val hasQuota = activeAccount.storageUsedBytes != null && activeAccount.storageLimitBytes != null
    val refreshSpin = rememberInfiniteTransition(label = "control_centre_refresh")
    val refreshAngle by refreshSpin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "control_centre_refresh_angle",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, semantic.cardBorder),
            shadowElevation = 10.dp,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .fillMaxWidth()
                .widthIn(max = 440.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Branded Control Centre Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp),
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("switcher_close_button"),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.gothwad_cd_close),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GothwadBrandBadge(size = 22.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gothwad_account_brand),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (onRefreshClick != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(36.dp),
                        ) {
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = !isRefreshing,
                                modifier = Modifier.testTag("top_refresh_button"),
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.gothwad_refresh_profile),
                                    tint = if (isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(if (isRefreshing) refreshAngle else 0f),
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }

                // Active Account Control Hero Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AccountAvatar(
                                displayName = activeAccount.displayName(),
                                initials = activeAccount.initials(),
                                avatarUrl = activeAccount.avatarUrl,
                                size = 60.dp,
                                showBrandRing = true,
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, semantic.cardBorder),
                                modifier = Modifier.size(20.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = semantic.verifiedGreen,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activeAccount.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeAccount.displayEmail(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusPill(
                                label = stringResource(R.string.gothwad_active),
                                tone = StatusTone.VERIFIED,
                            )
                            activeAccount.username?.takeIf { it.isNotBlank() }?.let { handle ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        text = "@$handle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            activeAccount.mfaEnabled?.let { mfa ->
                                StatusPill(
                                    label = if (mfa) "2-Step On" else "2-Step Off",
                                    tone = if (mfa) StatusTone.VERIFIED else StatusTone.WARNING,
                                )
                            }
                        }

                        if (hasQuota) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, semantic.cardBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    GothwadQuotaSummary(
                                        context = context,
                                        usedBytes = activeAccount.storageUsedBytes,
                                        limitBytes = activeAccount.storageLimitBytes,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onManageOnWeb,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                                .testTag("switcher_manage_web_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.gothwad_manage_web),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                // Switch / Add Account Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (otherLocalAccounts.isNotEmpty() || peerAccountsToAuthorize.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.gothwad_choose_account),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }

                        otherLocalAccounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable(role = Role.Button) { onSwitchAccount(acc.userId) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("switch_to_${acc.userId}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AccountAvatar(
                                    displayName = acc.displayName(),
                                    initials = acc.initials(),
                                    avatarUrl = acc.avatarUrl,
                                    size = 36.dp,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.displayName(), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        acc.displayEmail(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            HorizontalDivider(
                                color = semantic.cardBorder,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        peerAccountsToAuthorize.forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable(role = Role.Button) { onContinueWithPeerAccount(peer) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("continue_peer_${peer.userId}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AccountAvatar(
                                    displayName = peer.displayName(),
                                    initials = peer.initials(),
                                    avatarUrl = peer.avatarUrl,
                                    size = 36.dp,
                                    showBrandRing = true,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.gothwad_continue_as, peer.displayName()),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "${peer.displayEmail()} · ${peer.sourcePackage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    Icons.Rounded.Smartphone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            HorizontalDivider(
                                color = semantic.cardBorder,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable(role = Role.Button) { onAddAnotherAccount() }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("add_another_account_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PersonAddAlt1,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.gothwad_add_account),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // App Settings & Sign-out Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        if (onOpenSettings != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable(role = Role.Button, onClick = onOpenSettings)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("switcher_app_settings_button"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.gothwad_settings_title),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "Theme & navigation layout",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            HorizontalDivider(
                                color = semantic.cardBorder,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable(role = Role.Button) { onRequestSignOut(activeAccount) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("switcher_sign_out_active_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(semantic.criticalContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Logout,
                                    contentDescription = null,
                                    tint = semantic.onCriticalContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.gothwad_remove_local),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = "Local, device, or server OAuth options",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Footer Legal Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onOpenPrivacy) {
                        Text(stringResource(R.string.gothwad_privacy), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onOpenTerms) {
                        Text(stringResource(R.string.gothwad_terms), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Explicitly separates app-local sign-out, device-wide removal, and server OAuth grant disconnection. */
@Composable
fun SignOutOrRemoveDialog(
    target: SignOutDialogTarget,
    onDismiss: () -> Unit,
    onSignOutThisApp: () -> Unit,
    onRemoveFromDevice: () -> Unit,
    onDisconnectGrantOnServer: () -> Unit = onSignOutThisApp,
    onManageWebSessions: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .widthIn(max = 460.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(target.displayName, fontWeight = FontWeight.SemiBold)
                Text(
                    text = target.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.gothwad_local_removal_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onSignOutThisApp,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_sign_out_app_button"),
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gothwad_remove_local))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.gothwad_device_removal_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onRemoveFromDevice,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_remove_device_button"),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gothwad_remove_device))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = semantic.criticalContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.gothwad_server_disconnect_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onCriticalContainer,
                        )
                        OutlinedButton(
                            onClick = onDisconnectGrantOnServer,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_disconnect_server_button"),
                        ) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gothwad_disconnect_server))
                        }
                    }
                }

                TextButton(
                    onClick = onManageWebSessions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gothwad_manage_web))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}

@Composable
fun EditBasicProfileDialog(
    account: GothwadAccount,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (firstName: String, lastName: String, dateOfBirth: String, gender: String) -> Unit,
) {
    val splitName = remember(account.name) {
        val parts = account.name?.trim()?.split(Regex("\\s+"), limit = 2).orEmpty()
        parts.firstOrNull().orEmpty() to parts.getOrNull(1).orEmpty()
    }
    var firstName by remember(account) { mutableStateOf(account.firstName ?: splitName.first) }
    var lastName by remember(account) { mutableStateOf(account.lastName ?: splitName.second) }
    var dateOfBirth by remember(account) { mutableStateOf(account.dateOfBirth.orEmpty()) }
    var gender by remember(account) { mutableStateOf(account.gender.orEmpty()) }

    val todayIso = remember { LocalDate.now().toString() }
    val firstNameValid = ProfileValidation.isValidFirstName(firstName)
    val lastNameValid = ProfileValidation.isValidLastName(lastName)
    val dobValid = ProfileValidation.isValidDateOfBirth(dateOfBirth, todayIso)
    val genderValid = ProfileValidation.isValidGender(gender)
    val genderOptions = remember { listOf("" to "Not set") + ProfileValidation.ALLOWED_GENDERS.map { it to it } }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .widthIn(max = 460.dp),
        title = { Text("Edit personal info", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First name") },
                    isError = !firstNameValid,
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last name (optional)") },
                    isError = !lastNameValid,
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of birth (YYYY-MM-DD)") },
                    placeholder = { Text("YYYY-MM-DD or leave empty") },
                    isError = !dobValid,
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Gender", style = MaterialTheme.typography.labelMedium)
                genderOptions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pair.forEach { (value, label) ->
                            FilterChip(
                                selected = gender == value,
                                onClick = { if (!isSaving) gender = value },
                                label = { Text(label) },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(firstName.trim(), lastName.trim(), dateOfBirth.trim(), gender.trim()) },
                enabled = !isSaving && firstNameValid && lastNameValid && dobValid && genderValid,
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}
