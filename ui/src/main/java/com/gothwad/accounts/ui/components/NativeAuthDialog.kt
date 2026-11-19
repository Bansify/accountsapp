package com.gothwad.accounts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gothwad.accounts.core.NativeSignUpInput
import com.gothwad.accounts.core.ProfileValidation
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.model.NativeAuthDialogState
import com.gothwad.accounts.ui.model.NativeAuthStep
import java.time.LocalDate

@Composable
fun NativeAuthDialog(
    state: NativeAuthDialogState,
    onDismiss: () -> Unit,
    onSwitchStep: (NativeAuthStep) -> Unit,
    onSubmitIdentifier: (String) -> Unit,
    onCheckUsername: (String) -> Unit,
    onSubmitPassword: (identifier: String, password: String) -> Unit,
    onSubmitMfa: (code: String) -> Unit,
    onSubmitSignUp: (NativeSignUpInput) -> Unit,
    onSubmitForgotPassword: (username: String) -> Unit,
    onSubmitResetPassword: (token: String, newPassword: String) -> Unit,
    onSignInWithBrowser: () -> Unit,
) {
    if (!state.visible) return

    val semantic = LocalAccountColors.current
    var identifierInput by rememberSaveable(state.identifier) { mutableStateOf(state.identifier) }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mfaCodeInput by rememberSaveable { mutableStateOf("") }

    var signUpFirstName by rememberSaveable { mutableStateOf("") }
    var signUpLastName by rememberSaveable { mutableStateOf("") }
    var signUpUsername by rememberSaveable { mutableStateOf("") }
    var signUpPassword by rememberSaveable { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpRecoveryEmail by rememberSaveable { mutableStateOf("") }
    var signUpPhone by rememberSaveable { mutableStateOf("") }
    var signUpDob by rememberSaveable { mutableStateOf("") }
    var signUpGender by rememberSaveable { mutableStateOf("") }

    var forgotUsername by rememberSaveable(state.identifier) {
        mutableStateOf(state.identifier.substringBefore("@").trim())
    }
    var resetToken by rememberSaveable { mutableStateOf("") }
    var resetPassword by rememberSaveable { mutableStateOf("") }
    var resetPasswordVisible by remember { mutableStateOf(false) }

    val title = when (state.step) {
        NativeAuthStep.IDENTIFIER -> "Sign in to Bansify"
        NativeAuthStep.PASSWORD -> "Welcome back"
        NativeAuthStep.MFA -> "2-Step Verification"
        NativeAuthStep.SIGN_UP -> "Create Bansify Account"
        NativeAuthStep.FORGOT_PASSWORD -> "Recover your password"
        NativeAuthStep.RESET_PASSWORD -> "Set a new password"
    }

    Dialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(horizontal = 14.dp, vertical = 18.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (!state.isBusy) onDismiss() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.gothwad_cd_close),
                        )
                    }
                }

                state.infoMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = semantic.verifiedContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onVerifiedContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                state.errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = semantic.criticalContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("native_auth_error_banner"),
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onCriticalContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                when (state.step) {
                    NativeAuthStep.IDENTIFIER -> {
                        Text(
                            text = "Enter your Bansify username, @bansify.com email, or registered phone number.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = identifierInput,
                            onValueChange = { identifierInput = it },
                            label = { Text("Username, email, or phone") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_identifier_input"),
                        )
                        Button(
                            onClick = { onSubmitIdentifier(identifierInput) },
                            enabled = !state.isBusy && identifierInput.trim().isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_continue_button"),
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Continue")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                onClick = { onSwitchStep(NativeAuthStep.SIGN_UP) },
                                enabled = !state.isBusy,
                                modifier = Modifier.testTag("native_auth_create_account_button"),
                            ) {
                                Text("Create account")
                            }
                            TextButton(
                                onClick = { onSwitchStep(NativeAuthStep.FORGOT_PASSWORD) },
                                enabled = !state.isBusy,
                            ) {
                                Text("Forgot password?")
                            }
                        }
                    }

                    NativeAuthStep.PASSWORD -> {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = state.identifier.ifBlank { identifierInput },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        passwordInput = ""
                                        onSwitchStep(NativeAuthStep.IDENTIFIER)
                                    },
                                    enabled = !state.isBusy,
                                ) {
                                    Text("Change")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_password_input"),
                        )

                        Button(
                            onClick = {
                                val target = state.identifier.ifBlank { identifierInput }
                                onSubmitPassword(target, passwordInput)
                            },
                            enabled = !state.isBusy && passwordInput.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_signin_submit_button"),
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Sign in")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                onClick = { onSwitchStep(NativeAuthStep.FORGOT_PASSWORD) },
                                enabled = !state.isBusy,
                            ) {
                                Text("Forgot password?")
                            }
                            TextButton(
                                onClick = { onSwitchStep(NativeAuthStep.IDENTIFIER) },
                                enabled = !state.isBusy,
                            ) {
                                Text("Back")
                            }
                        }
                    }

                    NativeAuthStep.MFA -> {
                        Text(
                            text = state.mfaPrompt.ifBlank {
                                "Enter the 6-digit code from your authenticator app or an unused recovery code."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = mfaCodeInput,
                            onValueChange = { mfaCodeInput = it },
                            label = { Text("Authenticator or recovery code") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_mfa_input"),
                        )
                        Button(
                            onClick = { onSubmitMfa(mfaCodeInput) },
                            enabled = !state.isBusy && mfaCodeInput.trim().isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_mfa_submit_button"),
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Verify & sign in")
                            }
                        }
                        TextButton(
                            onClick = {
                                mfaCodeInput = ""
                                onSwitchStep(NativeAuthStep.PASSWORD)
                            },
                            enabled = !state.isBusy,
                        ) {
                            Text("Back to password")
                        }
                    }

                    NativeAuthStep.SIGN_UP -> {
                        val todayIso = remember { LocalDate.now().toString() }
                        val firstValid = ProfileValidation.isValidFirstName(signUpFirstName)
                        val lastValid = signUpLastName.trim().isNotEmpty() && ProfileValidation.isValidLastName(signUpLastName)
                        val userValid = ProfileValidation.isValidUsername(signUpUsername)
                        val passValid = ProfileValidation.isValidPassword(signUpPassword)
                        val emailValid = ProfileValidation.isValidEmail(signUpRecoveryEmail)
                        val phoneValid = ProfileValidation.isValidPhoneNumber(signUpPhone)
                        val dobValid = ProfileValidation.isValidDateOfBirth(signUpDob, todayIso)
                        val genderValid = ProfileValidation.isValidGender(signUpGender)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = signUpFirstName,
                                onValueChange = { signUpFirstName = it },
                                label = { Text("First name") },
                                singleLine = true,
                                enabled = !state.isBusy,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = signUpLastName,
                                onValueChange = { signUpLastName = it },
                                label = { Text("Last name") },
                                singleLine = true,
                                enabled = !state.isBusy,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = signUpUsername,
                                onValueChange = { signUpUsername = it.lowercase() },
                                label = { Text("Username (3-24 letters/numbers)") },
                                singleLine = true,
                                enabled = !state.isBusy,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { onCheckUsername(signUpUsername) },
                                enabled = !state.isBusy && signUpUsername.trim().isNotEmpty(),
                            ) {
                                Text("Check")
                            }
                        }
                        state.usernameStatusMessage?.let { statusMsg ->
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.usernameAvailable == true) {
                                    semantic.verifiedGreen
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }

                        OutlinedTextField(
                            value = signUpPassword,
                            onValueChange = { signUpPassword = it },
                            label = { Text("Password (8+ characters)") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                    Icon(
                                        imageVector = if (signUpPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = signUpRecoveryEmail,
                            onValueChange = { signUpRecoveryEmail = it },
                            label = { Text("Recovery email") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = signUpPhone,
                            onValueChange = { signUpPhone = it },
                            label = { Text("Phone number (optional)") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = signUpDob,
                            onValueChange = { signUpDob = it },
                            label = { Text("Date of birth YYYY-MM-DD (optional)") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text("Gender (optional)", style = MaterialTheme.typography.labelMedium)
                        val genderOptions = listOf("" to "Not set") + ProfileValidation.ALLOWED_GENDERS.map { it to it }
                        genderOptions.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEach { (value, label) ->
                                    FilterChip(
                                        selected = signUpGender == value,
                                        onClick = { if (!state.isBusy) signUpGender = value },
                                        label = { Text(label) },
                                        enabled = !state.isBusy,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Button(
                            onClick = {
                                onSubmitSignUp(
                                    NativeSignUpInput(
                                        firstName = signUpFirstName.trim(),
                                        lastName = signUpLastName.trim(),
                                        username = signUpUsername.trim().lowercase(),
                                        password = signUpPassword,
                                        recoveryEmail = signUpRecoveryEmail.trim(),
                                        phoneNumber = signUpPhone.trim(),
                                        dateOfBirth = signUpDob.trim(),
                                        gender = signUpGender.trim(),
                                    ),
                                )
                            },
                            enabled = !state.isBusy && firstValid && lastValid && userValid && passValid && emailValid && phoneValid && dobValid && genderValid,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("native_auth_signup_submit_button"),
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Create account")
                            }
                        }

                        TextButton(
                            onClick = { onSwitchStep(NativeAuthStep.IDENTIFIER) },
                            enabled = !state.isBusy,
                        ) {
                            Text("Already have an account? Sign in")
                        }
                    }

                    NativeAuthStep.FORGOT_PASSWORD -> {
                        Text(
                            text = "Enter your Bansify username to request a password reset token via your verified recovery email.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = forgotUsername,
                            onValueChange = { forgotUsername = it.lowercase() },
                            label = { Text("Username") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { onSubmitForgotPassword(forgotUsername) },
                            enabled = !state.isBusy && forgotUsername.trim().isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Send reset token")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                onClick = { onSwitchStep(NativeAuthStep.RESET_PASSWORD) },
                                enabled = !state.isBusy,
                            ) {
                                Text("Have a reset token?")
                            }
                            TextButton(
                                onClick = { onSwitchStep(NativeAuthStep.IDENTIFIER) },
                                enabled = !state.isBusy,
                            ) {
                                Text("Back to sign in")
                            }
                        }
                    }

                    NativeAuthStep.RESET_PASSWORD -> {
                        Text(
                            text = "Paste the reset token from your recovery email and choose a new password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = resetToken,
                            onValueChange = { resetToken = it },
                            label = { Text("Reset token") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = resetPassword,
                            onValueChange = { resetPassword = it },
                            label = { Text("New password (8+ characters)") },
                            singleLine = true,
                            enabled = !state.isBusy,
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                    Icon(
                                        imageVector = if (resetPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { onSubmitResetPassword(resetToken, resetPassword) },
                            enabled = !state.isBusy && resetToken.trim().isNotEmpty() && ProfileValidation.isValidPassword(resetPassword),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Update password")
                            }
                        }
                        TextButton(
                            onClick = { onSwitchStep(NativeAuthStep.IDENTIFIER) },
                            enabled = !state.isBusy,
                        ) {
                            Text("Back to sign in")
                        }
                    }
                }

                HorizontalDivider(color = semantic.cardBorder)
                OutlinedButton(
                    onClick = onSignInWithBrowser,
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("native_auth_browser_fallback_button"),
                ) {
                    Text(stringResource(R.string.gothwad_sign_in_browser))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
