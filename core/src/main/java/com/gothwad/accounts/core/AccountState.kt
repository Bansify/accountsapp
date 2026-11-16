package com.gothwad.accounts.core

import kotlinx.serialization.Serializable

@Serializable
data class GothwadAccount(
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val updatedAt: String? = null,
    val storageUsedBytes: Long? = null,
    val storageLimitBytes: Long? = null,
)

/**
 * Identity-only cross-app account metadata discovered from mutually certificate-verified
 * first-party Gothwad apps on this device. Never carries OAuth access or refresh tokens.
 */
@Serializable
data class DiscoveredAccount(
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val sourcePackage: String,
    val addedAt: Long = 0L,
    val lastSeenAt: Long = 0L,
    val isAuthorizedLocally: Boolean = false,
)

sealed interface AccountState {
    data object Initializing : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val account: GothwadAccount, val all: List<GothwadAccount>) : AccountState
    data class StorageCorrupted(val reason: String) : AccountState
}

enum class AuthFailure {
    BrowserUnavailable,
    InvalidCallback,
    LoginCancelled,
    SessionExpired,
    Unavailable,
    StorageUnavailable,
}
