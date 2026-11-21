package com.bansify.accounts.core.broker

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.bansify.accounts.core.BrokerPolicy
import com.bansify.accounts.core.BrokerTrustPolicy
import com.bansify.accounts.core.DiscoveredAccount
import com.bansify.accounts.core.GothwadAccount
import com.bansify.accounts.core.PeerAuthCodeResult
import java.io.IOException
import kotlinx.serialization.json.Json

internal interface PeerDirectoryTransport {
    fun listInstalledCandidatePackages(policy: BrokerTrustPolicy): List<String>
    fun queryPeer(packageName: String, policy: BrokerTrustPolicy): DirectorySnapshot?
    fun notifyRemoval(packageName: String, userId: String, removedAt: Long, policy: BrokerTrustPolicy): Boolean
    fun requestPeerAuthCode(
        packageName: String,
        policy: BrokerTrustPolicy,
        userId: String?,
        username: String?,
        email: String?,
        targetClientId: String,
        redirectUri: String,
        scope: String,
        codeChallenge: String,
        codeChallengeMethod: String = "S256",
    ): Result<PeerAuthCodeResult> = Result.failure(UnsupportedOperationException("Peer auth code not supported"))
}

internal class AndroidPeerDirectoryTransport(
    private val context: Context,
) : PeerDirectoryTransport {
    private val json = Json { ignoreUnknownKeys = true }

    override fun listInstalledCandidatePackages(policy: BrokerTrustPolicy): List<String> {
        val candidates = when (policy) {
            is BrokerTrustPolicy.PinnedSigners -> policy.packageCertSha256.keys
            is BrokerTrustPolicy.SameSigningCertificate -> policy.knownPackages
        }
        return candidates
            .filter { it != context.packageName }
            .filter { pkg ->
                val authority = KnownGothwadApps.directoryAuthorityFor(pkg)
                val info = context.packageManager.resolveContentProvider(authority, 0)
                info != null && info.packageName == pkg && TrustVerifier.verifyPackage(context, pkg, policy)
            }
            .sorted()
    }

    override fun queryPeer(packageName: String, policy: BrokerTrustPolicy): DirectorySnapshot? {
        if (packageName == context.packageName) return null
        val authority = KnownGothwadApps.directoryAuthorityFor(packageName)
        val info = context.packageManager.resolveContentProvider(authority, 0) ?: return null
        if (info.packageName != packageName || !TrustVerifier.verifyPackage(context, packageName, policy)) {
            return null
        }
        return runCatching {
            val uri = Uri.parse("content://$authority")
            val response = context.contentResolver.call(uri, BrokerIpcContract.METHOD_LIST_ACCOUNTS, null, null)
                ?: return@runCatching null
            val raw = response.getString(BrokerIpcContract.KEY_SNAPSHOT_JSON) ?: return@runCatching null
            if (raw.toByteArray().size > BrokerIpcContract.MAX_SNAPSHOT_BYTES) return@runCatching null
            json.decodeFromString<DirectorySnapshot>(raw)
        }.getOrNull()
    }

    override fun notifyRemoval(
        packageName: String,
        userId: String,
        removedAt: Long,
        policy: BrokerTrustPolicy,
    ): Boolean {
        if (packageName == context.packageName) return false
        val authority = KnownGothwadApps.directoryAuthorityFor(packageName)
        val info = context.packageManager.resolveContentProvider(authority, 0) ?: return false
        if (info.packageName != packageName || !TrustVerifier.verifyPackage(context, packageName, policy)) {
            return false
        }
        return runCatching {
            val uri = Uri.parse("content://$authority")
            val extras = Bundle().apply {
                putString(BrokerIpcContract.KEY_USER_ID, userId)
                putLong(BrokerIpcContract.KEY_REMOVED_AT, removedAt)
            }
            val result = context.contentResolver.call(uri, BrokerIpcContract.METHOD_REMOVE_FROM_DEVICE, null, extras)
            result?.getBoolean(BrokerIpcContract.KEY_OK, false) == true
        }.getOrDefault(false)
    }

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
    ): Result<PeerAuthCodeResult> {
        if (packageName == context.packageName) {
            return Result.failure(IllegalArgumentException("Cannot issue peer code from local package"))
        }
        val authority = KnownGothwadApps.directoryAuthorityFor(packageName)
        val info = context.packageManager.resolveContentProvider(authority, 0)
            ?: return Result.failure(IOException("Peer directory provider unavailable"))
        if (info.packageName != packageName || !TrustVerifier.verifyPackage(context, packageName, policy)) {
            return Result.failure(SecurityException("Peer package verification failed"))
        }
        return runCatching {
            val uri = Uri.parse("content://$authority")
            val extras = Bundle().apply {
                if (!userId.isNullOrBlank()) putString(BrokerIpcContract.KEY_USER_ID, userId)
                if (!username.isNullOrBlank()) putString(BrokerIpcContract.KEY_USERNAME, username)
                if (!email.isNullOrBlank()) putString(BrokerIpcContract.KEY_EMAIL, email)
                putString(BrokerIpcContract.KEY_TARGET_CLIENT_ID, targetClientId)
                putString(BrokerIpcContract.KEY_REDIRECT_URI, redirectUri)
                putString(BrokerIpcContract.KEY_SCOPE, scope)
                putString(BrokerIpcContract.KEY_CODE_CHALLENGE, codeChallenge)
                putString(BrokerIpcContract.KEY_CODE_CHALLENGE_METHOD, codeChallengeMethod)
            }
            val response = context.contentResolver.call(uri, BrokerIpcContract.METHOD_ISSUE_PEER_CODE, null, extras)
                ?: throw IOException("No response from peer account provider")
            if (!response.getBoolean(BrokerIpcContract.KEY_OK, false)) {
                val err = response.getString(BrokerIpcContract.KEY_ERROR)?.takeIf { it.isNotBlank() } ?: "peer_code_rejected"
                throw IOException(err)
            }
            val code = response.getString(BrokerIpcContract.KEY_CODE)?.takeIf { it.isNotBlank() }
                ?: throw IOException("Peer returned empty authorization code")
            PeerAuthCodeResult(
                code = code,
                clientId = targetClientId,
                redirectUri = response.getString(BrokerIpcContract.KEY_REDIRECT_URI) ?: redirectUri,
                scope = response.getString(BrokerIpcContract.KEY_SCOPE) ?: scope,
            )
        }
    }
}

