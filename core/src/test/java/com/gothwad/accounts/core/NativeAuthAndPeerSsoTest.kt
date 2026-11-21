package com.gothwad.accounts.core

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.content.pm.Signature
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.gothwad.accounts.core.broker.AccountDirectoryProvider
import com.gothwad.accounts.core.broker.BrokerIpcContract
import com.gothwad.accounts.core.broker.DirectoryAccountRecord
import com.gothwad.accounts.core.broker.DirectorySnapshot
import com.gothwad.accounts.core.broker.KnownGothwadApps
import com.gothwad.accounts.core.broker.PeerDirectoryTransport
import java.io.IOException
import java.net.InetAddress
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBinder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
class NativeAuthAndPeerSsoTest {

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
        GothwadAccounts.resetForTesting()
        server.shutdown()
    }

    private fun testOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
        .hostnameVerifier { _, _ -> true }
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun testConfig(policy: BrokerPolicy = BrokerPolicy.Disabled): GothwadConfig = GothwadConfig(
        clientId = "gothwad-accounts-android",
        redirectUri = "com.gothwad.accounts:/oauth/callback",
        authBaseUrl = server.url("/api").toString().trimEnd('/'),
        scopes = setOf(
            Scope.Profile,
            Scope.Email,
            Scope.OfflineAccess,
            Scope.AccountRead,
            Scope.AccountProfileWrite,
        ),
        brokerPolicy = policy,
    )

    @Test
    fun nativeAuthEndpointsSupportSignInWithMfaSignUpAndPasswordReset() = runTest {
        val config = testConfig()
        val client = MobileAccountClient(config, testOkHttp())

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"exists":true,"kind":"username"}"""))
        val idCheck = client.checkIdentifier("arjun")
        assertTrue(idCheck.exists)
        assertEquals("username", idCheck.kind)
        assertEquals("/api/mobile/auth/check-identifier", server.takeRequest().path)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ok":true,"available":true,"valid":true,"username":"arjun99","email":"arjun99@bansify.com"}""",
            ),
        )
        val userCheck = client.checkUsername("arjun99")
        assertTrue(userCheck.available && userCheck.valid)
        assertEquals("/api/mobile/auth/check-username", server.takeRequest().path)

        // 202 MFA challenge followed by /signin/mfa -> 200 tokens
        server.enqueue(
            MockResponse().setResponseCode(202).setBody(
                """{"ok":true,"mfa_required":true,"challenge":"${"a".repeat(64)}","expires_in":300,"message":"Enter TOTP code"}""",
            ),
        )
        val challengeOutcome = client.nativeSignIn("arjun", "Password123!")
        assertTrue(challengeOutcome is MobileAuthSignInOutcome.MfaRequired)
        assertEquals("/api/mobile/auth/signin", server.takeRequest().path)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ok":true,"access_token":"at-native","refresh_token":"rt-native","token_type":"Bearer","expires_in":3600,"scope":"profile email offline_access","client_id":"gothwad-accounts-android","session_id":"sid-1","user":{"id":"u-1","username":"arjun","email":"arjun@bansify.com","first_name":"Arjun","last_name":"Verma"}}""",
            ),
        )
        val mfaSuccess = client.nativeVerifyMfa("a".repeat(64), "123456")
        assertEquals("at-native", mfaSuccess.accessToken)
        assertEquals("rt-native", mfaSuccess.refreshToken)
        assertEquals("u-1", mfaSuccess.user?.id)
        assertEquals("/api/mobile/auth/signin/mfa", server.takeRequest().path)

        // Sign-up 201
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"ok":true,"access_token":"at-new","refresh_token":"rt-new","token_type":"Bearer","expires_in":3600,"scope":"profile","client_id":"gothwad-accounts-android","session_id":"sid-2","user":{"id":"u-2","username":"meera","email":"meera@bansify.com"}}""",
            ),
        )
        val signUpSuccess = client.nativeSignUp(
            NativeSignUpInput(
                firstName = "Meera",
                lastName = "Rao",
                username = "meera",
                password = "Password123!",
                recoveryEmail = "meera@example.com",
                gender = "Female",
            ),
            todayIso = "2026-11-16",
        )
        assertEquals("at-new", signUpSuccess.accessToken)
        assertEquals("/api/mobile/auth/signup", server.takeRequest().path)

        // Forgot + reset password
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"message":"Reset token sent."}"""))
        assertEquals("Reset token sent.", client.forgotPassword("arjun"))
        assertEquals("/api/mobile/auth/forgot-password", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"message":"Password updated."}"""))
        assertEquals("Password updated.", client.resetPassword("tok-123", "NewPassword123!"))
        assertEquals("/api/mobile/auth/reset-password", server.takeRequest().path)
    }

    @Test
    fun mobileSecurityAndAccountManagementEndpointsMatchContract() = runTest {
        val client = MobileAccountClient(testConfig(), testOkHttp())

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"revoked":1}"""))
        assertEquals(1, client.revokeSession("tok-1", "sess-9"))
        assertEquals("/api/mobile/sessions/revoke", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"avatar_url":"https://accounts.bansify.com/api/avatars/u1?v=1"}"""))
        val avatarUrl = client.uploadAvatar("tok-1", "data:image/jpeg;base64,/9j/4AAQ", "image/jpeg")
        assertTrue(avatarUrl?.contains("/api/avatars/") == true)
        assertEquals("/api/mobile/avatar", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"avatar_url":null}"""))
        client.deleteAvatar("tok-1")
        val delReq = server.takeRequest()
        assertEquals("DELETE", delReq.method)
        assertEquals("/api/mobile/avatar", delReq.path)

        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"mfa_required","message":"Enter a fresh authenticator code or unused recovery code."}"""))
        val mfaEx = assertThrows(MobileApiException::class.java) {
            client.updatePhoneNumber("tok-1", "+919876543210", "Pass123!")
        }
        assertEquals("mfa_required", mfaEx.errorCode)
        assertEquals("/api/mobile/phone", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"pending":true,"message":"Verification sent"}"""))
        assertEquals("Verification sent", client.requestRecoveryEmailChange("tok-1", "new@example.com", "Pass123!"))
        assertEquals("/api/mobile/recovery-email/request", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"recovery_email":"new@example.com","recovery_email_verified":true}"""))
        assertEquals("new@example.com", client.verifyRecoveryEmailChange("tok-1", "a".repeat(64)))
        assertEquals("/api/mobile/recovery-email/verify", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"message":"Password saved."}"""))
        assertEquals("Password saved.", client.changePassword("tok-1", "Pass123!", "NewPass123!"))
        assertEquals("/api/mobile/password", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"enabled":false,"enrollment_available":true,"recovery_codes_remaining":0}"""))
        assertFalse(client.getMfaStatus("tok-1").enabled)
        assertEquals("/api/mobile/mfa/status", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"enrollment_id":"enr-1","secret":"JBSWY3DPEHPK3PXP","otpauth_uri":"otpauth://totp/Gothwad:arjun?secret=JBSWY3DPEHPK3PXP"}"""))
        assertEquals("enr-1", client.beginMfaEnrollment("tok-1", "Pass123!").enrollmentId)
        assertEquals("/api/mobile/mfa/enroll", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"recovery_codes":["CODE1","CODE2"]}"""))
        assertEquals(listOf("CODE1", "CODE2"), client.confirmMfaEnrollment("tok-1", "Pass123!", "enr-1", "123456"))
        assertEquals("/api/mobile/mfa/confirm", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"recovery_codes":["CODE3","CODE4"]}"""))
        assertEquals(listOf("CODE3", "CODE4"), client.regenerateMfaRecoveryCodes("tok-1", "Pass123!", "123456"))
        assertEquals("/api/mobile/mfa/recovery-codes", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        client.disableMfa("tok-1", "Pass123!", "123456")
        assertEquals("/api/mobile/mfa/disable", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"export":"gothwad-accounts","version":2}"""))
        val exported = client.exportAccountDataJson("tok-1")
        assertTrue(exported.contains("gothwad-accounts"))
        assertEquals("/api/mobile/export", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"message":"Account deleted"}"""))
        client.deleteAccount("tok-1", "Pass123!")
        assertEquals("/api/mobile/delete-account", server.takeRequest().path)
    }

    @Test
    fun accountDirectoryProviderIssuePeerCodeVerifiesTrustRefreshesAndNeverLeaksTokens() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val trustedCert = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val rogueCert = byteArrayOf(0x09, 0x08, 0x07)
        val trustedSha = sha256Hex(trustedCert)

        installFakePackage(app, KnownGothwadApps.MAIL, 1010, trustedCert)
        installFakePackage(app, "com.evil.untrusted", 1011, rogueCert)

        val trustPolicy = BrokerTrustPolicy.PinnedSigners(
            mapOf(KnownGothwadApps.MAIL to setOf(trustedSha)),
        )
        val config = testConfig(BrokerPolicy.PeerDiscovery(trustPolicy))
        val okHttp = testOkHttp()
        val mobile = MobileAccountClient(config, okHttp)
        val oauth = OAuthClient(config, okHttp)

        // Seed expired access_token so provider MUST refresh silently before calling /mobile/auth/peer-code
        val store = object : TokenStore {
            private var s = StoredSession(
                credentials = listOf(
                    Credential(
                        account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com"),
                        accessToken = "expired-at-never-shared",
                        refreshToken = "rt-never-shared",
                        expiresAt = 0L,
                        addedAt = 10L,
                        lastSeenAt = 10L,
                    ),
                ),
                activeUserId = "u-1",
            )

            override fun read() = s
            override fun write(value: StoredSession) { s = value }
        }
        GothwadAccounts.initWithStoreForTesting(app, config, store, oauth = oauth, mobile = mobile)

        val providerInfo = ProviderInfo().apply {
            authority = KnownGothwadApps.directoryAuthorityFor(app.packageName)
            packageName = app.packageName
            exported = true
        }
        val provider = Robolectric.buildContentProvider(AccountDirectoryProvider::class.java)
            .create(providerInfo)
            .get()

        val requestExtras = Bundle().apply {
            putString(BrokerIpcContract.KEY_USER_ID, "u-1")
            putString(BrokerIpcContract.KEY_TARGET_CLIENT_ID, "gothwad-mail")
            putString(BrokerIpcContract.KEY_REDIRECT_URI, "com.gothwad.mail:/oauth/callback")
            putString(BrokerIpcContract.KEY_SCOPE, "profile email offline_access")
            putString(BrokerIpcContract.KEY_CODE_CHALLENGE, "challenge-abc")
            putString(BrokerIpcContract.KEY_CODE_CHALLENGE_METHOD, "S256")
        }

        // Untrusted caller receives ok=false & error=untrusted_caller
        ShadowBinder.setCallingUid(1011)
        val denied = provider.call(BrokerIpcContract.METHOD_ISSUE_PEER_CODE, null, requestExtras)
        assertFalse(denied.getBoolean(BrokerIpcContract.KEY_OK, true))
        assertEquals("untrusted_caller", denied.getString(BrokerIpcContract.KEY_ERROR))

        // Trusted Mail caller triggers silent refresh (/oauth/token) + peer-code (/mobile/auth/peer-code)
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"access_token":"fresh-at-never-leaked","refresh_token":"fresh-rt-never-leaked","expires_in":3600,"token_type":"Bearer"}""",
            ),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ok":true,"code":"peer-code-999","client_id":"gothwad-mail","redirect_uri":"com.gothwad.mail:/oauth/callback","scope":"profile email offline_access","expires_in":180}""",
            ),
        )

        ShadowBinder.setCallingUid(1010)
        val issued = provider.call(BrokerIpcContract.METHOD_ISSUE_PEER_CODE, null, requestExtras)
        assertTrue(issued.getBoolean(BrokerIpcContract.KEY_OK, false))
        assertEquals("peer-code-999", issued.getString(BrokerIpcContract.KEY_CODE))
        assertEquals("com.gothwad.mail:/oauth/callback", issued.getString(BrokerIpcContract.KEY_REDIRECT_URI))
        assertNull(issued.getString("access_token"))
        assertNull(issued.getString("refresh_token"))

        assertEquals("/api/oauth/token", server.takeRequest().path)
        val peerCodeReq = server.takeRequest()
        assertEquals("/api/mobile/auth/peer-code", peerCodeReq.path)
        assertEquals("Bearer fresh-at-never-leaked", peerCodeReq.getHeader("Authorization"))
    }

    @Test
    fun zeroLocalAccountsAutoSilentlySignsInFromVerifiedPeerWithZeroBrowser() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val trustPolicy = BrokerTrustPolicy.PinnedSigners(
            mapOf(KnownGothwadApps.MAIL to setOf("a".repeat(64))),
        )
        val config = testConfig(BrokerPolicy.PeerDiscovery(trustPolicy))
        val okHttp = testOkHttp()
        val oauth = OAuthClient(config, okHttp)
        val mobile = MobileAccountClient(config, okHttp)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"access_token":"sso-at","refresh_token":"sso-rt","expires_in":3600,"token_type":"Bearer"}""",
            ),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"sub":"u-1","preferred_username":"arjun","name":"Arjun Sharma","email":"arjun@bansify.com"}""",
            ),
        )

        val emptyStore = object : TokenStore {
            private var s = StoredSession()
            override fun read() = s
            override fun write(value: StoredSession) { s = value }
        }

        val fakeTransport = object : PeerDirectoryTransport {
            override fun listInstalledCandidatePackages(policy: BrokerTrustPolicy) = listOf(KnownGothwadApps.MAIL)
            override fun queryPeer(packageName: String, policy: BrokerTrustPolicy) = DirectorySnapshot(
                sourcePackage = KnownGothwadApps.MAIL,
                accounts = listOf(
                    DirectoryAccountRecord(
                        userId = "u-1",
                        username = "arjun",
                        name = "Arjun Sharma",
                        email = "arjun@bansify.com",
                        addedAt = 100L,
                        lastSeenAt = 200L,
                    ),
                ),
            )

            override fun notifyRemoval(packageName: String, userId: String, removedAt: Long, policy: BrokerTrustPolicy) = true

            override fun requestPeerAuthCode(
                packageName: String,
                policy: BrokerTrustPolicy,
                userId: String?,
                username: String?,
                email: String?,
                targetClientId: String,
                redirectUri: String,
                scope: String,
                codeChallenge: String,
                codeChallengeMethod: String,
            ): Result<PeerAuthCodeResult> = Result.success(
                PeerAuthCodeResult(
                    code = "silent-code-123",
                    clientId = targetClientId,
                    redirectUri = redirectUri,
                    scope = scope,
                ),
            )
        }

        GothwadAccounts.initWithStoreForTesting(app, config, emptyStore, oauth = oauth, mobile = mobile, transport = fakeTransport)

        // Wait briefly for initInternal background silent SSO completion
        val deadline = System.currentTimeMillis() + 5_000L
        while (GothwadAccounts.state.value !is AccountState.SignedIn && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }

        val signedIn = GothwadAccounts.state.value as AccountState.SignedIn
        assertEquals("u-1", signedIn.account.userId)
        assertEquals("arjun@bansify.com", signedIn.account.email)
        assertEquals("/api/oauth/token", server.takeRequest().path)
        assertEquals("/api/oauth/userinfo", server.takeRequest().path)
    }

    @Test
    fun siblingFirstPartyClientWithAccountScopesCanManageNativelyAndSupplyCurrentAccessToken() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val okHttp = testOkHttp()
        val fullMailConfig = GothwadConfig(
            clientId = "gothwad-mail",
            redirectUri = "com.gothwad.mail:/oauth/callback",
            authBaseUrl = server.url("/api").toString().trimEnd('/'),
            scopes = setOf(
                Scope.Profile,
                Scope.Email,
                Scope.OfflineAccess,
                Scope.Mail,
                Scope.AccountRead,
                Scope.AccountProfileWrite,
            ),
        )
        val store = object : TokenStore {
            private var s = StoredSession(
                credentials = listOf(
                    Credential(
                        account = GothwadAccount("u-mail", "arjun", "Arjun", email = "arjun@bansify.com"),
                        accessToken = "mail-at-live",
                        refreshToken = "mail-rt-live",
                        expiresAt = System.currentTimeMillis() + 3_600_000L,
                    ),
                ),
                activeUserId = "u-mail",
            )
            override fun read() = s
            override fun write(value: StoredSession) { s = value }
        }
        GothwadAccounts.initWithStoreForTesting(
            app,
            fullMailConfig,
            store,
            oauth = OAuthClient(fullMailConfig, okHttp),
            mobile = MobileAccountClient(fullMailConfig, okHttp),
        )

        assertTrue(GothwadAccounts.canManageAccountNatively())
        assertTrue(GothwadAccounts.canWriteProfileNatively())
        assertEquals("gothwad-mail", GothwadAccounts.currentClientId())
        assertEquals("mail-at-live", GothwadAccounts.currentAccessToken())
        assertFalse(GothwadAccounts.isAccountsAppInstalled())

        installFakePackage(app, KnownGothwadApps.ACCOUNTS, 1020, byteArrayOf(0x05, 0x06))
        assertTrue(GothwadAccounts.isAccountsAppInstalled())

        // Without Scope.AccountRead, native management remains cleanly disabled.
        GothwadAccounts.resetForTesting()
        val mailOnlyConfig = fullMailConfig.copy(
            scopes = setOf(Scope.Profile, Scope.Email, Scope.OfflineAccess, Scope.Mail),
        )
        GothwadAccounts.initWithStoreForTesting(
            app,
            mailOnlyConfig,
            store,
            oauth = OAuthClient(mailOnlyConfig, okHttp),
            mobile = MobileAccountClient(mailOnlyConfig, okHttp),
        )
        assertFalse(GothwadAccounts.canManageAccountNatively())
        assertFalse(GothwadAccounts.canWriteProfileNatively())
    }

    @Test
    fun deleteAccountBindsExpectedUserReportsCleanupFailureAndPersistsBrowserCompletion() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val okHttp = testOkHttp()
        val config = testConfig()

        // 1. Persisted browser completion marker survives reload & non-refreshable expired token invalidates cleanly.
        val reloadStore = object : TokenStore {
            private var s = StoredSession(
                credentials = listOf(
                    Credential(
                        account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com"),
                        accessToken = "at-no-refresh",
                        refreshToken = null,
                        expiresAt = System.currentTimeMillis() + 30_000L,
                    ),
                ),
                activeUserId = "u-1",
                lastBrowserCompletionEventId = "ev-persisted-1",
                lastBrowserCompletionUserId = "u-1",
            )
            override fun read() = s
            override fun write(value: StoredSession) { s = value }
        }
        GothwadAccounts.initWithStoreForTesting(
            app,
            config,
            reloadStore,
            oauth = OAuthClient(config, okHttp),
            mobile = MobileAccountClient(config, okHttp),
        )
        // Unexpired credential without refresh token still works inside the 60s refresh window.
        assertEquals("at-no-refresh", GothwadAccounts.currentAccessToken())
        assertEquals(
            BrowserAuthCompletion("ev-persisted-1", "u-1"),
            GothwadAccounts.browserAuthCompletion.value,
        )

        // Expired credential without refresh token is removed cleanly and transitions to SignedOut.
        GothwadAccounts.resetForTesting()
        val expiredNoRefreshStore = object : TokenStore {
            private var s = StoredSession(
                credentials = listOf(
                    Credential(
                        account = GothwadAccount("u-exp", "expired", "Expired"),
                        accessToken = "at-expired",
                        refreshToken = null,
                        expiresAt = 0L,
                    ),
                ),
                activeUserId = "u-exp",
            )
            override fun read() = s
            override fun write(value: StoredSession) { s = value }
        }
        GothwadAccounts.initWithStoreForTesting(
            app,
            config,
            expiredNoRefreshStore,
            oauth = OAuthClient(config, okHttp),
            mobile = MobileAccountClient(config, okHttp),
        )
        assertNull(GothwadAccounts.currentAccessToken())
        assertEquals(AccountState.SignedOut, GothwadAccounts.state.value)

        // 2. deleteAccount refuses mismatched expectedUserId without touching server or setting SessionExpired,
        // and surfaces local TokenStore write failure after server delete succeeds.
        GothwadAccounts.resetForTesting()
        var failWrites = false
        val guardedStore = object : TokenStore {
            private var s = StoredSession(
                credentials = listOf(
                    Credential(
                        account = GothwadAccount("u-2", "bella", "Bella"),
                        accessToken = "at-bella",
                        refreshToken = "rt-bella",
                        expiresAt = System.currentTimeMillis() + 3_600_000L,
                    ),
                ),
                activeUserId = "u-2",
            )
            override fun read() = s
            override fun write(value: StoredSession) {
                if (failWrites) throw IOException("keystore write failed")
                s = value
            }
        }
        GothwadAccounts.initWithStoreForTesting(
            app,
            config,
            guardedStore,
            oauth = OAuthClient(config, okHttp),
            mobile = MobileAccountClient(config, okHttp),
        )
        val mismatchRes = GothwadAccounts.deleteAccount("Pass123!", expectedUserId = "u-1")
        assertTrue(mismatchRes.isFailure)
        assertEquals(0, server.requestCount)
        assertNull(GothwadAccounts.error.value)

        failWrites = true
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true,"message":"deleted"}"""))
        val cleanupFailRes = GothwadAccounts.deleteAccount("Pass123!", expectedUserId = "u-2")
        assertTrue(cleanupFailRes.isFailure)
        assertTrue(cleanupFailRes.exceptionOrNull()?.message?.contains("local device cleanup failed") == true)
        assertEquals(1, server.requestCount)
        assertTrue(GothwadAccounts.state.value is AccountState.SignedIn)
    }

    private fun installFakePackage(
        context: Context,
        packageName: String,
        uid: Int,
        certBytes: ByteArray,
    ) {
        val shadowPm = shadowOf(context.packageManager)
        val pkgInfo = PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                this.uid = uid
                flags = ApplicationInfo.FLAG_INSTALLED
            }
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(certBytes))
        }
        shadowPm.installPackage(pkgInfo)
        shadowPm.setPackagesForUid(uid, packageName)
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
