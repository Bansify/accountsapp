package com.gothwad.accounts.core

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import com.gothwad.accounts.core.broker.AndroidPeerDirectoryTransport
import com.gothwad.accounts.core.broker.DirectorySnapshot
import com.gothwad.accounts.core.broker.KnownGothwadApps
import com.gothwad.accounts.core.broker.PeerBrokerClient
import com.gothwad.accounts.core.broker.PeerDirectoryTransport
import java.io.IOException
import java.security.PublicKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor

object GothwadAccounts {
    private lateinit var config: GothwadConfig
    private lateinit var session: SessionManager
    private lateinit var mobileClient: MobileAccountClient
    private lateinit var peerBroker: PeerBrokerClient
    private var appContext: Application? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ready: kotlinx.coroutines.Deferred<Unit>? = null
    @Volatile
    private var locallySignedOutInProcess = false

    private val mutableError = MutableStateFlow<AuthFailure?>(null)
    val error: StateFlow<AuthFailure?> = mutableError.asStateFlow()

    private val mutableDiscoveredAccounts = MutableStateFlow<List<DiscoveredAccount>>(emptyList())
    val discoveredAccounts: StateFlow<List<DiscoveredAccount>> = mutableDiscoveredAccounts.asStateFlow()

    private val mutableIsRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = mutableIsRefreshing.asStateFlow()

    private val mutableLastDisconnectOutcome = MutableStateFlow<DisconnectOutcome?>(null)
    val lastDisconnectOutcome: StateFlow<DisconnectOutcome?> = mutableLastDisconnectOutcome.asStateFlow()

    private val mutableBrowserAuthCompletion = MutableStateFlow<BrowserAuthCompletion?>(null)
    val browserAuthCompletion: StateFlow<BrowserAuthCompletion?> = mutableBrowserAuthCompletion.asStateFlow()

    val state: StateFlow<AccountState> get() = session.state

    @Synchronized
    fun init(application: Application, config: GothwadConfig) {
        initInternal(application, config, EncryptedTokenStore(application, config))
    }

    @Synchronized
    internal fun initWithStoreForTesting(
        application: Application,
        config: GothwadConfig,
        store: TokenStore,
        oauth: OAuthService = OAuthClient(config),
        mobile: MobileAccountClient = MobileAccountClient(config),
        transport: PeerDirectoryTransport = AndroidPeerDirectoryTransport(application),
    ) {
        ready = null
        locallySignedOutInProcess = false
        initInternal(application, config, store, oauth, mobile, transport)
    }

    @Synchronized
    internal fun resetForTesting() {
        ready = null
        appContext = null
        locallySignedOutInProcess = false
        mutableError.value = null
        mutableDiscoveredAccounts.value = emptyList()
        mutableIsRefreshing.value = false
        mutableLastDisconnectOutcome.value = null
        mutableBrowserAuthCompletion.value = null
    }

    private fun initInternal(
        application: Application,
        config: GothwadConfig,
        store: TokenStore,
        oauth: OAuthService = OAuthClient(config),
        mobile: MobileAccountClient = MobileAccountClient(config),
        transport: PeerDirectoryTransport = AndroidPeerDirectoryTransport(application),
    ) {
        if (ready != null) {
            require(this.config == config) { "GothwadAccounts.init called twice with different GothwadConfig" }
            return
        }
        this.appContext = application
        this.config = config
        this.mobileClient = mobile
        this.session = SessionManager(
            store = store,
            oauth = oauth,
            packageName = application.packageName,
        )
        this.peerBroker = PeerBrokerClient(
            localPackage = application.packageName,
            transport = transport,
        )
        ready = scope.async {
            try {
                session.load()
                mutableBrowserAuthCompletion.value = session.browserAuthCompletion.value
                syncDiscoveredAccountsInternal()
                if (
                    session.currentLocalAccounts().isEmpty() &&
                    !locallySignedOutInProcess &&
                    config.brokerPolicy is BrokerPolicy.PeerDiscovery
                ) {
                    val peer = mutableDiscoveredAccounts.value.firstOrNull { !it.isAuthorizedLocally }
                    if (peer != null) {
                        runCatching { signInSilentlyWithPeerInternal(peer) }
                    }
                }
            } catch (error: Exception) {
                mutableError.value = AuthFailure.StorageUnavailable
                throw error
            }
        }
    }

