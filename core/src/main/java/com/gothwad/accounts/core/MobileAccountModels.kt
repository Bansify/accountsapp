package com.gothwad.accounts.core

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

object ProfileValidation {
    val ALLOWED_GENDERS: List<String> = listOf("Male", "Female", "Other", "Rather not say")
    private val CONTROL_OR_ANGLE = Regex("[\\u0000-\\u001f\\u007f<>]")
    private val DATE_FORMAT = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun isValidFirstName(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 100 && !CONTROL_OR_ANGLE.containsMatchIn(trimmed)
    }

    fun isValidLastName(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.length <= 100 && !CONTROL_OR_ANGLE.containsMatchIn(trimmed)
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
