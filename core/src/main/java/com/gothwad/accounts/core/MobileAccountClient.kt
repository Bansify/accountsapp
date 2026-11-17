package com.gothwad.accounts.core

import com.google.crypto.tink.subtle.Ed25519Verify
import java.io.IOException
import java.security.PublicKey
import java.security.Signature
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MobileApiException(
    val code: Int,
    val error: String,
    val errorCode: String? = null,
) : IOException(error)

@Serializable
private data class SessionsEnvelope(val sessions: List<WebSessionInfo> = emptyList())

@Serializable
private data class RevokeOthersEnvelope(val ok: Boolean = false, val revoked: Int = 0)

@Serializable
private data class ActivityEnvelope(val activity: List<AccountActivityEntry> = emptyList())

@Serializable
private data class AuthorizationsEnvelope(val authorizations: List<ConnectedAppGrant> = emptyList())

@Serializable
private data class ProfilePatchPayload(
    val first_name: String,
    val last_name: String,
    val date_of_birth: String,
    val gender: String,
)

@Serializable
private data class RevokeAppPayload(val app_id: String)

@Serializable
private data class CheckIdentifierPayload(val identifier: String)

@Serializable
private data class CheckIdentifierEnvelope(
    val ok: Boolean = false,
    val exists: Boolean = false,
    val kind: String? = null,
    val error: String? = null,
    val isThirdParty: Boolean = false,
)

@Serializable
private data class CheckUsernamePayload(val username: String)

@Serializable
private data class CheckUsernameEnvelope(
    val ok: Boolean = false,
    val available: Boolean = false,
    val valid: Boolean = false,
    val username: String? = null,
    val email: String? = null,
    val error: String? = null,
)

@Serializable
private data class NativeSignInPayload(
    val client_id: String,
    val scope: String,
    val identifier: String,
    val password: String,
)

@Serializable
private data class NativeMfaVerifyPayload(
    val client_id: String,
    val scope: String,
    val challenge: String,
    val code: String,
)

@Serializable
private data class NativeSignUpPayload(
    val client_id: String,
    val scope: String,
    val first_name: String,
    val last_name: String,
    val username: String,
    val password: String,
    val recovery_email: String,
    val phone_number: String = "",
    val date_of_birth: String = "",
    val gender: String = "",
)

@Serializable
private data class NativeAuthSuccessEnvelope(
    val ok: Boolean = false,
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600L,
    val scope: String = "",
    @SerialName("client_id") val clientId: String = "",
    @SerialName("session_id") val sessionId: String = "",
    val user: MobileAuthUser? = null,
)

@Serializable
private data class NativeMfaChallengeEnvelope(
    val ok: Boolean = false,
    @SerialName("mfa_required") val mfaRequired: Boolean = false,
    val challenge: String = "",
    @SerialName("expires_in") val expiresIn: Long = 300L,
    val message: String = "",
)

@Serializable
private data class ForgotPasswordPayload(val username: String)

@Serializable
private data class ResetPasswordPayload(val token: String, val password: String)

@Serializable
private data class OkMessageEnvelope(val ok: Boolean = false, val message: String = "")

@Serializable
private data class PeerCodePayload(
    val target_client_id: String,
    val redirect_uri: String,
    val scope: String,
    val code_challenge: String,
    val code_challenge_method: String = "S256",
)

@Serializable
private data class RevokeSessionPayload(val session_id: String)

@Serializable
private data class AvatarUploadPayload(val image_base64: String, val mime: String)