internal class PeerBrokerClient(
    private val localPackage: String,
    private val transport: PeerDirectoryTransport,
) {
    fun installedVerifiedPeers(policy: BrokerPolicy): List<String> = when (policy) {
        BrokerPolicy.Disabled -> emptyList()
        is BrokerPolicy.PeerDiscovery -> transport.listInstalledCandidatePackages(policy.trustPolicy)
    }

    fun discoverAccounts(
        localSnapshot: DirectorySnapshot,
        localAccounts: List<GothwadAccount>,
        policy: BrokerPolicy,
    ): List<DiscoveredAccount> {
        val localIds = localAccounts.map { it.userId }.toSet()

        if (policy is BrokerPolicy.Disabled) {
            return localSnapshot.accounts.map { rec ->
                DiscoveredAccount(
                    userId = rec.userId,
                    username = rec.username,
                    name = rec.name,
                    email = rec.email,
                    avatarUrl = rec.avatarUrl,
                    sourcePackage = localPackage,
                    addedAt = rec.addedAt,
                    lastSeenAt = rec.lastSeenAt,
                    isAuthorizedLocally = rec.userId in localIds,
                )
            }
        }

        val discoveryPolicy = policy as BrokerPolicy.PeerDiscovery
        val snapshots = mutableListOf(localSnapshot)
        for (pkg in transport.listInstalledCandidatePackages(discoveryPolicy.trustPolicy)) {
            transport.queryPeer(pkg, discoveryPolicy.trustPolicy)?.let { snapshots.add(it) }
        }

        // Merge device-removal tombstones across local store + verified peers.
        val tombstones = mutableMapOf<String, Long>()
        for (snap in snapshots) {
            for ((uid, ts) in snap.tombstones) {
                tombstones[uid] = maxOf(tombstones[uid] ?: 0L, ts)
            }
        }

        val merged = linkedMapOf<String, DiscoveredAccount>()
        for (snap in snapshots) {
            val pkg = snap.sourcePackage.ifBlank { localPackage }
            for (rec in snap.accounts) {
                val isLocal = rec.userId in localIds
                val tombstoneAt = tombstones[rec.userId] ?: 0L
                val freshness = maxOf(rec.addedAt, rec.lastSeenAt)
                if (!isLocal && tombstoneAt > 0L && freshness <= tombstoneAt) {
                    continue
                }
                val candidate = DiscoveredAccount(
                    userId = rec.userId,
                    username = rec.username,
                    name = rec.name,
                    email = rec.email,
                    avatarUrl = rec.avatarUrl,
                    sourcePackage = pkg,
                    addedAt = rec.addedAt,
                    lastSeenAt = rec.lastSeenAt,
                    isAuthorizedLocally = isLocal,
                )
                val existing = merged[rec.userId]
                if (existing == null ||
                    (candidate.isAuthorizedLocally && !existing.isAuthorizedLocally) ||
                    candidate.lastSeenAt > existing.lastSeenAt
                ) {
                    merged[rec.userId] = candidate.copy(
                        isAuthorizedLocally = existing?.isAuthorizedLocally == true || candidate.isAuthorizedLocally,
                    )
                }
            }
        }

        return merged.values.sortedWith(
            compareByDescending<DiscoveredAccount> { it.isAuthorizedLocally }
                .thenByDescending { it.lastSeenAt }
                .thenBy { it.email ?: it.userId },
        )
    }

    fun requestPeerAuthCode(
        peer: DiscoveredAccount,
        policy: BrokerPolicy,
        targetClientId: String,
        redirectUri: String,
        scope: String,
        codeChallenge: String,
        codeChallengeMethod: String = "S256",
    ): Result<PeerAuthCodeResult> {
        val discoveryPolicy = policy as? BrokerPolicy.PeerDiscovery
            ?: return Result.failure(SecurityException("Gothwad peer broker is disabled"))
        return transport.requestPeerAuthCode(
            packageName = peer.sourcePackage,
            policy = discoveryPolicy.trustPolicy,
            userId = peer.userId,
            username = peer.username,
            email = peer.email,
            targetClientId = targetClientId,
            redirectUri = redirectUri,
            scope = scope,
            codeChallenge = codeChallenge,
            codeChallengeMethod = codeChallengeMethod,
        )
    }

    fun broadcastDeviceRemoval(userId: String, removedAt: Long, policy: BrokerPolicy) {
        val discoveryPolicy = policy as? BrokerPolicy.PeerDiscovery ?: return
        for (pkg in transport.listInstalledCandidatePackages(discoveryPolicy.trustPolicy)) {
            transport.notifyRemoval(pkg, userId, removedAt, discoveryPolicy.trustPolicy)
        }
    }
}
