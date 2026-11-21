package com.bansify.accounts.core

import com.bansify.accounts.core.broker.KnownGothwadApps
import java.net.URI

enum class Scope(val wireValue: String) {
    Profile("profile"),
    Email("email"),
    OfflineAccess("offline_access"),
    Mail("mail"),
    Drive("drive"),
    DriveRead("drive.read"),
    Notes("notes"),
    Chat("chat"),
    AccountRead("account.read"),
    AccountProfileWrite("account.profile.write"),
}

sealed interface BrokerTrustPolicy {
    /**
     * Explicit allowlist mapping each trusted package name to allowed lowercase SHA-256 certificate
     * fingerprints (64 hex chars, no colons). Use this when sibling apps are signed with distinct
     * production release keys (e.g., legacy Kiwi Store key alongside new org release key).
     */
    data class PinnedSigners(val packageCertSha256: Map<String, Set<String>>) : BrokerTrustPolicy {
        init {
            require(packageCertSha256.isNotEmpty())
            require(packageCertSha256.all { (pkg, certs) ->
                pkg.isNotBlank() && certs.isNotEmpty() && certs.all { it.matches(SHA256_HEX_REGEX) }
            }) { "PinnedSigners fingerprints must be 64-character SHA-256 hex strings" }
        }
    }

    /**
     * Trusts only explicitly allowlisted first-party package names whose APK signing certificate
     * matches the running app's own certificate. Rejects debug builds unless [allowDebugBuild] is true
     * (for local alpha/beta dev builds). Never trust arbitrary packages.
     */
    data class SameSigningCertificate(
        val knownPackages: Set<String> = KnownGothwadApps.FIRST_PARTY_PACKAGES,
        val allowDebugBuild: Boolean = false,
    ) : BrokerTrustPolicy {
        init {
            require(knownPackages.isNotEmpty() && knownPackages.all { it.isNotBlank() })
        }
    }

    private companion object {
        val SHA256_HEX_REGEX = Regex("^[0-9a-fA-F]{64}$")
    }
}

sealed interface BrokerPolicy {
    /** FOSS / untrusted-build default: disables peer discovery and refuses all incoming provider IPC. */
    data object Disabled : BrokerPolicy

    /**
     * Mutual certificate-verified peer account discovery across installed Gothwad apps.
     * Shares only non-secret identity metadata (zero tokens); target apps still obtain their own
     * client-scoped OAuth credentials.
     */
    data class PeerDiscovery(
        val trustPolicy: BrokerTrustPolicy = BrokerTrustPolicy.SameSigningCertificate(),
    ) : BrokerPolicy
}

data class GothwadConfig(
    val clientId: String,
    val redirectUri: String,
    val scopes: Set<Scope> = setOf(Scope.Profile, Scope.Email, Scope.OfflineAccess),
    val authBaseUrl: String = "https://accounts.bansify.com/api",
    val brokerPolicy: BrokerPolicy = BrokerPolicy.Disabled,
    val allowCustomSchemeRedirect: Boolean = true,
) {
    init {
        require(clientId.isNotBlank() && scopes.isNotEmpty())
        val base = URI(authBaseUrl)
        require(base.scheme == "https" && !base.host.isNullOrBlank())
        require(base.rawQuery == null && base.rawFragment == null && base.rawUserInfo == null)
        val redirect = URI(redirectUri)
        require(redirect.isAbsolute && !redirect.isOpaque && redirect.rawPath == "/oauth/callback")
        require(redirect.rawQuery == null && redirect.rawFragment == null && redirect.rawUserInfo == null)
        require(allowCustomSchemeRedirect || redirect.scheme == "https") {
            "Production config with allowCustomSchemeRedirect=false requires an https App Link redirectUri"
        }
        require(redirect.scheme == "https" || (redirect.scheme.contains('.') && redirect.rawAuthority == null))
        require(redirect.scheme != "https" || !redirect.host.isNullOrBlank())
    }
}
