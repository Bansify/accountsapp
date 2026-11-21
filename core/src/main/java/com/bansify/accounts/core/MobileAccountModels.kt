package com.bansify.accounts.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebSessionInfo(
    val id: String,
    @SerialName("device_name") val deviceName: String? = null,
    val browser: String? = null,
    val os: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("last_active") val lastActive: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val current: Boolean = false,
)

@Serializable
data class AccountActivityEntry(
    val id: String,
    val type: String,
    val title: String,
    val detail: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ConnectedAppGrant(
    @SerialName("app_id") val appId: String,
    val name: String,
    val scopes: List<String> = emptyList(),
    @SerialName("granted_at") val grantedAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)

@Serializable
data class TrustedAppRecord(
    @SerialName("client_id") val clientId: String,
    @SerialName("android_package") val androidPackage: String,
    @SerialName("android_cert_sha256") val androidCertSha256: List<String> = emptyList(),
)

@Serializable
data class TrustedAppsManifest(
    val version: Int,
    val issuer: String,
    @SerialName("issued_at") val issuedAt: Long,
    @SerialName("expires_at") val expiresAt: Long,
    val apps: List<TrustedAppRecord> = emptyList(),
    val signed: Boolean = false,
    val jws: String? = null,
)

@Serializable
data class IdentifierCheckResult(
    val exists: Boolean,
    val kind: String? = null,
    val error: String? = null,
    val isThirdParty: Boolean = false,
)

@Serializable
data class UsernameCheckResult(
    val available: Boolean,
    val valid: Boolean,
    val username: String? = null,
    val email: String? = null,
    val error: String? = null,
)

@Serializable
data class MobileAuthUser(
    val id: String,
    val username: String? = null,
    val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
) {
    fun toGothwadAccount(): GothwadAccount {
        val fullName = listOfNotNull(
            firstName?.trim()?.takeIf { it.isNotEmpty() },
            lastName?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" ").takeIf { it.isNotEmpty() }
        return GothwadAccount(
            userId = id,
            username = username,
            name = fullName,
            firstName = firstName,
            lastName = lastName,
            email = email,
            avatarUrl = avatarUrl,
        )
    }
}

sealed interface MobileAuthSignInOutcome {
    data class Authenticated(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Long,
        val scope: String,
        val clientId: String,
        val sessionId: String,
        val user: MobileAuthUser? = null,
    ) : MobileAuthSignInOutcome

    data class MfaRequired(
        val challenge: String,
        val expiresIn: Long,
        val message: String,
    ) : MobileAuthSignInOutcome
}

data class NativeSignUpInput(
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val recoveryEmail: String,
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
)

@Serializable
data class PeerAuthCodeResult(
    val code: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("redirect_uri") val redirectUri: String,
    val scope: String,
    @SerialName("expires_in") val expiresIn: Long = 180L,
)

@Serializable
data class MfaStatusInfo(
    val enabled: Boolean,
    @SerialName("enrollment_available") val enrollmentAvailable: Boolean = true,
    @SerialName("recovery_codes_remaining") val recoveryCodesRemaining: Int = 0,
)

@Serializable
data class MfaEnrollmentSetup(
    @SerialName("enrollment_id") val enrollmentId: String,
    val secret: String,
    @SerialName("otpauth_uri") val otpauthUri: String,
)

object ProfileValidation {
    val ALLOWED_GENDERS: List<String> = listOf("Male", "Female", "Other", "Rather not say")
    private val CONTROL_OR_ANGLE = Regex("[\\u0000-\\u001f\\u007f<>]")
    private val DATE_FORMAT = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val USERNAME_FORMAT = Regex("^[a-zA-Z0-9]{3,24}$")
    private val PHONE_FORMAT = Regex("^\\+?[0-9 ()-]{7,24}$")
    private val EMAIL_FORMAT = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidFirstName(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 100 && !CONTROL_OR_ANGLE.containsMatchIn(trimmed)
    }

    fun isValidLastName(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.length <= 100 && !CONTROL_OR_ANGLE.containsMatchIn(trimmed)
    }

    fun isValidUsername(value: String): Boolean =
        USERNAME_FORMAT.matches(value.trim())

    fun isValidPassword(value: String): Boolean =
        value.length in 8..128

    fun isValidEmail(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.length <= 254 && EMAIL_FORMAT.matches(trimmed)
    }

    fun isValidPhoneNumber(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isEmpty() || (trimmed.length <= 32 && PHONE_FORMAT.matches(trimmed))
    }

    fun isValidDateOfBirth(value: String, todayIso: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return true
        if (!DATE_FORMAT.matches(trimmed)) return false
        if (trimmed < "1900-01-01" || trimmed >= todayIso) return false
        return runCatching {
            java.time.LocalDate.parse(trimmed).toString() == trimmed
        }.getOrDefault(false)
    }

    fun isValidGender(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isEmpty() || trimmed in ALLOWED_GENDERS
    }
}
