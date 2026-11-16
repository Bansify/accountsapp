package com.gothwad.accounts.core

import java.net.InetAddress
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MobileAccountClientTest {

    private lateinit var server: MockWebServer
    private lateinit var clientCerts: HandshakeCertificates

    @Before
    fun setUp() {
        val localhost = InetAddress.getByName("127.0.0.1").canonicalHostName
        val cert = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName(localhost)
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        val serverCerts = HandshakeCertificates.Builder().heldCertificate(cert).build()
        clientCerts = HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        server = MockWebServer()
        server.useHttps(serverCerts.sslSocketFactory(), false)
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun testClient(): MobileAccountClient {
        val baseUrl = server.url("/api").toString().trimEnd('/')
        val config = GothwadConfig(
            clientId = "gothwad-accounts-android",
            redirectUri = "com.gothwad.accounts:/oauth/callback",
            authBaseUrl = baseUrl,
            scopes = setOf(Scope.Profile, Scope.Email, Scope.OfflineAccess, Scope.AccountRead, Scope.AccountProfileWrite),
        )
        val okHttp = OkHttpClient.Builder()
            .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
        return MobileAccountClient(config, okHttp)
    }

    @Test
    fun fetchesSessionsActivityAuthorizationsAndPatchesProfile() {
        val client = testClient()

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"sessions":[{"id":"s1","device_name":"Pixel","browser":"Chrome","os":"Android","ip_address":"198.51.100.1","current":true}]}""",
            ),
        )
        val sessions = client.listSessions("tok-1")
        assertEquals(1, sessions.size)
        assertTrue(sessions.single().current)
        assertEquals("/api/mobile/sessions", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"revoked":2}"""))
        assertEquals(2, client.revokeOtherSessions("tok-1"))
        assertEquals("/api/mobile/sessions/revoke-others", server.takeRequest().path)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"activity":[{"id":"e1","type":"signin","title":"Signed in","detail":"Android Chrome"}]}""",
            ),
        )
        assertEquals("signin", client.listActivity("tok-1").single().type)
        assertEquals("/api/mobile/activity", server.takeRequest().path)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"authorizations":[{"app_id":"gothwad-mail","name":"Gothwad Mail","scopes":["profile","mail"]}]}""",
            ),
        )
        val grants = client.listAuthorizations("tok-1")
        assertEquals("gothwad-mail", grants.single().appId)
        assertEquals("/api/mobile/authorizations", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        client.revokeAuthorization("tok-1", "gothwad-mail")
        val revokeReq = server.takeRequest()
        assertEquals("/api/mobile/authorizations/revoke", revokeReq.path)
        assertTrue(revokeReq.body.readUtf8().contains("gothwad-mail"))

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        client.patchProfile("tok-1", "Arjun", "Sharma", "1998-05-14", "Male", todayIso = "2026-11-16")
        val patchReq = server.takeRequest()
        assertEquals("PATCH", patchReq.method)
        assertEquals("/api/mobile/profile", patchReq.path)
    }

    @Test
    fun profileValidationMatchesBackendRulesStrictly() {
        assertFalse(ProfileValidation.isValidFirstName(""))
        assertFalse(ProfileValidation.isValidFirstName("<script>"))
        assertTrue(ProfileValidation.isValidFirstName("Arjun"))
        assertTrue(ProfileValidation.isValidDateOfBirth("", "2026-11-16"))
        assertTrue(ProfileValidation.isValidDateOfBirth("1999-12-31", "2026-11-16"))
        assertFalse(ProfileValidation.isValidDateOfBirth("1899-01-01", "2026-11-16"))
        assertFalse(ProfileValidation.isValidDateOfBirth("2026-11-16", "2026-11-16"))
        assertTrue(ProfileValidation.isValidGender("Rather not say"))
        assertFalse(ProfileValidation.isValidGender("InvalidGender"))
    }

    @Test
    fun signedTrustedAppsJwsVerifiedStrictlyAndUnsignedOrEmptyCertsRejected() {
        val kpGen = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }
        val kp = kpGen.generateKeyPair()
        val now = 1_760_000_000L
        val issuer = server.url("").toString().trimEnd('/')
        val payloadJson = """
            {
              "version": 1,
              "issuer": "$issuer",
              "issued_at": $now,
              "expires_at": ${now + 900},
              "apps": [
                {
                  "client_id": "gothwad-mail",
                  "android_package": "com.gothwad.mail",
                  "android_cert_sha256": ["${"a".repeat(64)}"]
                },
                {
                  "client_id": "empty-cert-app",
                  "android_package": "com.gothwad.unpinned",
                  "android_cert_sha256": []
                }
              ]
            }
        """.trimIndent()
        val enc = Base64.getUrlEncoder().withoutPadding()
        val headerB64 = enc.encodeToString("""{"alg":"ES256","typ":"JWT","kid":"key-1"}""".toByteArray())
        val payloadB64 = enc.encodeToString(payloadJson.toByteArray())
        val signingInput = "$headerB64.$payloadB64".toByteArray(Charsets.US_ASCII)

        val derSig = Signature.getInstance("SHA256withECDSA").run {
            initSign(kp.private as ECPrivateKey)
            update(signingInput)
            sign()
        }
        val joseSig = derToJose64(derSig)
        val jws = "$headerB64.$payloadB64.${enc.encodeToString(joseSig)}"

        val client = testClient()

        // Valid signed response succeeds and filters out empty android_cert_sha256 entries
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"version":1,"issuer":"$issuer","issued_at":$now,"expires_at":${now + 900},"apps":[],"signed":true,"jws":"$jws"}""",
            ),
        )
        val verified = client.verifySignedManifest(
            rawManifest = client.fetchTrustedApps(),
            pinnedPublicKey = kp.public,
            expectedIssuer = issuer,
            expectedKeyId = "key-1",
            nowEpochSeconds = now + 10,
        )
        assertEquals(1, verified.apps.size)
        assertEquals("com.gothwad.mail", verified.apps.single().androidPackage)

        // Unsigned response MUST throw SecurityException
        assertThrows(SecurityException::class.java) {
            client.verifySignedManifest(
                rawManifest = TrustedAppsManifest(1, issuer, now, now + 900, emptyList(), signed = false, jws = null),
                pinnedPublicKey = kp.public,
                expectedIssuer = issuer,
                expectedKeyId = "key-1",
                nowEpochSeconds = now + 10,
            )
        }

        // Expired manifest MUST throw SecurityException
        assertThrows(SecurityException::class.java) {
            client.verifySignedManifest(
                rawManifest = TrustedAppsManifest(1, issuer, now, now + 900, emptyList(), signed = true, jws = jws),
                pinnedPublicKey = kp.public,
                expectedIssuer = issuer,
                expectedKeyId = "key-1",
                nowEpochSeconds = now + 1000,
            )
        }
    }

    private fun derToJose64(der: ByteArray): ByteArray {
        val rLen = der[3].toInt() and 0xFF
        val rStart = 4
        val sLen = der[4 + rLen + 1].toInt() and 0xFF
        val sStart = 4 + rLen + 2
        val out = ByteArray(64)
        copyUnsigned(der, rStart, rLen, out, 0)
        copyUnsigned(der, sStart, sLen, out, 32)
        return out
    }

    private fun copyUnsigned(src: ByteArray, srcOff: Int, len: Int, dst: ByteArray, dstOff: Int) {
        val cleanLen = if (len > 32) 32 else len
        val offset = srcOff + (len - cleanLen)
        System.arraycopy(src, offset, dst, dstOff + (32 - cleanLen), cleanLen)
    }
}
