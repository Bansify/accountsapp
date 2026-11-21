package com.bansify.accounts.core.broker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import com.bansify.accounts.core.BrokerPolicy
import com.bansify.accounts.core.GothwadAccounts
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Distributed, mutually certificate-verified cross-app directory and zero-browser PKCE code
 * issuer provider.
 *
 * Security properties:
 * - Captures `Binder.getCallingUid()` synchronously before any work or storage access.
 * - Rejects ALL queries immediately when [BrokerPolicy.Disabled] is configured.
 * - Verifies all packages sharing the calling UID against [BrokerTrustPolicy] before reading state.
 * - Never returns raw OAuth access tokens or refresh tokens across IPC; exposes only public
 *   identity metadata or short-lived PKCE authorization codes bound to the target first-party client.
 */
class AccountDirectoryProvider : ContentProvider() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        // Capture Binder caller identity synchronously before any dispatch or storage read.
        val callingUid = Binder.getCallingUid()
        val ctx = context ?: throw SecurityException("Provider context unavailable")

        if (arg != null && arg.toByteArray().size > BrokerIpcContract.MAX_ARG_BYTES) {
            throw IllegalArgumentException("IPC argument exceeds bounds")
        }

        val brokerPolicy = GothwadAccounts.currentBrokerPolicy()

        if (method == BrokerIpcContract.METHOD_ISSUE_PEER_CODE) {
            val peerPolicy = brokerPolicy as? BrokerPolicy.PeerDiscovery
            if (peerPolicy == null || !TrustVerifier.isTrustedUid(ctx, callingUid, peerPolicy.trustPolicy)) {
                return Bundle().apply {
                    putBoolean(BrokerIpcContract.KEY_OK, false)
                    putString(BrokerIpcContract.KEY_ERROR, "untrusted_caller")
                }
            }
            val userId = extras?.getString(BrokerIpcContract.KEY_USER_ID)?.trim().orEmpty()
            val username = extras?.getString(BrokerIpcContract.KEY_USERNAME)?.trim().orEmpty()
            val email = extras?.getString(BrokerIpcContract.KEY_EMAIL)?.trim().orEmpty()
            val targetClientId = extras?.getString(BrokerIpcContract.KEY_TARGET_CLIENT_ID)?.trim().orEmpty()
            val redirectUri = extras?.getString(BrokerIpcContract.KEY_REDIRECT_URI)?.trim().orEmpty()
            val scope = extras?.getString(BrokerIpcContract.KEY_SCOPE)?.trim().orEmpty()
            val codeChallenge = extras?.getString(BrokerIpcContract.KEY_CODE_CHALLENGE)?.trim().orEmpty()
            val codeChallengeMethod = extras?.getString(BrokerIpcContract.KEY_CODE_CHALLENGE_METHOD)?.trim().orEmpty()
                .ifBlank { "S256" }

            if (targetClientId.isBlank() || redirectUri.isBlank() || codeChallenge.isBlank()) {
                return Bundle().apply {
                    putBoolean(BrokerIpcContract.KEY_OK, false)
                    putString(BrokerIpcContract.KEY_ERROR, "invalid_request")
                }
            }

            val result = GothwadAccounts.issuePeerCodeForCallerBlocking(
                userId = userId.ifBlank { null },
                username = username.ifBlank { null },
                email = email.ifBlank { null },
                targetClientId = targetClientId,
                redirectUri = redirectUri,
                scope = scope,
                codeChallenge = codeChallenge,
                codeChallengeMethod = codeChallengeMethod,
            )
            return result.fold(
                onSuccess = { peerCode ->
                    Bundle().apply {
                        putBoolean(BrokerIpcContract.KEY_OK, true)
                        putString(BrokerIpcContract.KEY_CODE, peerCode.code)
                        putString(BrokerIpcContract.KEY_REDIRECT_URI, peerCode.redirectUri)
                        putString(BrokerIpcContract.KEY_SCOPE, peerCode.scope)
                    }
                },
                onFailure = { err ->
                    Bundle().apply {
                        putBoolean(BrokerIpcContract.KEY_OK, false)
                        putString(BrokerIpcContract.KEY_ERROR, err.message ?: "peer_code_failed")
                    }
                },
            )
        }

        val policy = brokerPolicy as? BrokerPolicy.PeerDiscovery
            ?: throw SecurityException("Gothwad peer broker is disabled")

        val verified = TrustVerifier.verifyBinderCaller(ctx, callingUid, policy.trustPolicy)
            ?: throw SecurityException("Untrusted caller UID: $callingUid")

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
