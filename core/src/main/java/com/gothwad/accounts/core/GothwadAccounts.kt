package com.gothwad.accounts.core

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import com.gothwad.accounts.core.broker.AndroidPeerDirectoryTransport
import com.gothwad.accounts.core.broker.DirectorySnapshot
import com.gothwad.accounts.core.broker.PeerBrokerClient
import java.io.IOException
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
    private lateinit var peerBroker: PeerBrokerClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ready: kotlinx.coroutines.Deferred<Unit>? = null

    private val mutableError = MutableStateFlow<AuthFailure?>(null)
    val error: StateFlow<AuthFailure?> = mutableError.asStateFlow()

    private val mutableDiscoveredAccounts = MutableStateFlow<List<DiscoveredAccount>>(emptyList())
    val discoveredAccounts: StateFlow<List<DiscoveredAccount>> = mutableDiscoveredAccounts.asStateFlow()

    private val mutableIsRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = mutableIsRefreshing.asStateFlow()

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
    ) {
        ready = null
        initInternal(application, config, store, oauth)
    }

    @Synchronized
    internal fun resetForTesting() {
        ready = null
        mutableError.value = null
        mutableDiscoveredAccounts.value = emptyList()
        mutableIsRefreshing.value = false
    }

    private fun initInternal(
        application: Application,
        config: GothwadConfig,
        store: TokenStore,
        oauth: OAuthService = OAuthClient(config),
    ) {
        if (ready != null) {
            require(this.config == config) { "GothwadAccounts.init called twice with different GothwadConfig" }
            return
        }
        this.config = config
        this.session = SessionManager(
            store = store,
            oauth = oauth,
            packageName = application.packageName,
        )
        this.peerBroker = PeerBrokerClient(
            localPackage = application.packageName,
            transport = AndroidPeerDirectoryTransport(application),
        )
        ready = scope.async {
            try {
                session.load()
                syncDiscoveredAccountsInternal()
            } catch (error: Exception) {
                mutableError.value = AuthFailure.StorageUnavailable
                throw error
            }
        }
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

    fun switchTo(userId: String) = scope.launch {
        action {
            readyOrThrow()
            session.switchTo(userId)
            syncDiscoveredAccountsInternal()
        }
    }

    /** Removes local app credentials and optionally best-effort revokes this client's token on server. */
    fun signOut(userId: String, revokeOnServer: Boolean = true) = scope.launch {
        action {
            readyOrThrow()
            session.signOut(userId, revokeOnServer = revokeOnServer)
            syncDiscoveredAccountsInternal()
        }
    }

    /**
     * Removes credentials from this app AND notifies mutually verified peer Gothwad apps on this
     * device via tombstone + verified IPC. Server sessions and browser cookies remain independent.
     */
    fun removeFromDevice(userId: String) = scope.launch {
        action {
            readyOrThrow()
            val removedAt = session.removeFromDevice(userId)
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
            syncDiscoveredAccountsInternal()
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

    internal fun callback(uri: String) = scope.launch {
        mutableIsRefreshing.value = true
        try {
            action {
                readyOrThrow()
                session.completeLogin(uri, config.redirectUri)
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