@Serializable
private data class AvatarEnvelope(
    val ok: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class PhoneUpdatePayload(
    val phone_number: String,
    val current_password: String,
    val mfa_code: String = "",
)

@Serializable
private data class RecoveryEmailRequestPayload(
    val recovery_email: String,
    val current_password: String,
    val mfa_code: String = "",
)

@Serializable
private data class RecoveryEmailVerifyPayload(val token: String)

@Serializable
private data class RecoveryEmailVerifyEnvelope(
    val ok: Boolean = false,
    @SerialName("recovery_email") val recoveryEmail: String? = null,
)

@Serializable
private data class ChangePasswordPayload(
    val current_password: String,
    val new_password: String,
    val mfa_code: String = "",
)

@Serializable
private data class MfaPasswordPayload(val current_password: String)

@Serializable
private data class MfaConfirmPayload(
    val current_password: String,
    val enrollment_id: String,
    val code: String,
)

@Serializable
private data class MfaActionPayload(
    val current_password: String,
    val code: String,
)

@Serializable
private data class MfaRecoveryCodesEnvelope(
    val ok: Boolean = false,
    @SerialName("recovery_codes") val recoveryCodes: List<String> = emptyList(),
)

@Serializable
private data class DeleteAccountPayload(
    val password: String,
    val mfa_code: String = "",
)

internal class MobileAccountClient(
    private val config: GothwadConfig,
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prettyJson = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val base = config.authBaseUrl.trimEnd('/')
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val scopeString = config.scopes.map { it.wireValue }.sorted().joinToString(" ")

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // -------------------------------------------------------------------------
    // Direct Native Authentication (`/api/mobile/auth/*`)
    // -------------------------------------------------------------------------

    fun checkIdentifier(identifier: String): IdentifierCheckResult {
        val clean = identifier.trim()
        require(clean.isNotEmpty() && clean.length <= 254) { "Invalid identifier" }
        val raw = postJsonUnauthenticated(
            "$base/mobile/auth/check-identifier",
            json.encodeToString(CheckIdentifierPayload(clean)),
        )
        val env = json.decodeFromString<CheckIdentifierEnvelope>(raw)
        return IdentifierCheckResult(
            exists = env.exists,
            kind = env.kind,
            error = env.error,
            isThirdParty = env.isThirdParty,
        )
    }

    fun checkUsername(username: String): UsernameCheckResult {
        val clean = username.trim().lowercase()
        val raw = postJsonUnauthenticated(
            "$base/mobile/auth/check-username",
            json.encodeToString(CheckUsernamePayload(clean)),
        )
        val env = json.decodeFromString<CheckUsernameEnvelope>(raw)
        return UsernameCheckResult(
            available = env.available,
            valid = env.valid,
            username = env.username,
            email = env.email,
            error = env.error,
        )
    }

    fun nativeSignIn(identifier: String, password: String): MobileAuthSignInOutcome {
        val cleanId = identifier.trim()
        require(cleanId.isNotEmpty() && cleanId.length <= 254) { "Identifier is required" }
        require(password.isNotEmpty() && password.length <= 128) { "Password is required" }
        val payload = NativeSignInPayload(
            client_id = config.clientId,
            scope = scopeString,
            identifier = cleanId,
            password = password,
        )
        val raw = postJsonUnauthenticated("$base/mobile/auth/signin", json.encodeToString(payload))
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
        val mfaRequired = obj?.get("mfa_required")?.jsonPrimitive?.booleanOrNull == true
        return if (mfaRequired) {
            val env = json.decodeFromString<NativeMfaChallengeEnvelope>(raw)
            require(env.challenge.isNotBlank()) { "Missing MFA challenge" }
            MobileAuthSignInOutcome.MfaRequired(
                challenge = env.challenge,
                expiresIn = env.expiresIn,
                message = env.message.ifBlank { "Enter the 6-digit code from your authenticator app or a recovery code." },
            )
        } else {
            decodeAuthenticatedEnvelope(raw)
        }
    }

    fun nativeVerifyMfa(challenge: String, code: String): MobileAuthSignInOutcome.Authenticated {
        val cleanChallenge = challenge.trim()
        val cleanCode = code.trim()
        require(cleanChallenge.isNotEmpty() && cleanCode.isNotEmpty()) { "Challenge and code are required" }
        val payload = NativeMfaVerifyPayload(
            client_id = config.clientId,
            scope = scopeString,
            challenge = cleanChallenge,
            code = cleanCode,
        )
        val raw = postJsonUnauthenticated("$base/mobile/auth/signin/mfa", json.encodeToString(payload))
        return decodeAuthenticatedEnvelope(raw)
    }

    fun nativeSignUp(input: NativeSignUpInput, todayIso: String = LocalDate.now().toString()): MobileAuthSignInOutcome.Authenticated {
        val fn = input.firstName.trim()
        val ln = input.lastName.trim()
        val un = input.username.trim().lowercase()
        val rec = input.recoveryEmail.trim().lowercase()
        val phone = input.phoneNumber.trim()
        val dob = input.dateOfBirth.trim()
        val gender = input.gender.trim()
        require(ProfileValidation.isValidFirstName(fn)) { "First name is required" }
        require(ln.isNotEmpty() && ProfileValidation.isValidLastName(ln)) { "Last name is required" }
        require(ProfileValidation.isValidUsername(un)) { "Username must be 3-24 letters or numbers" }
        require(ProfileValidation.isValidPassword(input.password)) { "Password must be 8-128 characters" }
        require(ProfileValidation.isValidEmail(rec)) { "Enter a valid recovery email" }
        require(ProfileValidation.isValidPhoneNumber(phone)) { "Enter a valid phone number" }
        require(ProfileValidation.isValidDateOfBirth(dob, todayIso)) { "Date of birth must be YYYY-MM-DD in the past" }
        require(ProfileValidation.isValidGender(gender)) { "Invalid gender selection" }

        val payload = NativeSignUpPayload(
            client_id = config.clientId,
            scope = scopeString,
            first_name = fn,
            last_name = ln,
            username = un,
            password = input.password,
            recovery_email = rec,
            phone_number = phone,
            date_of_birth = dob,
            gender = gender,
        )
        val raw = postJsonUnauthenticated("$base/mobile/auth/signup", json.encodeToString(payload))
        return decodeAuthenticatedEnvelope(raw)
    }

    fun forgotPassword(username: String): String {
        val clean = username.trim().lowercase()
        require(clean.isNotEmpty() && clean.length <= 254) { "Enter your username" }
        val raw = postJsonUnauthenticated(
            "$base/mobile/auth/forgot-password",
            json.encodeToString(ForgotPasswordPayload(clean)),
        )
        return runCatching { json.decodeFromString<OkMessageEnvelope>(raw).message }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "If an account with a verified recovery email exists, a reset message has been sent."
    }

    fun resetPassword(token: String, newPassword: String): String {
        val cleanToken = token.trim()
        require(cleanToken.isNotEmpty()) { "Reset token is required" }
        require(ProfileValidation.isValidPassword(newPassword)) { "Password must be 8-128 characters" }
        val raw = postJsonUnauthenticated(
            "$base/mobile/auth/reset-password",
            json.encodeToString(ResetPasswordPayload(cleanToken, newPassword)),
        )
        return runCatching { json.decodeFromString<OkMessageEnvelope>(raw).message }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Password updated. You can now sign in."
    }

    fun issuePeerAuthCode(
        accessToken: String,
        targetClientId: String,
        redirectUri: String,
        scope: String,
        codeChallenge: String,
        codeChallengeMethod: String = "S256",
    ): PeerAuthCodeResult {
        val payload = PeerCodePayload(
            target_client_id = targetClientId.trim(),
            redirect_uri = redirectUri.trim(),
            scope = scope.trim(),
            code_challenge = codeChallenge.trim(),
            code_challenge_method = codeChallengeMethod.trim().ifBlank { "S256" },
        )
        val raw = postJson("$base/mobile/auth/peer-code", json.encodeToString(payload), accessToken)
        return json.decodeFromString<PeerAuthCodeResult>(raw).also {
            require(it.code.isNotBlank()) { "Server returned empty peer authorization code" }
        }
    }

    private fun decodeAuthenticatedEnvelope(raw: String): MobileAuthSignInOutcome.Authenticated {
        val env = json.decodeFromString<NativeAuthSuccessEnvelope>(raw)
        require(env.accessToken.isNotBlank() && env.tokenType.equals("Bearer", ignoreCase = true)) {
            "Invalid authentication token payload"
        }
        return MobileAuthSignInOutcome.Authenticated(
            accessToken = env.accessToken,
            refreshToken = env.refreshToken,
            expiresIn = env.expiresIn.coerceIn(60L, 86400L),
            scope = env.scope,
            clientId = env.clientId.ifBlank { config.clientId },
            sessionId = env.sessionId,
            user = env.user,
        )
    }

    // -------------------------------------------------------------------------
    // Native Account & Security Management (`/api/mobile/*`)
    // -------------------------------------------------------------------------

    fun listSessions(accessToken: String): List<WebSessionInfo> =
        json.decodeFromString<SessionsEnvelope>(get("$base/mobile/sessions", accessToken)).sessions

    fun revokeOtherSessions(accessToken: String): Int {
        val body = postJson("$base/mobile/sessions/revoke-others", "{}", accessToken)
        return json.decodeFromString<RevokeOthersEnvelope>(body).revoked
    }

    fun revokeSession(accessToken: String, sessionId: String): Int {
        val clean = sessionId.trim()
        require(clean.isNotEmpty() && clean.length <= 128) { "Invalid sessionId" }
        val body = postJson("$base/mobile/sessions/revoke", json.encodeToString(RevokeSessionPayload(clean)), accessToken)
        return json.decodeFromString<RevokeOthersEnvelope>(body).revoked
    }

    fun listActivity(accessToken: String): List<AccountActivityEntry> =
        json.decodeFromString<ActivityEnvelope>(get("$base/mobile/activity", accessToken)).activity

    fun listAuthorizations(accessToken: String): List<ConnectedAppGrant> =
        json.decodeFromString<AuthorizationsEnvelope>(get("$base/mobile/authorizations", accessToken)).authorizations

    fun revokeAuthorization(accessToken: String, appId: String) {
        val clean = appId.trim()
        require(clean.isNotEmpty() && clean.length <= 100) { "Invalid appId" }
        postJson("$base/mobile/authorizations/revoke", json.encodeToString(RevokeAppPayload(clean)), accessToken)
    }

    fun patchProfile(
        accessToken: String,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
        todayIso: String = LocalDate.now().toString(),
    ) {
        val fn = firstName.trim()
        val ln = lastName.trim()
        val dob = dateOfBirth.trim()
        val gen = gender.trim()
        require(ProfileValidation.isValidFirstName(fn)) { "Invalid first_name" }
        require(ProfileValidation.isValidLastName(ln)) { "Invalid last_name" }
        require(ProfileValidation.isValidDateOfBirth(dob, todayIso)) { "Invalid date_of_birth" }
        require(ProfileValidation.isValidGender(gen)) { "Invalid gender" }
        val body = json.encodeToString(ProfilePatchPayload(fn, ln, dob, gen))
        execute(
            Request.Builder()
                .url("$base/mobile/profile")
                .header("Authorization", "Bearer $accessToken")
                .patch(body.toRequestBody(jsonMediaType))
                .build(),
        )
    }

    fun uploadAvatar(accessToken: String, imageDataOrBase64: String, mime: String = "image/jpeg"): String? {
        require(imageDataOrBase64.isNotBlank()) { "Image data is required" }
        val raw = postJson(
            "$base/mobile/avatar",
            json.encodeToString(AvatarUploadPayload(imageDataOrBase64, mime)),
            accessToken,
        )
        return json.decodeFromString<AvatarEnvelope>(raw).avatarUrl
    }

    fun deleteAvatar(accessToken: String) {
        execute(
            Request.Builder()
                .url("$base/mobile/avatar")
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build(),
        )
    }

    fun updatePhoneNumber(
        accessToken: String,
        phoneNumber: String,
        currentPassword: String,
        mfaCode: String? = null,
    ) {
        val cleanPhone = phoneNumber.trim()
        require(ProfileValidation.isValidPhoneNumber(cleanPhone)) { "Enter a valid phone number" }
        postJson(
            "$base/mobile/phone",
            json.encodeToString(
                PhoneUpdatePayload(
                    phone_number = cleanPhone,
                    current_password = currentPassword,
                    mfa_code = mfaCode?.trim().orEmpty(),
                ),
            ),
            accessToken,
        )
    }

    fun requestRecoveryEmailChange(
        accessToken: String,
        recoveryEmail: String,
        currentPassword: String,
        mfaCode: String? = null,
    ): String {
        val clean = recoveryEmail.trim().lowercase()
        require(ProfileValidation.isValidEmail(clean)) { "Enter a valid recovery email" }
        val raw = postJson(
            "$base/mobile/recovery-email/request",
            json.encodeToString(
                RecoveryEmailRequestPayload(
                    recovery_email = clean,
                    current_password = currentPassword,
                    mfa_code = mfaCode?.trim().orEmpty(),
                ),
            ),
            accessToken,
        )
        return runCatching { json.decodeFromString<OkMessageEnvelope>(raw).message }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Check the new recovery email for your verification token."
    }

    fun verifyRecoveryEmailChange(accessToken: String, token: String): String? {
        val clean = token.trim()
        require(clean.isNotEmpty()) { "Verification token is required" }
        val raw = postJson(
            "$base/mobile/recovery-email/verify",
            json.encodeToString(RecoveryEmailVerifyPayload(clean)),
            accessToken,
        )
        return json.decodeFromString<RecoveryEmailVerifyEnvelope>(raw).recoveryEmail
    }

    fun changePassword(
        accessToken: String,
        currentPassword: String,
        newPassword: String,
        mfaCode: String? = null,
    ): String {
        require(ProfileValidation.isValidPassword(newPassword)) { "New password must be between 8 and 128 characters" }
        val raw = postJson(
            "$base/mobile/password",
            json.encodeToString(
                ChangePasswordPayload(
                    current_password = currentPassword,
                    new_password = newPassword,
                    mfa_code = mfaCode?.trim().orEmpty(),
                ),
            ),
            accessToken,
        )
        return runCatching { json.decodeFromString<OkMessageEnvelope>(raw).message }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Password saved."
    }

    fun getMfaStatus(accessToken: String): MfaStatusInfo =
        json.decodeFromString<MfaStatusInfo>(get("$base/mobile/mfa/status", accessToken))

    fun beginMfaEnrollment(accessToken: String, currentPassword: String): MfaEnrollmentSetup {
        val raw = postJson(
            "$base/mobile/mfa/enroll",
            json.encodeToString(MfaPasswordPayload(currentPassword)),
            accessToken,
        )
        return json.decodeFromString<MfaEnrollmentSetup>(raw)
    }

    fun confirmMfaEnrollment(
        accessToken: String,
        currentPassword: String,
        enrollmentId: String,
        code: String,
    ): List<String> {
        require(enrollmentId.isNotBlank() && code.trim().isNotEmpty()) { "Enrollment ID and verification code are required" }
        val raw = postJson(
            "$base/mobile/mfa/confirm",
            json.encodeToString(MfaConfirmPayload(currentPassword, enrollmentId.trim(), code.trim())),
            accessToken,
        )
        return json.decodeFromString<MfaRecoveryCodesEnvelope>(raw).recoveryCodes
    }

    fun regenerateMfaRecoveryCodes(accessToken: String, currentPassword: String, code: String): List<String> {
        require(code.trim().isNotEmpty()) { "Verification code is required" }
        val raw = postJson(
            "$base/mobile/mfa/recovery-codes",
            json.encodeToString(MfaActionPayload(currentPassword, code.trim())),
            accessToken,
        )
        return json.decodeFromString<MfaRecoveryCodesEnvelope>(raw).recoveryCodes
    }

    fun disableMfa(accessToken: String, currentPassword: String, code: String) {
        require(code.trim().isNotEmpty()) { "Verification code is required" }
        postJson(
            "$base/mobile/mfa/disable",
            json.encodeToString(MfaActionPayload(currentPassword, code.trim())),
            accessToken,
        )
    }

    fun exportAccountDataJson(accessToken: String): String {
        val raw = get("$base/mobile/export", accessToken)
        return runCatching {
            val element = prettyJson.parseToJsonElement(raw)
            prettyJson.encodeToString(JsonElement.serializer(), element)
        }.getOrDefault(raw)
    }

    fun deleteAccount(accessToken: String, password: String, mfaCode: String? = null) {
        postJson(
            "$base/mobile/delete-account",
            json.encodeToString(DeleteAccountPayload(password = password, mfa_code = mfaCode?.trim().orEmpty())),
            accessToken,
        )
    }

    // -------------------------------------------------------------------------
    // Signed Trusted Apps Manifest (`/api/apps/trusted`)
    // -------------------------------------------------------------------------

    fun fetchTrustedApps(
        pinnedPublicKey: PublicKey? = null,
        expectedKeyId: String? = null,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): TrustedAppsManifest {
        val raw = client.newCall(Request.Builder().url("$base/apps/trusted").get().build()).execute().use { res ->
            val body = res.body?.string() ?: throw IOException("Empty trusted apps response")
            if (!res.isSuccessful) throw IOException("Failed to fetch trusted apps (${res.code})")
            body
        }
        val manifest = json.decodeFromString<TrustedAppsManifest>(raw)
        if (pinnedPublicKey != null) {
            return verifySignedManifest(
                rawManifest = manifest,
                pinnedPublicKey = pinnedPublicKey,
                expectedIssuer = config.authBaseUrl.substringBefore("/api").trimEnd('/'),
                expectedKeyId = expectedKeyId,
                nowEpochSeconds = nowEpochSeconds,
            )
        }
        return manifest
    }

    /**
     * Verifies JWS signature, algorithm (`EdDSA` or `ES256`), key ID, issuer, and expiration against
     * an independently pinned public key. Rejects `signed == false` or empty cert arrays strictly.
     */
    fun verifySignedManifest(
        rawManifest: TrustedAppsManifest,
        pinnedPublicKey: PublicKey,
        expectedIssuer: String,
        expectedKeyId: String?,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): TrustedAppsManifest {
        val jws = rawManifest.jws?.takeIf { rawManifest.signed && it.isNotBlank() }
            ?: throw SecurityException("Trusted apps manifest is unsigned")
        val parts = jws.split('.')
        if (parts.size != 3) throw SecurityException("Malformed JWS")

        val decoder = Base64.getUrlDecoder()
        val headerObj = json.parseToJsonElement(decoder.decode(parts[0]).toString(Charsets.UTF_8)).jsonObject
        val alg = headerObj["alg"]?.jsonPrimitive?.content.orEmpty()
        val kid = headerObj["kid"]?.jsonPrimitive?.content.orEmpty()
        if (expectedKeyId != null && kid != expectedKeyId) {
            throw SecurityException("Unexpected signing key id")
        }
        val sigAlg = when (alg) {
            "EdDSA", "ES256" -> alg
            else -> throw SecurityException("Unsupported JWS alg: $alg")
        }

        val signingInput = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
        val rawSig = decoder.decode(parts[2])
        val verified = when (sigAlg) {
            "EdDSA" -> verifyEd25519(pinnedPublicKey, signingInput, rawSig)
            "ES256" -> {
                val sigBytes = joseEcdsaToDer(rawSig)
                Signature.getInstance("SHA256withECDSA").run {
                    initVerify(pinnedPublicKey)
                    update(signingInput)
                    verify(sigBytes)
                }
            }
            else -> false
        }
        if (!verified) {
            throw SecurityException("Invalid JWS signature")
        }

        val verifiedPayload = json.decodeFromString<TrustedAppsManifest>(
            decoder.decode(parts[1]).toString(Charsets.UTF_8),
        )
        if (verifiedPayload.issuer.trimEnd('/') != expectedIssuer.trimEnd('/')) {
            throw SecurityException("Trusted apps issuer mismatch")
        }
        if (verifiedPayload.expiresAt <= nowEpochSeconds || verifiedPayload.issuedAt > nowEpochSeconds + 60) {
            throw SecurityException("Trusted apps manifest expired or not yet valid")
        }
        val filteredApps = verifiedPayload.apps.filter { entry ->
            entry.androidPackage.isNotBlank() &&
                entry.androidCertSha256.isNotEmpty() &&
                entry.androidCertSha256.all { it.matches(Regex("^[0-9a-f]{64}$")) }
        }
        return verifiedPayload.copy(apps = filteredApps, signed = true, jws = jws)
    }

    private fun verifyEd25519(publicKey: PublicKey, data: ByteArray, rawSignature: ByteArray): Boolean {
        val spki = publicKey.encoded
        if (spki != null && spki.size == 44 && spki[0] == 0x30.toByte() && spki[1] == 0x2a.toByte()) {
            val rawKey = spki.copyOfRange(12, 44)
            return runCatching {
                Ed25519Verify(rawKey).verify(rawSignature, data)
                true
            }.getOrDefault(false)
        }
        return runCatching {
            Signature.getInstance("Ed25519").run {
                initVerify(publicKey)
                update(data)
                verify(rawSignature)
            }
        }.getOrDefault(false)
    }

    private fun joseEcdsaToDer(jose: ByteArray): ByteArray {
        if (jose.size != 64) return jose
        val r = toDerInt(jose.copyOfRange(0, 32))
        val s = toDerInt(jose.copyOfRange(32, 64))
        val seqLen = 2 + r.size + 2 + s.size
        val out = ByteArray(2 + seqLen)
        out[0] = 0x30
        out[1] = seqLen.toByte()
        out[2] = 0x02
        out[3] = r.size.toByte()
        System.arraycopy(r, 0, out, 4, r.size)
        val sPos = 4 + r.size
        out[sPos] = 0x02
        out[sPos + 1] = s.size.toByte()
        System.arraycopy(s, 0, out, sPos + 2, s.size)
        return out
    }

    private fun toDerInt(raw: ByteArray): ByteArray {
        var offset = 0
        while (offset < raw.size - 1 && raw[offset].toInt() == 0) offset++
        val sliced = raw.copyOfRange(offset, raw.size)
        return if ((sliced[0].toInt() and 0x80) != 0) byteArrayOf(0) + sliced else sliced
    }

    private fun get(url: String, accessToken: String): String =
        execute(
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build(),
        )

    private fun postJson(url: String, jsonBody: String, accessToken: String): String =
        execute(
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build(),
        )

    private fun postJsonUnauthenticated(url: String, jsonBody: String): String =
        execute(
            Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build(),
        )

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string() ?: throw IOException("Empty mobile API response")
        if (!response.isSuccessful) {
            val obj = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
            val errorCode = obj?.get("error")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val humanMessage = obj?.get("message")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: errorCode
                ?: "http_${response.code}"
            throw MobileApiException(response.code, humanMessage, errorCode)
        }
        body
    }
}
