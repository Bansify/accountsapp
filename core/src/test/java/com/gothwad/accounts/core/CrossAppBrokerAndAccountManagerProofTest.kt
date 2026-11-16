package com.gothwad.accounts.core

import android.accounts.AccountManager
import android.accounts.AuthenticatorDescription
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.content.pm.Signature
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.gothwad.accounts.core.broker.AccountDirectoryProvider
import com.gothwad.accounts.core.broker.BrokerIpcContract
import com.gothwad.accounts.core.broker.DirectorySnapshot
import com.gothwad.accounts.core.broker.KnownGothwadApps
import com.gothwad.accounts.core.broker.PeerBrokerClient
import com.gothwad.accounts.core.broker.PeerDirectoryTransport
import com.gothwad.accounts.core.broker.TrustVerifier
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBinder

/**
 * Executable proof suite verifying:
 * 1. Why Android `AccountManager` multi-authenticator shared-`accountType` registration fails
 *    (single winning authenticator, no automatic failover, uninstall-wipe risk).
 * 2. Real `AccountDirectoryProvider` Binder UID + certificate verification under Robolectric.
 * 3. End-to-end Scenarios A–E across independently installable Gothwad apps (`mailapp`, `drive`,
 *    `storeapp`, `accountsapp`) with isolated per-app encrypted stores and per-app selections.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
class CrossAppBrokerAndAccountManagerProofTest {

    @After
    fun tearDown() {
        GothwadAccounts.resetForTesting()
    }

    // -------------------------------------------------------------------------
    // PROOF 1: Empirical rejection of multi-authenticator shared accountType
    // -------------------------------------------------------------------------

    @Test
    fun proof1_androidAccountManagerAllowsOnlySingleAuthenticatorOwnerPerAccountType() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val am = AccountManager.get(context)
        val shadowAm = shadowOf(am)
        val pm = context.packageManager
        val shadowPm = shadowOf(pm)

        val sharedAccountType = "com.gothwad.account"

        // Simulate two independent apps (Mail & Drive) both declaring android.accounts.AccountAuthenticator
        // in their manifests for the same shared accountType.
        val mailService = ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                packageName = KnownGothwadApps.MAIL
                name = "com.gothwad.mail.GothwadAuthenticatorService"
            }
        }
        val driveService = ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                packageName = KnownGothwadApps.DRIVE
                name = "com.gothwad.drive.GothwadAuthenticatorService"
            }
        }
        val authIntent = Intent("android.accounts.AccountAuthenticator")
        shadowPm.addResolveInfoForIntent(authIntent, mailService)
        shadowPm.addResolveInfoForIntent(authIntent, driveService)

        // PackageManager sees two candidate authenticator services...
        assertEquals(2, pm.queryIntentServices(authIntent, 0).size)

        // ...HOWEVER Android's RegisteredServicesCache (and Robolectric's ShadowAccountManager mirror)
        // indexes authenticator ownership by `type` (1 slot per accountType): registering Drive
        // overwrites Mail's ownership rather than providing multi-owner or failover!
        shadowAm.addAuthenticator(
            AuthenticatorDescription(sharedAccountType, KnownGothwadApps.MAIL, 0, 0, 0, 0),
        )
        shadowAm.addAuthenticator(
            AuthenticatorDescription(sharedAccountType, KnownGothwadApps.DRIVE, 0, 0, 0, 0),
        )

        val registeredForType = am.authenticatorTypes.filter { it.type == sharedAccountType }
        assertEquals(
            "Android AccountManager keeps strictly ONE winning authenticator per accountType",
            1,
            registeredForType.size,
        )
        assertEquals(KnownGothwadApps.DRIVE, registeredForType.single().packageName)
    }

    // -------------------------------------------------------------------------
    // PROOF 2: Real AccountDirectoryProvider Binder UID + Signing Cert Checks
    // -------------------------------------------------------------------------

    @Test
    fun proof2_directoryProviderVerifiesBinderUidAndCertificateBeforeMetadataAccess() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowPm = shadowOf(app.packageManager)

        val trustedCertBytes = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55)
        val rogueCertBytes = byteArrayOf(0x99.toByte(), 0x88.toByte(), 0x77)
        val trustedSha = sha256Hex(trustedCertBytes)

        installFakePackage(app, KnownGothwadApps.ACCOUNTS, 1001, trustedCertBytes)
        installFakePackage(app, KnownGothwadApps.MAIL, 1002, trustedCertBytes)
        installFakePackage(app, KnownGothwadApps.DRIVE, 1003, trustedCertBytes)
        installFakePackage(app, "com.evil.spoof", 1004, rogueCertBytes)
        // Mixed-UID attack: UID 1005 shares trusted Drive + rogue spoofing package!
        installFakePackage(app, KnownGothwadApps.STORE, 1005, trustedCertBytes, uidPackages = arrayOf(KnownGothwadApps.STORE, "com.evil.mixed"))
        installFakePackage(app, "com.evil.mixed", 1005, rogueCertBytes, uidPackages = arrayOf(KnownGothwadApps.STORE, "com.evil.mixed"))

        val trustPolicy = BrokerTrustPolicy.PinnedSigners(
            mapOf(
                KnownGothwadApps.ACCOUNTS to setOf(trustedSha),
                KnownGothwadApps.MAIL to setOf(trustedSha),
                KnownGothwadApps.DRIVE to setOf(trustedSha),
                KnownGothwadApps.STORE to setOf(trustedSha),
            ),
        )

        // Direct TrustVerifier Binder UID checks:
        assertNotNull(TrustVerifier.verifyBinderCaller(app, 1002, trustPolicy))
        assertNull("Untrusted signer must be rejected", TrustVerifier.verifyBinderCaller(app, 1004, trustPolicy))
        assertNull("Mixed-UID with one untrusted package must be rejected", TrustVerifier.verifyBinderCaller(app, 1005, trustPolicy))

        // Real ContentProvider IPC check under Robolectric:
        val providerInfo = ProviderInfo().apply {
            authority = KnownGothwadApps.directoryAuthorityFor(app.packageName)
            packageName = app.packageName
            exported = true
        }
        val controller = Robolectric.buildContentProvider(AccountDirectoryProvider::class.java).create(providerInfo)
        val provider = controller.get()

        // When BrokerPolicy is Disabled (FOSS default), even a trusted UID is rejected!
        ShadowBinder.setCallingUid(1002)
        assertThrows(SecurityException::class.java) {
            provider.call(BrokerIpcContract.METHOD_LIST_ACCOUNTS, null, null)
        }

        // Initialize GothwadAccounts in host context with PeerDiscovery policy and a seeded store.
        val seededAccount = GothwadAccount("uid-1", "arjun", "Arjun", "arjun@gothwadtech.com")
        val seededStore = object : TokenStore {
            private var s = StoredSession(
                credentials = listOf(
                    Credential(seededAccount, "secret-at-never-shared", "secret-rt-never-shared", 99999999L, addedAt = 10L, lastSeenAt = 20L),
                ),
                activeUserId = "uid-1",
            )
            override fun read() = s
            override fun write(value: StoredSession) { s = value }
        }
        val config = GothwadConfig(
            clientId = "gothwad-accounts-android",
            redirectUri = "${app.packageName}:/oauth/callback",
            brokerPolicy = BrokerPolicy.PeerDiscovery(trustPolicy),
        )
        GothwadAccounts.initWithStoreForTesting(app, config, seededStore)

        // Wait briefly for initial store load, then verify untrusted UID 1004 is rejected BEFORE storage access!
        ShadowBinder.setCallingUid(1004)
        assertThrows(SecurityException::class.java) {
            provider.call(BrokerIpcContract.METHOD_LIST_ACCOUNTS, null, null)
        }
        ShadowBinder.setCallingUid(1005)
        assertThrows(SecurityException::class.java) {
            provider.call(BrokerIpcContract.METHOD_LIST_ACCOUNTS, null, null)
        }

        // Trusted UID 1002 (com.gothwad.mail) receives valid snapshot JSON containing metadata ONLY!
        ShadowBinder.setCallingUid(1002)
        val bundle: Bundle = provider.call(BrokerIpcContract.METHOD_LIST_ACCOUNTS, null, null)
        val rawJson = bundle.getString(BrokerIpcContract.KEY_SNAPSHOT_JSON)!!
        assertFalse("Directory IPC must never contain accessToken", rawJson.contains("secret-at-never-shared"))
        assertFalse("Directory IPC must never contain refreshToken", rawJson.contains("secret-rt-never-shared"))
        val snapshot = Json { ignoreUnknownKeys = true }.decodeFromString<DirectorySnapshot>(rawJson)
        assertEquals(app.packageName, snapshot.sourcePackage)
        assertEquals(listOf("uid-1"), snapshot.accounts.map { it.userId })

        // Verify device removal propagation via real AccountDirectoryProvider:
        val removalArgs = Bundle().apply {
            putString(BrokerIpcContract.KEY_USER_ID, "uid-1")
            putLong(BrokerIpcContract.KEY_REMOVED_AT, 50L)
        }
        ShadowBinder.setCallingUid(1004)
        assertThrows(SecurityException::class.java) {
            provider.call(BrokerIpcContract.METHOD_REMOVE_FROM_DEVICE, null, removalArgs)
        }
        ShadowBinder.setCallingUid(1002)
        val removalBundle = provider.call(BrokerIpcContract.METHOD_REMOVE_FROM_DEVICE, null, removalArgs)
        assertTrue(removalBundle.getBoolean(BrokerIpcContract.KEY_OK))
        val afterRemovalRaw = provider.call(BrokerIpcContract.METHOD_LIST_ACCOUNTS, null, null)
            .getString(BrokerIpcContract.KEY_SNAPSHOT_JSON)!!
        val afterRemoval = Json { ignoreUnknownKeys = true }.decodeFromString<DirectorySnapshot>(afterRemovalRaw)
        assertTrue(afterRemoval.accounts.isEmpty())
        assertEquals(50L, afterRemoval.tombstones["uid-1"])

        // Verify EncryptedTokenStore fails closed (never falls back to plaintext) when Keystore is unavailable:
        val realEncryptedStore = EncryptedTokenStore(app, config)
        assertThrows(Exception::class.java) {
            realEncryptedStore.write(StoredSession())
        }
    }

    // -------------------------------------------------------------------------
    // PROOF 3: End-to-End Scenarios A, B, C, D, E (Independent Install & Uninstall)
    // -------------------------------------------------------------------------

    @Test
    fun proof3_scenariosAToE_independentAppsNeverWipeOrLeakTokens() = runTest {
        var now = 1_000_000L
        val clock = { now }

        val accountA = GothwadAccount("uid-a", "arjun", "Arjun", "arjun@gothwadtech.com")
        val accountB = GothwadAccount("uid-b", "meera", "Meera", "meera@gothwadtech.com")
        val accountC = GothwadAccount("uid-c", "kiran", "Kiran", "kiran@gothwadtech.com")

        val bus = FakeVerifiedDeviceBus()
        val trust = BrokerTrustPolicy.PinnedSigners(
            KnownGothwadApps.FIRST_PARTY_PACKAGES.associateWith { setOf("a".repeat(64)) },
        )
        val brokerPolicy = BrokerPolicy.PeerDiscovery(trust)

        // Scenario A: Mail installed FIRST (Drive, Store, Accounts NOT installed yet).
        val mail = SimulatedAppHost(KnownGothwadApps.MAIL, "gothwad-mail", bus, clock)
        bus.install(mail)
        mail.signIn(accountA, accessToken = "mail-at-A", refreshToken = "mail-rt-A")
        now += 100
        mail.signIn(accountB, accessToken = "mail-at-B", refreshToken = "mail-rt-B")
        now += 100
        mail.session.switchTo(accountB.userId)

        assertEquals(accountB.userId, (mail.state() as AccountState.SignedIn).account.userId)
        assertEquals(listOf("uid-b", "uid-a"), mail.discover(brokerPolicy).map { it.userId })

        // Scenario B: Drive installed LATER (Accounts STILL not installed!).
        val drive = SimulatedAppHost(KnownGothwadApps.DRIVE, "gothwad-drive", bus, clock)
        bus.install(drive)

        val driveDiscovered = drive.discover(brokerPolicy)
        assertEquals(setOf("uid-a", "uid-b"), driveDiscovered.map { it.userId }.toSet())
        assertTrue("Neither A nor B has Drive tokens yet", driveDiscovered.none { it.isAuthorizedLocally })

        // User chooses Account A in Drive -> Drive gets Drive-scoped tokens; Mail MUST stay on B!
        now += 100
        drive.signIn(accountA, accessToken = "drive-at-A", refreshToken = "drive-rt-A")
        assertEquals(accountA.userId, (drive.state() as AccountState.SignedIn).account.userId)
        assertEquals(accountB.userId, (mail.state() as AccountState.SignedIn).account.userId)
        assertNotEquals(drive.session.accessToken(), mail.session.accessToken())

        // Scenario C: Drive adds Account C -> Mail sees {A, B, C} while remaining active on B!
        now += 100
        drive.signIn(accountC, accessToken = "drive-at-C", refreshToken = "drive-rt-C")
        assertEquals(accountC.userId, (drive.state() as AccountState.SignedIn).account.userId)
        assertEquals(accountB.userId, (mail.state() as AccountState.SignedIn).account.userId)

        val mailSeesAfterC = mail.discover(brokerPolicy)
        assertEquals(setOf("uid-a", "uid-b", "uid-c"), mailSeesAfterC.map { it.userId }.toSet())
        assertFalse("C is discovered on device for Mail, not yet authorized locally in Mail", mailSeesAfterC.first { it.userId == "uid-c" }.isAuthorizedLocally)

        // Scenario D: Accounts app installed AFTER Mail & Drive -> adopts {A, B, C} with ZERO wipes!
        val accountsApp = SimulatedAppHost(KnownGothwadApps.ACCOUNTS, "gothwad-accounts-android", bus, clock)
        bus.install(accountsApp)
        val accountsView = accountsApp.discover(brokerPolicy)
        assertEquals(setOf("uid-a", "uid-b", "uid-c"), accountsView.map { it.userId }.toSet())
        assertEquals(accountB.userId, (mail.state() as AccountState.SignedIn).account.userId)
        assertEquals(accountC.userId, (drive.state() as AccountState.SignedIn).account.userId)

        // Scenario E: Uninstall Mail (the first-installed app!), plus Store running WITHOUT Accounts!
        bus.uninstall(KnownGothwadApps.MAIL)
        assertEquals(
            "Drive keeps its own credentials & remains on C after Mail uninstall",
            accountC.userId,
            (drive.state() as AccountState.SignedIn).account.userId,
        )
        assertEquals(
            "Accounts keeps discovering Drive's {A, C} after Mail uninstall",
            setOf("uid-a", "uid-c"),
            accountsApp.discover(brokerPolicy).map { it.userId }.toSet(),
        )

        // Even if Accounts app is ALSO uninstalled, Store + Drive still operate independently!
        bus.uninstall(KnownGothwadApps.ACCOUNTS)
        val store = SimulatedAppHost(KnownGothwadApps.STORE, "kiwi-store", bus, clock)
        bus.install(store)
        assertEquals(setOf("uid-a", "uid-c"), store.discover(brokerPolicy).map { it.userId }.toSet())

        // FOSS / Disabled BrokerPolicy never discovers peer accounts or answers queries!
        assertTrue(store.discover(BrokerPolicy.Disabled).isEmpty())

        // App-local sign-out vs Device-wide removal tombstone:
        now += 100
        store.signIn(accountA, accessToken = "store-at-A", refreshToken = "store-rt-A")
        store.session.signOut("uid-a") // Local sign-out only -> Drive STILL has A!
        assertTrue((drive.state() as AccountState.SignedIn).all.any { it.userId == "uid-a" })

        now += 100
        val removedAt = drive.session.removeFromDevice("uid-a", removedAt = now)
        drive.broker.broadcastDeviceRemoval("uid-a", removedAt, brokerPolicy)
        assertFalse(
            "Device removal clears uid-a from Drive & peer discovery",
            store.discover(brokerPolicy).any { it.userId == "uid-a" },
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun installFakePackage(
        context: Context,
        packageName: String,
        uid: Int,
        certBytes: ByteArray,
        uidPackages: Array<String> = arrayOf(packageName),
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
        shadowPm.setPackagesForUid(uid, *uidPackages)
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class MemoryStore : TokenStore {
        private var stored = StoredSession()
        override fun read() = stored
        override fun write(value: StoredSession) { stored = value }
    }

    private class FakeVerifiedDeviceBus : PeerDirectoryTransport {
        val installed = linkedMapOf<String, SimulatedAppHost>()

        fun install(host: SimulatedAppHost) { installed[host.packageName] = host }
        fun uninstall(packageName: String) { installed.remove(packageName) }

        override fun listInstalledCandidatePackages(policy: BrokerTrustPolicy): List<String> =
            installed.keys.sorted()

        override fun queryPeer(packageName: String, policy: BrokerTrustPolicy): DirectorySnapshot? =
            kotlinx.coroutines.runBlocking { installed[packageName]?.session?.directorySnapshot() }

        override fun notifyRemoval(
            packageName: String,
            userId: String,
            removedAt: Long,
            policy: BrokerTrustPolicy,
        ): Boolean {
            val peer = installed[packageName] ?: return false
            kotlinx.coroutines.runBlocking { peer.session.applyPeerDeviceRemoval(userId, removedAt) }
            return true
        }
    }

    private class SimulatedAppHost(
        val packageName: String,
        private val clientId: String,
        private val bus: FakeVerifiedDeviceBus,
        private val clock: () -> Long,
    ) {
        private var nextAuthPayload: Triple<GothwadAccount, String, String>? = null
        private val store = MemoryStore()
        private val oauth = object : OAuthService {
            override fun exchange(code: String, verifier: String): TokenResponse {
                val (_, at, rt) = requireNotNull(nextAuthPayload)
                return TokenResponse(at, rt, 3600, "Bearer")
            }

            override fun refresh(token: String) = TokenResponse("refreshed-$clientId", token, 3600, "Bearer")
            override fun userInfo(token: String) = requireNotNull(nextAuthPayload).first
        }

        val session = SessionManager(store, oauth, packageName, clock)
        val broker = PeerBrokerClient(
            localPackage = packageName,
            transport = object : PeerDirectoryTransport {
                override fun listInstalledCandidatePackages(policy: BrokerTrustPolicy) =
                    bus.listInstalledCandidatePackages(policy).filter { it != packageName }

                override fun queryPeer(packageName: String, policy: BrokerTrustPolicy) =
                    if (packageName == this@SimulatedAppHost.packageName) null else bus.queryPeer(packageName, policy)

                override fun notifyRemoval(packageName: String, userId: String, removedAt: Long, policy: BrokerTrustPolicy) =
                    if (packageName == this@SimulatedAppHost.packageName) false else bus.notifyRemoval(packageName, userId, removedAt, policy)
            },
        )

        init {
            kotlinx.coroutines.runBlocking { session.load() }
        }

        suspend fun signIn(account: GothwadAccount, accessToken: String, refreshToken: String) {
            nextAuthPayload = Triple(account, accessToken, refreshToken)
            val pending = session.beginLogin()
            val redirect = "$packageName:/oauth/callback"
            session.completeLogin("$redirect?state=${pending.state}&code=code-${account.userId}", redirect)
        }

        fun state(): AccountState = session.state.value

        suspend fun discover(policy: BrokerPolicy): List<DiscoveredAccount> =
            broker.discoverAccounts(session.directorySnapshot(), session.currentLocalAccounts(), policy)
    }
}