    /**
     * Returns true when this host app requests [Scope.AccountRead] so shared `:ui` screens can
     * manage `/api/mobile/...` Account Centre data directly. When a server or legacy cached token
     * rejects `/api/mobile/...` with `insufficient_scope`, `:ui` degrades automatically to web `/me`.
     */
    fun canManageAccountNatively(): Boolean =
        ::config.isInitialized && Scope.AccountRead in config.scopes

    fun canWriteProfileNatively(): Boolean =
        canManageAccountNatively() && Scope.AccountProfileWrite in config.scopes

    fun currentClientId(): String? = if (::config.isInitialized) config.clientId else null

    /**
     * Returns a valid OAuth bearer token for the currently active account (refreshing silently
     * when near expiration), or `null` when signed out or storage is unavailable.
     */
    suspend fun currentAccessToken(): String? = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            session.accessToken()
        }.getOrNull()
    }

    fun isAccountsAppInstalled(): Boolean {
        val ctx = appContext ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(
                    KnownGothwadApps.ACCOUNTS,
                    PackageManager.PackageInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(KnownGothwadApps.ACCOUNTS, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun manageOnWeb(activity: Activity, section: String? = null) = scope.launch {
        action {
            readyOrThrow()
            val base = Uri.parse(config.authBaseUrl)
            val builder = base.buildUpon().path("/me").clearQuery()
            if (!section.isNullOrBlank()) {
                builder.fragment(section)
            }
            val uri = builder.build()
            withContext(Dispatchers.Main) { CustomTabsIntent.Builder().build().launchUrl(activity, uri) }
        }
    }

    fun openWebPage(activity: Activity, path: String) = scope.launch {
        action {
            readyOrThrow()
            val normalizedPath = if (path.startsWith('/')) path else "/$path"
            val uri = Uri.parse(config.authBaseUrl).buildUpon().path(normalizedPath).clearQuery().build()
            withContext(Dispatchers.Main) { CustomTabsIntent.Builder().build().launchUrl(activity, uri) }
        }
    }

    fun clearError() {
        mutableError.value = null
    }

    fun clearDisconnectOutcome() {
        mutableLastDisconnectOutcome.value = null
    }

    /** Secondary Browser / Chrome Custom Tabs OAuth 2.0 + S256 PKCE sign-in fallback. */
    fun signInWithBrowser(
        activity: Activity,
        chooseAccount: Boolean = false,
        loginHint: String? = null,
    ) = signIn(activity, chooseAccount, loginHint)

    fun signIn(
        activity: Activity,
        chooseAccount: Boolean = false,
        loginHint: String? = null,
    ) = scope.launch {
        action {
            readyOrThrow()
            val pending = session.beginLogin()
            val authBuilder = Uri.parse("${config.authBaseUrl.trimEnd('/')}/oauth/authorize").buildUpon()
                .appendQueryParameter("client_id", config.clientId)
                .appendQueryParameter("redirect_uri", config.redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", config.scopes.map { it.wireValue }.sorted().joinToString(" "))
                .appendQueryParameter("state", pending.state)
                .appendQueryParameter("code_challenge", Pkce.challenge(pending.verifier))
                .appendQueryParameter("code_challenge_method", "S256")
            if (!loginHint.isNullOrBlank()) {
                authBuilder.appendQueryParameter("login_hint", loginHint.trim())
            }
            val authorizeUri = authBuilder.build()
            val destination = if (chooseAccount) {
                Uri.parse(config.authBaseUrl).buildUpon().path("/signin").clearQuery()
                    .appendQueryParameter("choose", "1")
                    .appendQueryParameter("next", authorizeUri.toString())
                    .build()
            } else {
                authorizeUri
            }
            withContext(Dispatchers.Main) { CustomTabsIntent.Builder().build().launchUrl(activity, destination) }
        }
    }

    // -------------------------------------------------------------------------
    // Direct Native Authentication (`/api/mobile/auth/*`)
    // -------------------------------------------------------------------------

    suspend fun checkIdentifier(identifier: String): Result<IdentifierCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            mobileClient.checkIdentifier(identifier)
        }
    }

    suspend fun checkUsername(username: String): Result<UsernameCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            mobileClient.checkUsername(username)
        }
    }

    suspend fun signInNative(identifier: String, password: String): Result<MobileAuthSignInOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                readyOrThrow()
                val outcome = mobileClient.nativeSignIn(identifier, password)
                if (outcome is MobileAuthSignInOutcome.Authenticated) {
                    session.completeNativeAuth(outcome)
                    locallySignedOutInProcess = false
                    mutableError.value = null
                    syncDiscoveredAccountsInternal()
                }
                outcome
            }
        }

    suspend fun verifyNativeMfa(challenge: String, code: String): Result<GothwadAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                readyOrThrow()
                val auth = mobileClient.nativeVerifyMfa(challenge, code)
                val account = session.completeNativeAuth(auth)
                locallySignedOutInProcess = false
                mutableError.value = null
                syncDiscoveredAccountsInternal()
                account
            }
        }

    suspend fun signUpNative(input: NativeSignUpInput): Result<GothwadAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                readyOrThrow()
                val auth = mobileClient.nativeSignUp(input)
                val account = session.completeNativeAuth(auth)
                locallySignedOutInProcess = false
                mutableError.value = null
                syncDiscoveredAccountsInternal()
                account
            }
        }

    suspend fun forgotPasswordNative(username: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            mobileClient.forgotPassword(username)
        }
    }

    suspend fun resetPasswordNative(token: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            mobileClient.resetPassword(token, newPassword)
        }
    }

    // -------------------------------------------------------------------------
    // Zero-Browser Cross-App Silent Peer SSO (`POST /api/mobile/auth/peer-code`)
    // -------------------------------------------------------------------------

    suspend fun signInSilentlyWithPeer(peer: DiscoveredAccount): Result<GothwadAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                readyOrThrow()
                signInSilentlyWithPeerInternal(peer)
            }
        }

    private suspend fun signInSilentlyWithPeerInternal(peer: DiscoveredAccount): GothwadAccount {
        val verifier = Pkce.random()
        val challenge = Pkce.challenge(verifier)
        val scopeString = config.scopes.map { it.wireValue }.sorted().joinToString(" ")
        val peerCode = peerBroker.requestPeerAuthCode(
            peer = peer,
            policy = config.brokerPolicy,
            targetClientId = config.clientId,
            redirectUri = config.redirectUri,
            scope = scopeString,
            codeChallenge = challenge,
            codeChallengeMethod = "S256",
        ).getOrThrow()
        val account = session.completePeerCodeExchange(peerCode.code, verifier)
        locallySignedOutInProcess = false
        mutableError.value = null
        syncDiscoveredAccountsInternal()
        return account
    }

    fun switchTo(userId: String) = scope.launch {
        action {
            readyOrThrow()
            session.switchTo(userId)
            syncDiscoveredAccountsInternal()
        }
    }

    /**
     * Removes credentials from THIS app installation (`revokeOnServer = false` by default).
     * Grants on other Android installations/devices remain active unless [revokeOnServer] is true
     * or [disconnectFromServer] is invoked explicitly.
     */
    fun signOut(userId: String, revokeOnServer: Boolean = false) = scope.launch {
        action {
            readyOrThrow()
            locallySignedOutInProcess = true
            val outcome = session.signOut(userId, revokeOnServer = revokeOnServer)
            if (revokeOnServer) {
                mutableLastDisconnectOutcome.value = outcome
            }
            syncDiscoveredAccountsInternal()
        }
    }

    /**
     * Explicitly disconnects the entire user/client OAuth grant via `POST /oauth/revoke` before
     * removing local credentials. Records exact [DisconnectOutcome] in [lastDisconnectOutcome]
     * so offline local removal is never misreported as server revocation.
     */
    suspend fun disconnectFromServer(userId: String): DisconnectOutcome = withContext(Dispatchers.IO) {
        readyOrThrow()
        locallySignedOutInProcess = true
        val outcome = session.signOut(userId, revokeOnServer = true)
        mutableLastDisconnectOutcome.value = outcome
        syncDiscoveredAccountsInternal()
        outcome
    }

    fun disconnectFromServerAsync(userId: String) = scope.launch {
        action { disconnectFromServer(userId) }
    }

    /**
     * Removes credentials from this app AND notifies mutually verified peer Gothwad apps on this
     * device via tombstone + verified IPC. Server sessions and browser cookies remain independent.
     */
    fun removeFromDevice(userId: String, revokeOnServer: Boolean = false) = scope.launch {
        action {
            readyOrThrow()
            locallySignedOutInProcess = true
            val removedAt = session.removeFromDevice(userId, revokeOnServer = revokeOnServer)
            peerBroker.broadcastDeviceRemoval(userId, removedAt, config.brokerPolicy)
            syncDiscoveredAccountsInternal()
        }
    }

    fun refreshProfile() = scope.launch {
        mutableIsRefreshing.value = true
        try {
            action {
                readyOrThrow()
                session.refreshActiveProfile()
                syncDiscoveredAccountsInternal()
            }
        } finally {
            mutableIsRefreshing.value = false
        }
    }

    fun syncDiscoveredAccounts() = scope.launch {
        action {
            readyOrThrow()
            syncDiscoveredAccountsInternal()
        }
    }

    fun recoverCorruptedStorage() = scope.launch {
        action {
            readyOrThrow()
            session.recoverCorruptedStorage()
            mutableBrowserAuthCompletion.value = null
            syncDiscoveredAccountsInternal()
        }
    }

    // --- Native first-party mobile management APIs (`gothwad-accounts-android` + `account.*` scopes) ---

    suspend fun listWebSessions(): Result<List<WebSessionInfo>> = mobileCall { token ->
        requireAccountReadScope()
        mobileClient.listSessions(token)
    }

    suspend fun revokeOtherWebSessions(): Result<Int> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.revokeOtherSessions(token).also {
            runCatching { session.refreshActiveProfile() }
        }
    }

    suspend fun revokeWebSession(sessionId: String): Result<Int> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.revokeSession(token, sessionId).also {
            runCatching { session.refreshActiveProfile() }
        }
    }

    suspend fun listActivity(): Result<List<AccountActivityEntry>> = mobileCall { token ->
        requireAccountReadScope()
        mobileClient.listActivity(token)
    }

    suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> = mobileCall { token ->
        requireAccountReadScope()
        mobileClient.listAuthorizations(token)
    }

    suspend fun revokeAuthorization(appId: String): Result<Unit> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.revokeAuthorization(token, appId)
        runCatching { session.refreshActiveProfile() }
        Unit
    }

    suspend fun updateBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ): Result<GothwadAccount?> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.patchProfile(token, firstName, lastName, dateOfBirth, gender)
        session.refreshActiveProfile()
    }

    suspend fun uploadAvatar(imageDataOrBase64: String, mime: String = "image/jpeg"): Result<GothwadAccount?> =
        mobileCall { token ->
            requireAccountProfileWriteScope()
            mobileClient.uploadAvatar(token, imageDataOrBase64, mime)
            session.refreshActiveProfile()
        }

    suspend fun removeAvatar(): Result<GothwadAccount?> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.deleteAvatar(token)
        session.refreshActiveProfile()
    }

    suspend fun updatePhoneNumber(
        phoneNumber: String,
        currentPassword: String,
        mfaCode: String? = null,
    ): Result<GothwadAccount?> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.updatePhoneNumber(token, phoneNumber, currentPassword, mfaCode)
        session.refreshActiveProfile()
    }

    suspend fun requestRecoveryEmailChange(
        recoveryEmail: String,
        currentPassword: String,
        mfaCode: String? = null,
    ): Result<String> = mobileCall { token ->
        requireAccountProfileWriteScope()
        mobileClient.requestRecoveryEmailChange(token, recoveryEmail, currentPassword, mfaCode)
    }

    suspend fun verifyRecoveryEmailChange(token: String): Result<GothwadAccount?> = mobileCall { bearer ->
        requireAccountProfileWriteScope()
        mobileClient.verifyRecoveryEmailChange(bearer, token)
        session.refreshActiveProfile()
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        mfaCode: String? = null,
    ): Result<String> = mobileCall { bearer ->
        requireAccountProfileWriteScope()
        val message = mobileClient.changePassword(bearer, currentPassword, newPassword, mfaCode)
        runCatching { session.refreshActiveProfile() }
        message
    }

    suspend fun getMfaStatus(): Result<MfaStatusInfo> = mobileCall { bearer ->
        requireAccountReadScope()
        mobileClient.getMfaStatus(bearer)
    }

    suspend fun beginMfaEnrollment(currentPassword: String): Result<MfaEnrollmentSetup> = mobileCall { bearer ->
        requireAccountProfileWriteScope()
        mobileClient.beginMfaEnrollment(bearer, currentPassword)
    }

    suspend fun confirmMfaEnrollment(
        currentPassword: String,
        enrollmentId: String,
        code: String,
    ): Result<List<String>> = mobileCall { bearer ->
        requireAccountProfileWriteScope()
        val codes = mobileClient.confirmMfaEnrollment(bearer, currentPassword, enrollmentId, code)
        runCatching { session.refreshActiveProfile() }
        codes
    }

    suspend fun regenerateMfaRecoveryCodes(currentPassword: String, code: String): Result<List<String>> =
        mobileCall { bearer ->
            requireAccountProfileWriteScope()
            mobileClient.regenerateMfaRecoveryCodes(bearer, currentPassword, code)
        }

    suspend fun disableMfa(currentPassword: String, code: String): Result<Unit> = mobileCall { bearer ->
        requireAccountProfileWriteScope()
        mobileClient.disableMfa(bearer, currentPassword, code)
        runCatching { session.refreshActiveProfile() }
        Unit
    }

    suspend fun exportAccountDataJson(): Result<String> = mobileCall { bearer ->
        requireAccountReadScope()
        mobileClient.exportAccountDataJson(bearer)
    }

    suspend fun deleteAccount(
        password: String,
        mfaCode: String? = null,
        expectedUserId: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            requireAccountProfileWriteScope()
            val resolution = session.activeAccessTokenForDeletion(expectedUserId)
            val (capturedUserId, bearer) = when (resolution) {
                is SessionManager.DeletionAuthResolution.Ready -> resolution.userId to resolution.bearerToken
                SessionManager.DeletionAuthResolution.AccountChanged ->
                    throw IllegalStateException("Active account changed before deletion; please try again.")
                SessionManager.DeletionAuthResolution.NoActiveSession ->
                    throw OAuthException(rejected = true)
            }
            mobileClient.deleteAccount(bearer, password, mfaCode)
            val removedAt = try {
                session.removeFromDevice(capturedUserId, revokeOnServer = false)
            } catch (_: Exception) {
                try {
                    session.removeFromDevice(capturedUserId, revokeOnServer = false)
                } catch (secondErr: Exception) {
                    throw IllegalStateException(
                        "Account deleted on server, but local device cleanup failed; remove the account locally or restart.",
                        secondErr,
                    )
                }
            }
            runCatching {
                peerBroker.broadcastDeviceRemoval(capturedUserId, removedAt, config.brokerPolicy)
                syncDiscoveredAccountsInternal()
            }
            val anyRemainingLocal = runCatching { session.currentLocalAccounts().isNotEmpty() }.getOrDefault(false)
            if (!anyRemainingLocal) {
                locallySignedOutInProcess = true
            }
            Unit
        }.also { result ->
            val err = result.exceptionOrNull()
            if (err is OAuthException && err.rejected) {
                mutableError.value = AuthFailure.SessionExpired
            }
        }
    }

    suspend fun fetchTrustedApps(
        pinnedPublicKey: PublicKey? = null,
        expectedKeyId: String? = null,
    ): Result<TrustedAppsManifest> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            mobileClient.fetchTrustedApps(pinnedPublicKey, expectedKeyId)
        }
    }

    fun installedVerifiedPeers(): List<String> {
        if (ready == null || !::peerBroker.isInitialized) return emptyList()
        return runCatching { peerBroker.installedVerifiedPeers(config.brokerPolicy) }.getOrDefault(emptyList())
    }

    internal fun currentBrokerPolicy(): BrokerPolicy =
        if (::config.isInitialized) config.brokerPolicy else BrokerPolicy.Disabled

    internal fun directorySnapshotBlocking(): DirectorySnapshot = runBlocking(Dispatchers.IO) {
        withTimeout(2_000L) {
            readyOrThrow()
            session.directorySnapshot()
        }
    }

    internal fun applyPeerDeviceRemovalBlocking(userId: String, removedAt: Long) = runBlocking(Dispatchers.IO) {
        withTimeout(2_000L) {
            readyOrThrow()
            session.applyPeerDeviceRemoval(userId, removedAt)
            syncDiscoveredAccountsInternal()
        }
    }

    internal fun issuePeerCodeForCallerBlocking(
        userId: String?,
        username: String?,
        email: String?,
        targetClientId: String,
        redirectUri: String,
        scope: String,
        codeChallenge: String,
        codeChallengeMethod: String,
    ): Result<PeerAuthCodeResult> = runBlocking(Dispatchers.IO) {
        runCatching {
            withTimeout(15_000L) {
                readyOrThrow()
                val (_, accessToken) = session.accessTokenForUser(userId, username, email)
                    ?: throw IOException("account_not_signed_in")
                mobileClient.issuePeerAuthCode(
                    accessToken = accessToken,
                    targetClientId = targetClientId,
                    redirectUri = redirectUri,
                    scope = scope,
                    codeChallenge = codeChallenge,
                    codeChallengeMethod = codeChallengeMethod,
                )
            }
        }
    }

    internal fun callback(uri: String) = scope.launch {
        mutableIsRefreshing.value = true
        try {
            action {
                readyOrThrow()
                session.completeLogin(uri, config.redirectUri)
                locallySignedOutInProcess = false
                mutableBrowserAuthCompletion.value = session.browserAuthCompletion.value
                syncDiscoveredAccountsInternal()
            }
        } finally {
            mutableIsRefreshing.value = false
        }
    }

    /** Install as a NETWORK interceptor so every redirect hop is host-checked. */
    fun authInterceptor(allowedHosts: Set<String>): Interceptor {
        require(allowedHosts.isNotEmpty())
        require(allowedHosts.all { it == it.lowercase() && '/' !in it && ':' !in it && it.isNotBlank() })
        return Interceptor { chain ->
            check(Looper.myLooper() != Looper.getMainLooper()) {
                "Authenticated network requests must run off the main thread"
            }
            require(chain.connection() != null) { "Use addNetworkInterceptor, not addInterceptor" }
            val request = chain.request()
            val builder = request.newBuilder().removeHeader("Authorization")
            if (request.url.isHttps && request.url.host in allowedHosts && request.url.port == 443) {
                val token = runBlocking(Dispatchers.IO) { readyOrThrow(); session.accessToken() }
                if (token != null) builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
    }

    private suspend fun <T> mobileCall(block: suspend (String) -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            readyOrThrow()
            val token = session.accessToken() ?: throw OAuthException(rejected = true)
            block(token)
        }.also { result ->
            val err = result.exceptionOrNull()
            if (err is OAuthException && err.rejected) {
                mutableError.value = AuthFailure.SessionExpired
            }
        }
    }

    private fun requireAccountReadScope() {
        check(Scope.AccountRead in config.scopes) { "Requires Scope.AccountRead" }
    }

    private fun requireAccountProfileWriteScope() {
        check(Scope.AccountProfileWrite in config.scopes) { "Requires Scope.AccountProfileWrite" }
    }

    private suspend fun syncDiscoveredAccountsInternal() {
        val localSnap = session.directorySnapshot()
        val localAccounts = session.currentLocalAccounts()
        val discovered = runCatching {
            peerBroker.discoverAccounts(localSnap, localAccounts, config.brokerPolicy)
        }.getOrDefault(emptyList())
        mutableDiscoveredAccounts.value = discovered
    }

    private suspend fun readyOrThrow() {
        checkNotNull(ready) { "Call GothwadAccounts.init from Application.onCreate" }.await()
    }

    private suspend fun action(block: suspend () -> Unit) {
        mutableError.value = null
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mutableError.value = when (error) {
                is ActivityNotFoundException -> AuthFailure.BrowserUnavailable
                is LoginCancelledException -> AuthFailure.LoginCancelled
                is IllegalArgumentException -> AuthFailure.InvalidCallback
                is OAuthException -> if (error.rejected) AuthFailure.SessionExpired else AuthFailure.Unavailable
                is IOException -> AuthFailure.Unavailable
                else -> AuthFailure.StorageUnavailable
            }
        }
    }
}
