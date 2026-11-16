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
)

@Serializable
private data class AccountSecuritySummary(
    @SerialName("has_password") val hasPassword: Boolean? = null,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean? = null,
    @SerialName("passkey_count") val passkeyCount: Int? = null,
    @SerialName("linked_google_count") val linkedGoogleCount: Int? = null,
    @SerialName("active_sessions_count") val activeSessionsCount: Int? = null,
)

@Serializable
private data class UserInfo(
    val sub: String,
    @SerialName("preferred_username") val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val picture: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("recovery_email") val recoveryEmail: String? = null,
    @SerialName("recovery_email_verified") val recoveryEmailVerified: Boolean? = null,
    val security: AccountSecuritySummary? = null,
    val drive: DriveQuotaInfo? = null,
)

internal interface OAuthService {
    fun exchange(code: String, verifier: String): TokenResponse
    fun refresh(token: String): TokenResponse
    fun userInfo(token: String): GothwadAccount
    fun revoke(refreshToken: String?, accessToken: String? = null): Boolean = false
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
            email = info.email,
            avatarUrl = info.picture,
            createdAt = info.createdAt,
            updatedAt = info.updatedAt,
            phoneNumber = info.phoneNumber,
            recoveryEmail = info.recoveryEmail,
            recoveryEmailVerified = info.recoveryEmailVerified,
            hasPassword = info.security?.hasPassword,
            mfaEnabled = info.security?.mfaEnabled,
            passkeyCount = info.security?.passkeyCount,
            linkedGoogleCount = info.security?.linkedGoogleCount,
            activeSessionsCount = info.security?.activeSessionsCount,
            storageUsedBytes = info.drive?.usedBytes,
            storageLimitBytes = info.drive?.limitBytes,
        )
    }

    /**
     * Best-effort RFC 7009 token revocation for this client's refresh/access grant.
     * Returns true on 2xx acceptance; never throws on offline/unimplemented (404/405/501) so local
     * sign-out succeeds immediately and deterministically.
     */
    override fun revoke(refreshToken: String?, accessToken: String?): Boolean {
        val targetToken = refreshToken?.takeIf { it.isNotBlank() } ?: accessToken?.takeIf { it.isNotBlank() } ?: return false
        val form = FormBody.Builder()
            .add("client_id", config.clientId)
            .add("token", targetToken)
            .apply {
                if (!refreshToken.isNullOrBlank()) {
                    add("token_type_hint", "refresh_token")
                }
            }
            .build()
        val reqBuilder = Request.Builder().url("$base/oauth/revoke").post(form)
        if (!accessToken.isNullOrBlank()) {
            reqBuilder.header("Authorization", "Bearer $accessToken")
        }
        return runCatching {
            client.newCall(reqBuilder.build()).execute().use { it.isSuccessful }
        }.getOrDefault(false)
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
