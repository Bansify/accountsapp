package com.gothwad.accounts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PersonAddAlt1
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.ProfileValidation
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.model.SignOutDialogTarget
import java.time.LocalDate

@Composable
fun AccountSwitcherDialog(
    activeAccount: GothwadAccount,
    otherLocalAccounts: List<GothwadAccount>,
    peerAccountsToAuthorize: List<DiscoveredAccount>,
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("switcher_close_button"),
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.gothwad_cd_close),
                        )
                    }
                    Text(
                        text = stringResource(R.string.gothwad_accounts),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Active account hero box
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountAvatar(
                                displayName = activeAccount.displayName(),
                                initials = activeAccount.initials(),
                                avatarUrl = activeAccount.avatarUrl,
                                size = 54.dp,
                                showBrandRing = true,
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeAccount.displayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = activeAccount.displayEmail(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusPill(
                                    label = stringResource(R.string.gothwad_active),
                                    tone = StatusTone.VERIFIED,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onManageOnWeb,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switcher_manage_web_button"),
                        ) {
                            Text(stringResource(R.string.gothwad_manage_web))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { onRequestSignOut(activeAccount) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switcher_sign_out_active_button"),
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gothwad_remove_local))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
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
                                .clickable(role = Role.Button) { onAddAnotherAccount() }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("add_another_account_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.PersonAddAlt1,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.gothwad_add_account),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = onOpenPrivacy) {
                        Text(stringResource(R.string.gothwad_privacy), style = MaterialTheme.typography.bodySmall)
                    }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.displayName, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = target.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.gothwad_local_removal_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.gothwad_device_removal_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.gothwad_server_disconnect_notice),
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
                TextButton(
                    onClick = onManageWebSessions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gothwad_manage_web))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSignOutThisApp,
                modifier = Modifier.testTag("dialog_sign_out_app_button"),
            ) {
                Text(stringResource(R.string.gothwad_remove_local))
            }
        },
        dismissButton = {
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
