package com.gothwad.accounts.core

import java.net.InetAddress
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OAuthClientSecurityTest {

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
        val serverCerts = HandshakeCertificates.Builder()
            .heldCertificate(cert)
            .build()
        clientCerts = HandshakeCertificates.Builder()
            .addTrustedCertificate(cert.certificate)
            .build()
        server = MockWebServer()
        server.useHttps(serverCerts.sslSocketFactory(), false)
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun testOAuthClient(): OAuthClient {
        val baseUrl = server.url("/api").toString().trimEnd('/')
        val config = GothwadConfig(
            clientId = "gothwad-accounts-android",
            redirectUri = "com.gothwad.accounts:/oauth/callback",
            authBaseUrl = baseUrl,
        )
        val okHttp = OkHttpClient.Builder()
            .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
        return OAuthClient(config, okHttp)
    }

    @Test
    fun parsesExactAccountsUserInfoContractIncludingExtendedProfileAndSecurity() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "sub": "user-123",
                      "preferred_username": "arjun",
                      "name": "Arjun Sharma",
                      "given_name": "Arjun",
                      "family_name": "Sharma",
                      "email": "arjun@gothwadtech.com",
                      "picture": "https://accounts.gothwadtech.com/api/avatars/user-123?v=abc",
                      "created_at": "2026-01-01T00:00:00Z",
                      "updated_at": "2026-11-16T00:00:00Z",
                      "date_of_birth": "1998-05-14",
                      "gender": "Male",
                      "phone_number": "+919876543210",
                      "phone_number_verified": true,
                      "recovery_email": "arjun.recovery@example.com",
                      "recovery_email_verified": true,
                      "security": {
                        "has_password": true,
                        "password_changed_at": "2026-10-01T12:00:00Z",
                        "mfa_enabled": true,
                        "passkey_count": 2,
                        "authorization_count": 4,
                        "session_count": 3
                      },
                      "drive": {
                        "used_bytes": 5242880,
                        "limit_bytes": 16106127360,
                        "write_access": false
                      }
                    }
                    """.trimIndent(),
                ),
        )
        val account = testOAuthClient().userInfo("tok")
        assertEquals("user-123", account.userId)
        assertEquals("arjun", account.username)
        assertEquals("Arjun Sharma", account.name)
        assertEquals("Arjun", account.firstName)
        assertEquals("Sharma", account.lastName)
        assertEquals("1998-05-14", account.dateOfBirth)
        assertEquals("Male", account.gender)
        assertEquals("arjun@gothwadtech.com", account.email)
        assertEquals("https://accounts.gothwadtech.com/api/avatars/user-123?v=abc", account.avatarUrl)
        assertEquals("2026-01-01T00:00:00Z", account.createdAt)
        assertEquals("+919876543210", account.phoneNumber)
        assertEquals(true, account.phoneNumberVerified)
        assertEquals("arjun.recovery@example.com", account.recoveryEmail)
        assertEquals(true, account.recoveryEmailVerified)
        assertEquals(true, account.hasPassword)
        assertEquals("2026-10-01T12:00:00Z", account.passwordChangedAt)
        assertEquals(true, account.mfaEnabled)
        assertEquals(2, account.passkeyCount)
        assertEquals(4, account.authorizationCount)
        assertEquals(3, account.activeSessionsCount)
        assertEquals(5242880L, account.storageUsedBytes)
        assertEquals(16106127360L, account.storageLimitBytes)
    }

    @Test
    fun invalidGrantIsRejectedWhile503IsTransientOutage() {
        val oauth = testOAuthClient()

        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"invalid_grant","error_description":"Revoked"}"""),
        )
        try {
            oauth.refresh("bad-rt")
            fail("Expected OAuthException")
        } catch (err: OAuthException) {
            assertTrue("invalid_grant must mark rejected=true", err.rejected)
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"error":"temporarily_unavailable"}"""),
        )
        try {
            oauth.refresh("good-rt")
            fail("Expected OAuthException")
        } catch (err: OAuthException) {
            assertFalse("503 outage must NOT mark rejected=true", err.rejected)
        }
    }

    @Test
    fun oauthClientDoesNotFollowRedirectsAutomatically() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "https://evil.example/steal-token"),
        )
        try {
            testOAuthClient().userInfo("secret-access-token")
            fail("Redirect must be rejected, not followed")
        } catch (err: OAuthException) {
            assertFalse(err.rejected)
        }
        assertNull(server.takeRequest().getHeader("Referer"))
    }

    @Test
    fun revokePostsClientAndRefreshTokenAndDistinguishesRevokedInvalidAndOutage() {
        val oauth = testOAuthClient()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        assertEquals(RevokeStatus.Revoked, oauth.revoke("rt-123", "at-456"))
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/oauth/revoke", recorded.path)
        assertEquals("Bearer at-456", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("client_id=gothwad-accounts-android"))
        assertTrue(body.contains("token=rt-123"))
        assertTrue(body.contains("token_type_hint=refresh_token"))

        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))
        assertEquals(RevokeStatus.InvalidGrant, oauth.revoke("rt-123", "at-456"))

        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(RevokeStatus.Failed, oauth.revoke("rt-123", "at-456"))
    }

    @Test
    fun productionConfigCanRequireHttpsAppLinkRedirects() {
        assertThrows(IllegalArgumentException::class.java) {
            GothwadConfig(
                clientId = "gothwad-tube",
                redirectUri = "com.gothwad.tube:/oauth/callback",
                allowCustomSchemeRedirect = false,
            )
        }
        val prodConfig = GothwadConfig(
            clientId = "gothwad-tube",
            redirectUri = "https://tube.gothwadtech.com/oauth/callback",
            allowCustomSchemeRedirect = false,
        )
        assertEquals("https://tube.gothwadtech.com/oauth/callback", prodConfig.redirectUri)
    }
}
