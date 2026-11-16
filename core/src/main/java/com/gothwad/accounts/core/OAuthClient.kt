package com.gothwad.accounts.core

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

internal class OAuthException(val rejected: Boolean) : IOException("OAuth request failed")

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String,
)

@Serializable
private data class DriveQuotaInfo(
    @SerialName("used_bytes") val usedBytes: Long? = null,
    @SerialName("limit_bytes") val limitBytes: Long? = null,
    @SerialName("write_access") val writeAccess: Boolean? = null,
)

@Serializable
private data class AccountSecuritySummary(
    @SerialName("session_count") val sessionCount: Int? = null,
    @SerialName("authorization_count") val authorizationCount: Int? = null,
    @SerialName("passkey_count") val passkeyCount: Int? = null,
    @SerialName("has_password") val hasPassword: Boolean? = null,
    @SerialName("password_changed_at") val passwordChangedAt: String? = null,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean? = null,
)

@Serializable
private data class UserInfo(
    val sub: String,
    @SerialName("preferred_username") val username: String? = null,
    val name: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    val email: String? = null,
    val picture: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("phone_number_verified") val phoneNumberVerified: Boolean? = null,
    @SerialName("recovery_email") val recoveryEmail: String? = null,
    @SerialName("recovery_email_verified") val recoveryEmailVerified: Boolean? = null,
    val security: AccountSecuritySummary? = null,
    val drive: DriveQuotaInfo? = null,
)

internal enum class RevokeStatus {
    Revoked,
    InvalidGrant,
    Failed,
}

internal interface OAuthService {
    fun exchange(code: String, verifier: String): TokenResponse
    fun refresh(token: String): TokenResponse
    fun userInfo(token: String): GothwadAccount
    fun revoke(refreshToken: String?, accessToken: String?): RevokeStatus = RevokeStatus.Failed
}

internal class OAuthClient(
    private val config: GothwadConfig,
    private val client: OkHttpClient = defaultClient(),
) : OAuthService {
    private val json = Json { ignoreUnknownKeys = true }
    private val base = config.authBaseUrl.trimEnd('/')

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override fun exchange(code: String, verifier: String): TokenResponse = token(
        "grant_type" to "authorization_code",
        "code" to code,
        "code_verifier" to verifier,
        "redirect_uri" to config.redirectUri,
    )

    override fun refresh(token: String): TokenResponse =
        token("grant_type" to "refresh_token", "refresh_token" to token)

    private fun token(vararg fields: Pair<String, String>): TokenResponse {
        val form = FormBody.Builder().add("client_id", config.clientId)
        fields.forEach { (key, value) -> form.add(key, value) }
        return json.decodeFromString<TokenResponse>(
            execute(Request.Builder().url("$base/oauth/token").post(form.build()).build()),
        ).also {
            require(it.tokenType.equals("Bearer", ignoreCase = true) && it.accessToken.isNotBlank())
            require(it.expiresIn in 1..86400)
        }
    }

    override fun userInfo(token: String): GothwadAccount {
        val info = json.decodeFromString<UserInfo>(
            execute(
                Request.Builder()
                    .url("$base/oauth/userinfo")
                    .header("Authorization", "Bearer $token")
                    .build(),
            ),
        )
        require(info.sub.isNotBlank())
        return GothwadAccount(
            userId = info.sub,
            username = info.username,
            name = info.name,
            firstName = info.givenName,
            lastName = info.familyName,
            email = info.email,
            avatarUrl = info.picture,
            createdAt = info.createdAt,
            updatedAt = info.updatedAt,
            dateOfBirth = info.dateOfBirth,
            gender = info.gender,
            phoneNumber = info.phoneNumber,
            phoneNumberVerified = info.phoneNumberVerified,
            recoveryEmail = info.recoveryEmail,
            recoveryEmailVerified = info.recoveryEmailVerified,
            hasPassword = info.security?.hasPassword,
            passwordChangedAt = info.security?.passwordChangedAt,
            mfaEnabled = info.security?.mfaEnabled,
            passkeyCount = info.security?.passkeyCount,
            authorizationCount = info.security?.authorizationCount,
            activeSessionsCount = info.security?.sessionCount,
            storageUsedBytes = info.drive?.usedBytes,
            storageLimitBytes = info.drive?.limitBytes,
            driveWriteAccess = info.drive?.writeAccess,
        )
    }

    /**
     * Explicit server revocation (`POST /oauth/revoke` matching `oauth-native-revoke.ts`).
     * Requires a valid `Authorization: Bearer <accessToken>` plus form `client_id` & `token`.
     * Distinguishes server revocation (`Revoked`) from `InvalidGrant` vs offline/network errors (`Failed`).
     */
    override fun revoke(refreshToken: String?, accessToken: String?): RevokeStatus {
        val bearer = accessToken?.takeIf { it.isNotBlank() } ?: return RevokeStatus.Failed
        val targetToken = refreshToken?.takeIf { it.isNotBlank() } ?: bearer
        val hint = if (!refreshToken.isNullOrBlank()) "refresh_token" else "access_token"
        val form = FormBody.Builder()
            .add("client_id", config.clientId)
            .add("token", targetToken)
            .add("token_type_hint", hint)
            .build()
        val request = Request.Builder()
            .url("$base/oauth/revoke")
            .header("Authorization", "Bearer $bearer")
            .post(form)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> RevokeStatus.Revoked
                    response.code == 400 || response.code == 401 -> RevokeStatus.InvalidGrant
                    else -> RevokeStatus.Failed
                }
            }
        }.getOrDefault(RevokeStatus.Failed)
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string() ?: throw IOException("Empty OAuth response")
        if (!response.isSuccessful) {
            val error = runCatching {
                json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
            }.getOrNull()
            throw OAuthException(response.code in 400..499 && error == "invalid_grant")
        }
        body
    }
}
