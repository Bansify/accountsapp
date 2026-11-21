package com.gothwad.accounts.core

import kotlinx.serialization.Serializable

@Serializable
data class GothwadAccount(
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val phoneNumber: String? = null,
    val phoneNumberVerified: Boolean? = null,
    val recoveryEmail: String? = null,
    val recoveryEmailVerified: Boolean? = null,
    val hasPassword: Boolean? = null,
    val passwordChangedAt: String? = null,
    val mfaEnabled: Boolean? = null,
    val passkeyCount: Int? = null,
    val authorizationCount: Int? = null,
    val activeSessionsCount: Int? = null,
    val storageUsedBytes: Long? = null,
    val storageLimitBytes: Long? = null,
    val driveWriteAccess: Boolean? = null,
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

enum class DisconnectOutcome {
    /** Server revoked the entire user/client grant via POST /oauth/revoke and local storage was cleared. */
    RevokedOnServer,
    /** Offline or transient network failure; local credential cleared, server grant NOT confirmed revoked. */
    OfflineRemovedLocally,
    /** Server rejected token (already invalid/revoked); local credential cleared. */
    AlreadyInvalidOnServer,
}

data class BrowserAuthCompletion(
    val generation: Int,
    val userId: String,
)
