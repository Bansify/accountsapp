package com.gothwad.accounts.core.broker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import com.gothwad.accounts.core.BrokerPolicy
import com.gothwad.accounts.core.GothwadAccounts
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Distributed, mutually certificate-verified cross-app directory provider.
 *
 * Security properties:
 * - Captures `Binder.getCallingUid()` synchronously before any work or storage access.
 * - Rejects ALL queries immediately when [BrokerPolicy.Disabled] is configured.
 * - Verifies all packages sharing the calling UID against [BrokerTrustPolicy] before reading state.
 * - Never holds or returns OAuth access tokens or refresh tokens; exposes identity metadata only.
 */
class AccountDirectoryProvider : ContentProvider() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        // Capture Binder caller identity synchronously before any dispatch or storage read.
        val callingUid = Binder.getCallingUid()
        val ctx = context ?: throw SecurityException("Provider context unavailable")

        val policy = GothwadAccounts.currentBrokerPolicy() as? BrokerPolicy.PeerDiscovery
            ?: throw SecurityException("Gothwad peer broker is disabled")

        val verified = TrustVerifier.verifyBinderCaller(ctx, callingUid, policy.trustPolicy)
            ?: throw SecurityException("Untrusted caller UID: $callingUid")

        if (arg != null && arg.toByteArray().size > BrokerIpcContract.MAX_ARG_BYTES) {
            throw IllegalArgumentException("IPC argument exceeds bounds")
        }

        return when (method) {
            BrokerIpcContract.METHOD_LIST_ACCOUNTS -> {
                val snapshot = GothwadAccounts.directorySnapshotBlocking()
                val encoded = json.encodeToString(snapshot)
                if (encoded.toByteArray().size > BrokerIpcContract.MAX_SNAPSHOT_BYTES) {
                    throw IllegalStateException("Directory snapshot exceeds bounds")
                }
                Bundle().apply {
                    putString(BrokerIpcContract.KEY_SNAPSHOT_JSON, encoded)
                }
            }

            BrokerIpcContract.METHOD_REMOVE_FROM_DEVICE -> {
                val userId = extras?.getString(BrokerIpcContract.KEY_USER_ID)?.trim().orEmpty()
                val removedAt = extras?.getLong(BrokerIpcContract.KEY_REMOVED_AT, -1L) ?: -1L
                if (userId.isBlank() || userId.length > 256 || removedAt <= 0L) {
                    throw IllegalArgumentException("Invalid removal payload from ${verified.packages.firstOrNull()}")
                }
                GothwadAccounts.applyPeerDeviceRemovalBlocking(userId, removedAt)
                Bundle().apply {
                    putBoolean(BrokerIpcContract.KEY_OK, true)
                }
            }

            else -> throw IllegalArgumentException("Unsupported directory method: $method")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        enforceTrustedCallerIfDirectUriQuery()
        return null
    }

    override fun getType(uri: Uri): String? {
        enforceTrustedCallerIfDirectUriQuery()
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        enforceTrustedCallerIfDirectUriQuery()
        throw UnsupportedOperationException("Insert not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        enforceTrustedCallerIfDirectUriQuery()
        throw UnsupportedOperationException("Delete not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        enforceTrustedCallerIfDirectUriQuery()
        throw UnsupportedOperationException("Update not supported")
    }

    private fun enforceTrustedCallerIfDirectUriQuery() {
        val callingUid = Binder.getCallingUid()
        val ctx = context ?: throw SecurityException("Provider context unavailable")
        val policy = GothwadAccounts.currentBrokerPolicy() as? BrokerPolicy.PeerDiscovery
            ?: throw SecurityException("Gothwad peer broker is disabled")
        TrustVerifier.verifyBinderCaller(ctx, callingUid, policy.trustPolicy)
            ?: throw SecurityException("Untrusted caller UID: $callingUid")
    }
}
