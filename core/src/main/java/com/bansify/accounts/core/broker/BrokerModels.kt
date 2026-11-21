package com.bansify.accounts.core.broker

import kotlinx.serialization.Serializable

object KnownGothwadApps {
    const val ACCOUNTS = "com.bansify.accounts"
    const val MAIL = "com.gothwad.mail"
    const val DRIVE = "com.gothwad.drive"
    const val STORE = "com.gothwad.store"
    const val TUBE = "com.gothwad.tube"
    const val SAMPLE_ALPHA = "com.bansify.accounts.sample.alpha"
    const val SAMPLE_BETA = "com.bansify.accounts.sample.beta"

    val FIRST_PARTY_PACKAGES: Set<String> = setOf(
        ACCOUNTS,
        MAIL,
        DRIVE,
        STORE,
        TUBE,
        SAMPLE_ALPHA,
        SAMPLE_BETA,
    )

    fun directoryAuthorityFor(packageName: String): String =
        "$packageName.bansify.account.directory"
}

internal object BrokerIpcContract {
    const val METHOD_LIST_ACCOUNTS = "gothwad.directory.list_v1"
    const val METHOD_REMOVE_FROM_DEVICE = "gothwad.directory.remove_v1"
    const val METHOD_ISSUE_PEER_CODE = "issue_peer_code"

    const val KEY_SNAPSHOT_JSON = "snapshot_json"
    const val KEY_USER_ID = "user_id"
    const val KEY_USERNAME = "username"
    const val KEY_EMAIL = "email"
    const val KEY_REMOVED_AT = "removed_at"
    const val KEY_OK = "ok"
    const val KEY_ERROR = "error"
    const val KEY_TARGET_CLIENT_ID = "target_client_id"
    const val KEY_REDIRECT_URI = "redirect_uri"
    const val KEY_SCOPE = "scope"
    const val KEY_CODE_CHALLENGE = "code_challenge"
    const val KEY_CODE_CHALLENGE_METHOD = "code_challenge_method"
    const val KEY_CODE = "code"
    const val MAX_ARG_BYTES = 4096
    const val MAX_SNAPSHOT_BYTES = 256 * 1024
}

@Serializable
internal data class DirectoryAccountRecord(
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val addedAt: Long = 0L,
    val lastSeenAt: Long = 0L,
)

@Serializable
internal data class DirectorySnapshot(
    val version: Int = 1,
    val sourcePackage: String = "",
    val accounts: List<DirectoryAccountRecord> = emptyList(),
    val tombstones: Map<String, Long> = emptyMap(),
)

internal data class VerifiedCaller(
    val uid: Int,
    val packages: List<String>,
)
