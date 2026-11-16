package com.gothwad.accounts.core

import com.google.crypto.tink.subtle.Ed25519Verify
import java.io.IOException
import java.security.PublicKey
import java.security.Signature
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class MobileApiException(val code: Int, val error: String) : IOException("Mobile API error ($code): $error")

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

internal class MobileAccountClient(
    private val config: GothwadConfig,
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val base = config.authBaseUrl.trimEnd('/')
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun listSessions(accessToken: String): List<WebSessionInfo> =
        json.decodeFromString<SessionsEnvelope>(get("$base/mobile/sessions", accessToken)).sessions

    fun revokeOtherSessions(accessToken: String): Int {
        val body = postJson("$base/mobile/sessions/revoke-others", "{}", accessToken)
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
        // Only treat entries with non-empty valid 64-hex SHA-256 certs as broker candidates.
        val filteredApps = verifiedPayload.apps.filter { entry ->
            entry.androidPackage.isNotBlank() &&
                entry.androidCertSha256.isNotEmpty() &&
                entry.androidCertSha256.all { it.matches(Regex("^[0-9a-f]{64}$")) }
        }
        return verifiedPayload.copy(apps = filteredApps, signed = true, jws = jws)
    }

    private fun verifyEd25519(publicKey: PublicKey, data: ByteArray, rawSignature: ByteArray): Boolean {
        val spki = publicKey.encoded
        // Standard Ed25519 X.509 SPKI prefix (12 bytes) + 32-byte raw key
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

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string() ?: throw IOException("Empty mobile API response")
        if (!response.isSuccessful) {
            val error = runCatching {
                (json.parseToJsonElement(body) as? JsonObject)?.get("error")?.jsonPrimitive?.content
            }.getOrNull() ?: "http_${response.code}"
            throw MobileApiException(response.code, error)
        }
        body
    }
}
