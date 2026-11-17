package com.gothwad.accounts.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.MfaEnrollmentSetup
import com.gothwad.accounts.core.MfaStatusInfo
import com.gothwad.accounts.core.ProfileValidation
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AvatarManagementDialog(
    account: GothwadAccount,
    isBusy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onUploadAvatar: (dataUrl: String, mime: String) -> Unit,
    onRemoveAvatar: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localError by remember { mutableStateOf<String?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingPhoto = true
            localError = null
            val encoded = withContext(Dispatchers.IO) { encodeAvatarFromUri(context, uri) }
            isProcessingPhoto = false
            encoded.onSuccess { dataUrl ->
                onUploadAvatar(dataUrl, "image/jpeg")
            }.onFailure { err ->
                localError = err.message ?: "Could not process selected photo"
            }
        }
    }

    val working = isBusy || isProcessingPhoto

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("Profile photo", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountAvatar(
                    displayName = account.displayName(),
                    initials = account.initials(),
                    avatarUrl = account.avatarUrl,
                    size = 84.dp,
                    showBrandRing = true,
                )
                Text(
                    text = "Choose a photo from your device. It will be resized to 512x512 JPEG before uploading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                (localError ?: errorMessage)?.let { err ->
                    ManagementErrorBanner(err)
                }
                Button(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = !working,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (working) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose photo from device")
                    }
                }
                if (!account.avatarUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onRemoveAvatar,
                        enabled = !working,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove photo")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}

@Composable
fun PhoneManagementDialog(
    account: GothwadAccount,
    isBusy: Boolean,
    requiresMfa: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSavePhone: (phoneNumber: String, currentPassword: String, mfaCode: String?) -> Unit,
) {
    var phone by rememberSaveable(account.phoneNumber) { mutableStateOf(account.phoneNumber.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var mfaCode by rememberSaveable { mutableStateOf("") }
    val phoneValid = ProfileValidation.isValidPhoneNumber(phone)

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Phone number", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Set or clear your phone number. Leave blank to remove it. Your current password is required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                errorMessage?.let { ManagementErrorBanner(it) }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    placeholder = { Text("+91 98765 43210") },
                    isError = !phoneValid,
                    singleLine = true,
                    enabled = !isBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Current password",
                    enabled = !isBusy,
                )
                if (requiresMfa || account.mfaEnabled == true) {
                    OutlinedTextField(
                        value = mfaCode,
                        onValueChange = { mfaCode = it },
                        label = { Text("Authenticator or recovery code") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSavePhone(phone.trim(), password, mfaCode.trim().ifBlank { null }) },
                enabled = !isBusy && phoneValid && password.isNotEmpty(),
            ) {
                Text(if (isBusy) "Saving..." else if (phone.trim().isEmpty()) "Remove phone" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}

@Composable
fun RecoveryEmailDialog(
    account: GothwadAccount,
    isBusy: Boolean,
    pendingVerification: Boolean,
    requiresMfa: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRequestChange: (recoveryEmail: String, currentPassword: String, mfaCode: String?) -> Unit,
    onVerifyToken: (token: String) -> Unit,
) {
    var recoveryEmail by rememberSaveable(account.recoveryEmail) { mutableStateOf(account.recoveryEmail.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var mfaCode by rememberSaveable { mutableStateOf("") }
    var verificationToken by rememberSaveable { mutableStateOf("") }
    var manualVerifyStep by rememberSaveable { mutableStateOf(false) }

    val showVerifyStep = pendingVerification || manualVerifyStep
    val emailValid = ProfileValidation.isValidEmail(recoveryEmail)

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                if (showVerifyStep) "Verify recovery email" else "Recovery email",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                errorMessage?.let { ManagementErrorBanner(it) }
                if (!showVerifyStep) {
                    Text(
                        text = "Enter the new recovery email address. A verification token will be sent to that inbox.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        label = { Text("New recovery email") },
                        isError = recoveryEmail.isNotEmpty() && !emailValid,
                        singleLine = true,
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PasswordInputField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Current password",
                        enabled = !isBusy,
                    )
                    if (requiresMfa || account.mfaEnabled == true) {
                        OutlinedTextField(
                            value = mfaCode,
                            onValueChange = { mfaCode = it },
                            label = { Text("Authenticator or recovery code") },
                            singleLine = true,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TextButton(
                        onClick = { manualVerifyStep = true },
                        enabled = !isBusy,
                    ) {
                        Text("Already have a verification token?")
                    }
                } else {
                    Text(
                        text = "Paste the 64-character verification token sent to your replacement recovery email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = verificationToken,
                        onValueChange = { verificationToken = it },
                        label = { Text("Verification token") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!pendingVerification) {
                        TextButton(
                            onClick = { manualVerifyStep = false },
                            enabled = !isBusy,
                        ) {
                            Text("Back to email entry")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showVerifyStep) {
                Button(
                    onClick = { onRequestChange(recoveryEmail.trim(), password, mfaCode.trim().ifBlank { null }) },
                    enabled = !isBusy && emailValid && password.isNotEmpty(),
                ) {
                    Text(if (isBusy) "Sending..." else "Send verification")
                }
            } else {
                Button(
                    onClick = { onVerifyToken(verificationToken.trim()) },
                    enabled = !isBusy && verificationToken.trim().isNotEmpty(),
                ) {
                    Text(if (isBusy) "Verifying..." else "Verify email")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}

@Composable
fun PasswordManagementDialog(
    account: GothwadAccount,
    isBusy: Boolean,
    requiresMfa: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, mfaCode: String?) -> Unit,
) {
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var mfaCode by rememberSaveable { mutableStateOf("") }
    val needsCurrent = account.hasPassword != false
    val newValid = ProfileValidation.isValidPassword(newPassword)
    val matchValid = newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                if (needsCurrent) "Change password" else "Add password",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Choose an 8-128 character password. Saving keeps this app signed in and signs out other devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                errorMessage?.let { ManagementErrorBanner(it) }
                if (needsCurrent) {
                    PasswordInputField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Current password",
                        enabled = !isBusy,
                    )
                }
                PasswordInputField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New password (8+ characters)",
                    enabled = !isBusy,
                    isError = newPassword.isNotEmpty() && !newValid,
                )
                PasswordInputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm new password",
                    enabled = !isBusy,
                    isError = confirmPassword.isNotEmpty() && !matchValid,
                )
                if (requiresMfa || account.mfaEnabled == true) {
                    OutlinedTextField(
                        value = mfaCode,
                        onValueChange = { mfaCode = it },
                        label = { Text("Authenticator or recovery code") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onChangePassword(currentPassword, newPassword, mfaCode.trim().ifBlank { null }) },
                enabled = !isBusy && (!needsCurrent || currentPassword.isNotEmpty()) && newValid && matchValid,
            ) {
                Text(if (isBusy) "Saving..." else "Save password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}

@Composable
fun TwoFactorManagementDialog(
    account: GothwadAccount,
    mfaStatus: MfaStatusInfo?,
    mfaSetup: MfaEnrollmentSetup?,
    recoveryCodes: List<String>,
    isBusy: Boolean,
    errorMessage: String?,
    onCopyText: (label: String, text: String) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onBeginEnroll: (currentPassword: String) -> Unit,
    onConfirmEnroll: (currentPassword: String, enrollmentId: String, code: String) -> Unit,
    onRegenerateCodes: (currentPassword: String, code: String) -> Unit,
    onDisableMfa: (currentPassword: String, code: String) -> Unit,
) {
    val context = LocalContext.current
    val semantic = LocalAccountColors.current
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val isEnabled = mfaStatus?.enabled ?: (account.mfaEnabled == true)

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("2-Step Verification", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                errorMessage?.let { ManagementErrorBanner(it) }

                when {
                    recoveryCodes.isNotEmpty() -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = semantic.verifiedContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Save your backup recovery codes now. Each code works once if you lose your authenticator app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.onVerifiedContainer,
                                )
                                Text(
                                    text = recoveryCodes.joinToString("\n"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = semantic.onVerifiedContainer,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onCopyText("Recovery codes", recoveryCodes.joinToString("\n")) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy all recovery codes")
                        }
                    }

                    mfaSetup != null -> {
                        Text(
                            text = "Add this account in your authenticator app (Google Authenticator, Aegis, Ente, etc.), then enter the 6-digit code below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Setup key", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = mfaSetup.secret,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onCopyText("Setup key", mfaSetup.secret) }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy key")
                                    }
                                    TextButton(onClick = {
                                        val launched = runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mfaSetup.otpauthUri)))
                                        }.isSuccess
                                        if (!launched) {
                                            onShowMessage("No authenticator app handled otpauth links; copy the setup key manually.")
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Authenticator")
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("6-digit verification code") },
                            singleLine = true,
                            enabled = !isBusy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    isEnabled -> {
                        val remaining = mfaStatus?.recoveryCodesRemaining
                        Text(
                            text = if (remaining != null) {
                                "Authenticator verification is active ($remaining backup recovery code(s) remaining)."
                            } else {
                                "Authenticator verification is active on your account."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PasswordInputField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Current password",
                            enabled = !isBusy,
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Authenticator or recovery code") },
                            singleLine = true,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(
                            onClick = { onRegenerateCodes(password, code) },
                            enabled = !isBusy && password.isNotEmpty() && code.trim().isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Regenerate backup codes")
                        }
                        OutlinedButton(
                            onClick = { onDisableMfa(password, code) },
                            enabled = !isBusy && password.isNotEmpty() && code.trim().isNotEmpty(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Disable 2-Step Verification")
                        }
                    }

                    else -> {
                        Text(
                            text = "Confirm your current password to begin setting up an authenticator app for 2-Step Verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PasswordInputField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Current password",
                            enabled = !isBusy,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                recoveryCodes.isNotEmpty() -> {
                    Button(onClick = onDismiss) { Text("Done") }
                }
                mfaSetup != null -> {
                    Button(
                        onClick = { onConfirmEnroll(password, mfaSetup.enrollmentId, code) },
                        enabled = !isBusy && code.trim().isNotEmpty(),
                    ) {
                        Text(if (isBusy) "Verifying..." else "Enable 2FA")
                    }
                }
                !isEnabled -> {
                    Button(
                        onClick = { onBeginEnroll(password) },
                        enabled = !isBusy && password.isNotEmpty(),
                    ) {
                        Text(if (isBusy) "Starting..." else "Continue")
                    }
                }
            }
        },
        dismissButton = {
            if (recoveryCodes.isEmpty()) {
                TextButton(onClick = onDismiss, enabled = !isBusy) {
                    Text(stringResource(R.string.gothwad_cancel))
                }
            }
        },
    )
}

@Composable
fun ExportDataDialog(
    jsonContent: String,
    onCopyJson: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val saved = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
            } ?: error("Output stream unavailable")
        }.isSuccess
        onShowMessage(if (saved) "Account data saved as JSON" else "Could not write JSON file")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export your account data", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Ready (${jsonContent.toByteArray(Charsets.UTF_8).size} bytes). Save to your device or share directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, LocalAccountColors.current.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = jsonContent.take(600) + if (jsonContent.length > 600) "\n..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Button(
                    onClick = { saveLauncher.launch("gothwad-account-export.json") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save .json file")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onCopyJson(jsonContent) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "Gothwad Account Data Export")
                                putExtra(Intent.EXTRA_TEXT, jsonContent)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(shareIntent, "Export account data"))
                            }.onFailure {
                                onShowMessage("No share target available")
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun DeleteAccountDialog(
    account: GothwadAccount,
    isBusy: Boolean,
    requiresMfa: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmDelete: (password: String, mfaCode: String?) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var mfaCode by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                "Delete Gothwad Account",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "This permanently deletes ${account.displayEmail()}, removes your OAuth authorizations and sessions, and cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                errorMessage?.let { ManagementErrorBanner(it) }
                PasswordInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Confirm password",
                    enabled = !isBusy,
                )
                if (requiresMfa || account.mfaEnabled == true) {
                    OutlinedTextField(
                        value = mfaCode,
                        onValueChange = { mfaCode = it },
                        label = { Text("Authenticator or recovery code") },
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(password, mfaCode.trim().ifBlank { null }) },
                enabled = !isBusy && password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_account_button"),
            ) {
                Text(if (isBusy) "Deleting..." else "Delete permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.gothwad_cancel))
            }
        },
    )
}

@Composable
private fun ManagementErrorBanner(message: String) {
    val semantic = LocalAccountColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = semantic.criticalContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = semantic.onCriticalContainer,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun encodeAvatarFromUri(context: Context, uri: Uri): Result<String> = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: error("Could not read selected image")

    val maxDim = max(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (maxDim / sampleSize > 1024) {
        sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
    } ?: error("Selected file is not a valid image")

    val scale = if (max(decoded.width, decoded.height) > 512) {
        512f / max(decoded.width, decoded.height).toFloat()
    } else {
        1f
    }
    val target = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            decoded,
            max(1, (decoded.width * scale).roundToInt()),
            max(1, (decoded.height * scale).roundToInt()),
            true,
        )
    } else {
        decoded
    }

    val out = ByteArrayOutputStream()
    var quality = 85
    target.compress(Bitmap.CompressFormat.JPEG, quality, out)
    while (out.size() > 1_800_000 && quality > 50) {
        out.reset()
        quality -= 15
        target.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }
    if (target !== decoded) target.recycle()
    decoded.recycle()

    val bytes = out.toByteArray()
    require(bytes.isNotEmpty() && bytes.size <= 2 * 1024 * 1024) { "Photo exceeds 2 MB limit" }
    "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}
