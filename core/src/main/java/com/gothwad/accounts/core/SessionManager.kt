package com.gothwad.accounts.core

import com.gothwad.accounts.core.broker.DirectoryAccountRecord
import com.gothwad.accounts.core.broker.DirectorySnapshot
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException

internal class SessionManager(
    private val store: TokenStore,
    private val oauth: OAuthService,
    private val packageName: String = "com.gothwad.accounts",
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()
    private var data = StoredSession()
    private var generation = 0L
    private var corrupted = false

    private val mutableState = MutableStateFlow<AccountState>(AccountState.Initializing)
    val state = mutableState.asStateFlow()

    suspend fun load() = mutex.withLock {
        try {
            data = store.read()
            corrupted = false
            publish()
        } catch (error: Exception) {
            if (error is GeneralSecurityException || error is SerializationException) {
                corrupted = true
                mutableState.value = AccountState.StorageCorrupted(
                    error.message ?: "Encrypted account storage could not be unlocked",
                )
            } else {
                throw error
            }
        }
    }

    suspend fun recoverCorruptedStorage() = mutex.withLock {
        generation++
        store.clear()
        val empty = StoredSession()
        store.write(empty)
        data = empty
        corrupted = false
        publish()
    }

    suspend fun beginLogin(): PendingLogin = mutex.withLock {
        check(!corrupted) { "Account storage is corrupted; recover before signing in" }
        generation++
        val pending = PendingLogin(Pkce.random(), Pkce.random(), clock())
        save(data.copy(pending = pending))
        pending
    }

    suspend fun completeLogin(callback: String, redirect: String): GothwadAccount {
        val (pending, params, epoch) = mutex.withLock {
            check(!corrupted) { "Account storage is corrupted" }
            val pending = requireNotNull(data.pending)
            require(clock() - pending.createdAt in 0..600_000)
            val params = CallbackValidator.parameters(callback, redirect, pending.state)
            save(data.copy(pending = null))
            Triple(pending, params, generation)
        }
        if (params.containsKey("error")) throw LoginCancelledException()
        val tokens = oauth.exchange(params.getValue("code"), pending.verifier)
        val account = oauth.userInfo(tokens.accessToken)
        mutex.withLock {
            if (generation != epoch) throw IOException("Login superseded")
            val now = clock()
            val previous = data.credentials.find { it.account.userId == account.userId }
            val addedAt = previous?.addedAt?.takeIf { it > 0L } ?: now
            val credential = Credential(
                account = account,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresAt = now + tokens.expiresIn * 1000,
                addedAt = addedAt,
                lastSeenAt = now,
            )
            // Signing back into a previously removed account supersedes older tombstones.
            val nextTombstones = data.deviceRemovalTombstones.toMutableMap().apply {
                val existingTombstone = this[account.userId]
                if (existingTombstone != null && now > existingTombstone) {
                    remove(account.userId)
                }
            }
            save(
                data.copy(
                    credentials = data.credentials.filterNot { it.account.userId == account.userId } + credential,
                    activeUserId = account.userId,
                    deviceRemovalTombstones = nextTombstones,
                ),
            )
        }
        return account
    }

    suspend fun completeNativeAuth(auth: MobileAuthSignInOutcome.Authenticated): GothwadAccount {
        val epoch = mutex.withLock {
            check(!corrupted) { "Account storage is corrupted" }
            generation++
            generation
        }
        val account = runCatching { oauth.userInfo(auth.accessToken) }
            .getOrElse {
                auth.user?.toGothwadAccount()
                    ?: throw IOException("Could not load authenticated account profile", it)
            }
        persistAuthenticatedSession(
            account = account,
            accessToken = auth.accessToken,
            refreshToken = auth.refreshToken,
            expiresInSeconds = auth.expiresIn,
            expectedEpoch = epoch,
        )
        return account
    }

    suspend fun completePeerCodeExchange(code: String, verifier: String): GothwadAccount {
        val epoch = mutex.withLock {
            check(!corrupted) { "Account storage is corrupted" }
            generation++
            generation
        }
        val tokens = oauth.exchange(code, verifier)
        val account = oauth.userInfo(tokens.accessToken)
        persistAuthenticatedSession(
            account = account,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresInSeconds = tokens.expiresIn,
            expectedEpoch = epoch,
        )
        return account
    }

    private suspend fun persistAuthenticatedSession(
        account: GothwadAccount,
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long,
        expectedEpoch: Long,
    ) = mutex.withLock {
        if (generation != expectedEpoch) throw IOException("Login superseded")
        val now = clock()
        val previous = data.credentials.find { it.account.userId == account.userId }
        val addedAt = previous?.addedAt?.takeIf { it > 0L } ?: now
        val credential = Credential(
            account = account,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = now + expiresInSeconds * 1000,
            addedAt = addedAt,
            lastSeenAt = now,
        )
        val nextTombstones = data.deviceRemovalTombstones.toMutableMap().apply {
            val existingTombstone = this[account.userId]
            if (existingTombstone != null && now > existingTombstone) {
                remove(account.userId)
            }
        }
        save(
            data.copy(
                credentials = data.credentials.filterNot { it.account.userId == account.userId } + credential,
                activeUserId = account.userId,
                pending = null,
                deviceRemovalTombstones = nextTombstones,
            ),
        )
    }

    suspend fun switchTo(userId: String) = mutex.withLock {
        check(!corrupted) { "Account storage is corrupted" }
        require(data.credentials.any { it.account.userId == userId })
        val now = clock()
        val updated = data.credentials.map {
            if (it.account.userId == userId) it.copy(lastSeenAt = now) else it
        }
        save(data.copy(credentials = updated, activeUserId = userId))
    }

    /**
     * App-local removal by default (`revokeOnServer = false`), preserving grants on other devices.
     * When `revokeOnServer = true`, attempts `POST /oauth/revoke` BEFORE deleting local tokens
     * (refreshing the access token first if needed) and reports the true server vs offline outcome.
     */
    suspend fun signOut(userId: String, revokeOnServer: Boolean = false): DisconnectOutcome {
        val outcome = if (revokeOnServer) {
            revokeGrantForUser(userId)
        } else {
            DisconnectOutcome.OfflineRemovedLocally
        }
        removeLocalCredential(userId)
        return outcome
    }

    /**
     * Removes local tokens and records a device-removal tombstone at [removedAt].
     * Defaults to local/device-only (`revokeOnServer = false`) so sibling installations on other
     * devices stay independent unless explicitly disconnected from server.
     */
    suspend fun removeFromDevice(
        userId: String,
        removedAt: Long = clock(),
        revokeOnServer: Boolean = false,
    ): Long {
        if (revokeOnServer) {
            revokeGrantForUser(userId)
        }
        mutex.withLock {
            check(!corrupted) { "Account storage is corrupted" }
            generation++
            val remaining = data.credentials.filterNot { it.account.userId == userId }
            val nextTombstones = data.deviceRemovalTombstones + (userId to removedAt)
            save(
                data.copy(
                    credentials = remaining,
                    activeUserId = if (data.activeUserId == userId) remaining.firstOrNull()?.account?.userId else data.activeUserId,
                    pending = null,
                    deviceRemovalTombstones = nextTombstones,
                ),
            )
        }
        return removedAt
    }

    private suspend fun revokeGrantForUser(userId: String): DisconnectOutcome {
        val cred = mutex.withLock {
            check(!corrupted) { "Account storage is corrupted" }
            data.credentials.find { it.account.userId == userId }
        } ?: return DisconnectOutcome.AlreadyInvalidOnServer

        var bearer = cred.accessToken
        var refresh = cred.refreshToken
        if (cred.expiresAt - clock() <= 60_000 && !refresh.isNullOrBlank()) {
            val refreshed = runCatching { oauth.refresh(refresh) }
            refreshed.onSuccess {
                bearer = it.accessToken
                refresh = it.refreshToken ?: refresh
            }
            val ex = refreshed.exceptionOrNull()
            if (ex is OAuthException && ex.rejected) {
                return DisconnectOutcome.AlreadyInvalidOnServer
            }
        }
        return when (oauth.revoke(refresh, bearer)) {
            RevokeStatus.Revoked -> DisconnectOutcome.RevokedOnServer
            RevokeStatus.InvalidGrant -> DisconnectOutcome.AlreadyInvalidOnServer
            RevokeStatus.Failed -> DisconnectOutcome.OfflineRemovedLocally
        }
    }

    private suspend fun removeLocalCredential(userId: String) = mutex.withLock {
        check(!corrupted) { "Account storage is corrupted" }
        generation++
        val remaining = data.credentials.filterNot { it.account.userId == userId }
        save(
            data.copy(
                credentials = remaining,
                activeUserId = if (data.activeUserId == userId) remaining.firstOrNull()?.account?.userId else data.activeUserId,
                pending = null,
            ),
        )
    }

    /** Applies a verified peer's device-removal notification when [removedAt] is newer than local sign-in. */
    suspend fun applyPeerDeviceRemoval(userId: String, removedAt: Long) = mutex.withLock {
        if (corrupted) return@withLock
        val existingTombstone = data.deviceRemovalTombstones[userId] ?: 0L
        val effectiveTombstone = maxOf(existingTombstone, removedAt)
        val localCred = data.credentials.find { it.account.userId == userId }
        val shouldRemoveLocal = localCred != null && maxOf(localCred.addedAt, localCred.lastSeenAt) <= effectiveTombstone
        if (shouldRemoveLocal) {
            generation++
        }
        val remaining = if (shouldRemoveLocal) {
            data.credentials.filterNot { it.account.userId == userId }
        } else {
            data.credentials
        }
        val nextActive = if (shouldRemoveLocal && data.activeUserId == userId) {
            remaining.firstOrNull()?.account?.userId
        } else {
            data.activeUserId
        }
        save(
            data.copy(
                credentials = remaining,
                activeUserId = nextActive,
                deviceRemovalTombstones = data.deviceRemovalTombstones + (userId to effectiveTombstone),
            ),
        )
    }

    /** Refreshes the active user's `/oauth/userinfo` profile and quota fields. */
    suspend fun refreshActiveProfile(): GothwadAccount? {
        val userId = mutex.withLock { data.activeUserId } ?: return null
        val token = accessToken() ?: return null
        val original = mutex.withLock { data.credentials.find { it.account.userId == userId } } ?: return null
        val updated = oauth.userInfo(token)
        return mutex.withLock {
            val current = data.credentials.find { it.account.userId == userId } ?: return@withLock null
            if (current.addedAt != original.addedAt) return@withLock null
            val next = current.copy(account = updated, lastSeenAt = clock())
            save(data.copy(credentials = data.credentials.map { if (it.account.userId == userId) next else it }))
            updated
        }
    }

    suspend fun activeUserId(): String? = mutex.withLock {
        if (corrupted) null else data.activeUserId
    }

    suspend fun accessToken(): String? {
        val userId = mutex.withLock { data.activeUserId } ?: return null
        return accessTokenForResolvedUserId(userId, requireActive = true)
    }

    suspend fun activeAccessTokenAndUserId(): Pair<String, String>? {
        val userId = mutex.withLock { if (corrupted) null else data.activeUserId } ?: return null
        val token = accessTokenForResolvedUserId(userId, requireActive = true) ?: return null
        return userId to token
    }

    /**
     * Resolves a local credential matching [userId], [username], or [email] (or active user if all blank)
     * and returns `(resolvedUserId, validAccessToken)`, silently refreshing via [oauth.refresh] when needed.
     */
    suspend fun accessTokenForUser(
        userId: String? = null,
        username: String? = null,
        email: String? = null,
    ): Pair<String, String>? {
        val resolvedUserId = mutex.withLock {
            if (corrupted) return@withLock null
            val wantedId = userId?.trim().orEmpty()
            val wantedUsername = username?.trim()?.lowercase().orEmpty()
            val wantedEmail = email?.trim()?.lowercase().orEmpty()
            val matched = when {
                wantedId.isNotEmpty() -> data.credentials.find { it.account.userId == wantedId }
                wantedUsername.isNotEmpty() -> data.credentials.find { it.account.username?.lowercase() == wantedUsername }
                wantedEmail.isNotEmpty() -> data.credentials.find { it.account.email?.lowercase() == wantedEmail }
                else -> data.credentials.find { it.account.userId == data.activeUserId } ?: data.credentials.firstOrNull()
            }
            matched?.account?.userId
        } ?: return null

        val token = accessTokenForResolvedUserId(resolvedUserId, requireActive = false) ?: return null
        return resolvedUserId to token
    }

    private suspend fun accessTokenForResolvedUserId(userId: String, requireActive: Boolean): String? {
        return refreshLocks.getOrPut(userId) { Mutex() }.withLock {
            val original = mutex.withLock {
                data.credentials.find { it.account.userId == userId }
                    ?.takeIf { !requireActive || data.activeUserId == userId }
            } ?: return@withLock null
            val remainingMs = original.expiresAt - clock()
            if (remainingMs > 60_000) return@withLock original.accessToken
            val refreshToken = original.refreshToken
            if (refreshToken == null) {
                if (remainingMs > 0) return@withLock original.accessToken
                throw OAuthException(true)
            }
            val tokens = try {
                oauth.refresh(refreshToken)
            } catch (error: OAuthException) {
                if (error.rejected) mutex.withLock {
                    if (data.credentials.find { it.account.userId == userId } == original) {
                        val remaining = data.credentials.filterNot { it.account.userId == userId }
                        save(
                            data.copy(
                                credentials = remaining,
                                activeUserId = if (data.activeUserId == userId) remaining.firstOrNull()?.account?.userId else data.activeUserId,
                            ),
                        )
                    }
                }
                throw error
            }
            mutex.withLock persist@{
                if (data.credentials.find { it.account.userId == userId } != original) return@persist null
                val rotated = original.copy(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken ?: original.refreshToken,
                    expiresAt = clock() + tokens.expiresIn * 1000,
                )
                save(data.copy(credentials = data.credentials.map { if (it.account.userId == userId) rotated else it }))
                if (!requireActive || data.activeUserId == userId) rotated.accessToken else null
            }
        }
    }

    /** Non-secret snapshot safe to expose to mutually verified sibling Gothwad apps. */
    suspend fun directorySnapshot(): DirectorySnapshot = mutex.withLock {
        if (corrupted) return@withLock DirectorySnapshot()
        DirectorySnapshot(
            sourcePackage = packageName,
            accounts = data.credentials.map { cred ->
                DirectoryAccountRecord(
                    userId = cred.account.userId,
                    username = cred.account.username,
                    name = cred.account.name,
                    email = cred.account.email,
                    avatarUrl = cred.account.avatarUrl,
                    addedAt = cred.addedAt,
                    lastSeenAt = cred.lastSeenAt,
                )
            },
            tombstones = data.deviceRemovalTombstones,
        )
    }

    suspend fun currentLocalAccounts(): List<GothwadAccount> = mutex.withLock {
        if (corrupted) emptyList() else data.credentials.map { it.account }
    }

    private fun save(next: StoredSession) {
        store.write(next)
        data = next
        publish()
    }

    private fun publish() {
        val all = data.credentials.map { it.account }
        val active = all.find { it.userId == data.activeUserId } ?: all.firstOrNull()
        mutableState.value = active?.let { AccountState.SignedIn(it, all) } ?: AccountState.SignedOut
    }
}

internal class LoginCancelledException : Exception()
