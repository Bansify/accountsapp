package com.gothwad.accounts.core.broker

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.gothwad.accounts.core.BrokerPolicy
import com.gothwad.accounts.core.BrokerTrustPolicy
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import kotlinx.serialization.json.Json

internal interface PeerDirectoryTransport {
    fun listInstalledCandidatePackages(policy: BrokerTrustPolicy): List<String>
    fun queryPeer(packageName: String, policy: BrokerTrustPolicy): DirectorySnapshot?
    fun notifyRemoval(packageName: String, userId: String, removedAt: Long, policy: BrokerTrustPolicy): Boolean
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

    fun broadcastDeviceRemoval(userId: String, removedAt: Long, policy: BrokerPolicy) {
        val discoveryPolicy = policy as? BrokerPolicy.PeerDiscovery ?: return
        for (pkg in transport.listInstalledCandidatePackages(discoveryPolicy.trustPolicy)) {
            transport.notifyRemoval(pkg, userId, removedAt, discoveryPolicy.trustPolicy)
        }
    }
}
