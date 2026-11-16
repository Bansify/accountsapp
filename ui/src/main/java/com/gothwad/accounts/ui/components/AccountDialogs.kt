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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.model.DemoAccountProfile
import com.gothwad.accounts.ui.model.EditableField

@Composable
fun AccountSwitcherDialog(
    activeAccount: DemoAccountProfile,
    otherAccounts: List<DemoAccountProfile>,
    onDismiss: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onAddDemoAccount: (String, String) -> Unit,
    onSignInWithBrowser: () -> Unit,
    onOpenDeviceAccountsTip: () -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    val semantic = LocalAccountColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("switcher_close_button"),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close account switcher")
                    }
                    Text(
                        text = "Accounts",
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
                                account = activeAccount,
                                size = 56.dp,
                                showGoogleRing = true,
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeAccount.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = activeAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switcher_manage_account_button"),
                        ) {
                            Text("Manage your Account")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Other accounts list
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        otherAccounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSwitchAccount(acc.id) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("switch_to_${acc.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AccountAvatar(account = acc, size = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(acc.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        acc.email,
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

                        if (!showAddForm) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddForm = true }
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
                                Text("Add another account", style = MaterialTheme.typography.titleSmall)
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    label = { Text("Full name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = newEmail,
                                    onValueChange = { newEmail = it },
                                    label = { Text("Email address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TextButton(onClick = onSignInWithBrowser) {
                                        Text("Browser OAuth")
                                    }
                                    Row {
                                        TextButton(onClick = { showAddForm = false }) { Text("Cancel") }
                                        Button(onClick = { onAddDemoAccount(newName, newEmail) }) {
                                            Text("Add")
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = semantic.cardBorder,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDeviceAccountsTip() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    Icons.Rounded.ManageAccounts,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Manage accounts on this device", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Privacy Policy  •  Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
fun EditFieldDialog(
    field: EditableField,
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var textValue by remember(field, currentValue) { mutableStateOf(currentValue) }
    val presetChoices = when (field) {
        EditableField.GENDER -> listOf("Male", "Female", "Non-binary", "Prefer not to say")
        EditableField.LANGUAGE -> listOf("English (India)", "English (United States)", "हिन्दी (भारत)", "मराठी")
        EditableField.PRONOUNS -> listOf("he/him", "she/her", "they/them", "Do not display")
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${field.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = field.helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (presetChoices.isNotEmpty()) {
                    Column {
                        presetChoices.forEach { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { textValue = choice }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = textValue == choice,
                                    onClick = { textValue = choice },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(choice, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text(field.title) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_field_input"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(textValue) },
                enabled = textValue.isNotBlank(),
                modifier = Modifier.testTag("edit_field_save_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirmChange: () -> Unit,
) {
    var currentPass by remember { mutableStateOf("••••••••••••") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }

    val strength = when {
        newPass.length >= 12 && newPass.any { it.isDigit() } && newPass.any { !it.isLetterOrDigit() } -> 1.0f
        newPass.length >= 8 -> 0.65f
        newPass.isNotEmpty() -> 0.3f
        else -> 0f
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.LockReset, contentDescription = null) },
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose a strong password that you don't use for other websites.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = { Text("Current password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New password") },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                if (showNew) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle password visibility",
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (newPass.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { strength },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = when {
                                strength >= 0.9f -> "Password strength: Strong"
                                strength >= 0.6f -> "Password strength: Medium"
                                else -> "Password strength: Use at least 8 characters"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Confirm new password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmChange,
                enabled = newPass.length >= 6 && newPass == confirmPass,
            ) {
                Text("Change password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PhotoPickerDemoDialog(
    account: DemoAccountProfile,
    onDismiss: () -> Unit,
    onSavePhoto: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile picture") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AccountAvatar(account = account, size = 96.dp, showGoogleRing = true)
                Text(
                    "Your profile picture is visible across services and helps people recognize you in messages and shared folders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onSavePhoto) { Text("Set as profile photo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun TakeoutExportDialog(
    onDismiss: () -> Unit,
    onStartExport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
        title = { Text("Download your data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Create a single encrypted archive (.zip) of your Drive files, Mail, Contacts, Calendar, and Activity history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf("Drive (18.4 GB)", "Mail (9.6 GB)", "Photos & Media (13.2 GB)", "Contacts & Calendar (18 MB)").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartExport) { Text("Create export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun DeleteAccountPreviewDialog(
    email: String,
    onDismiss: () -> Unit,
    onConfirmDemo: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Delete your Account?") },
        text = {
            Text(
                "Deleting $email permanently removes your Drive data, emails, subscriptions, and connected app logins. This is a safe demo preview and will not delete your real account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDemo,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Simulate request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep account") }
        },
    )
}

@Composable
fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onAdd: (label: String, detail: String, type: String) -> Unit,
) {
    var selectedType by remember { mutableStateOf("UPI") }
    var inputValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add payment method") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("UPI", "Card").forEach { type ->
                        val selected = selectedType == type
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedType = type },
                        ) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = {
                        Text(if (selectedType == "UPI") "UPI ID (e.g. pawan@okicici)" else "Card last 4 digits (e.g. 9012)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = if (selectedType == "UPI") "UPI • $inputValue" else "Mastercard •••• ${inputValue.takeLast(4)}"
                    val detail = if (selectedType == "UPI") "Instant bank transfer" else "Expires 11/30"
                    onAdd(label, detail, selectedType)
                },
                enabled = inputValue.isNotBlank(),
            ) {
                Text("Save method")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun SendFeedbackDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var feedback by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Describe your suggestion or issue with the Account dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Your feedback") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(feedback) }, enabled = feedback.isNotBlank()) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
