# Public API

## 1. Initialization (`core`)

Initialize once from `Application.onCreate` (required on every process launch, including callback restoration after process death):

```kotlin
GothwadAccounts.init(
    this,
    GothwadConfig(
        clientId = "gothwad-accounts-android",
        redirectUri = if (BuildConfig.DEBUG) {
            "$packageName:/oauth/callback"
        } else {
            "https://accounts.bansify.com/oauth/callback"
        },
        allowCustomSchemeRedirect = BuildConfig.DEBUG, // Release enforces verified HTTPS App Links
        scopes = setOf(
            Scope.Profile,
            Scope.Email,
            Scope.OfflineAccess,
            Scope.DriveRead,
            Scope.AccountRead,
            Scope.AccountProfileWrite,
        ),
        brokerPolicy = BrokerPolicy.PeerDiscovery(
            trustPolicy = BrokerTrustPolicy.SameSigningCertificate(
                allowDebugBuild = BuildConfig.DEBUG,
            ),
        ),
    ),
)
```

For FOSS or zero-IPC builds, leave `brokerPolicy = BrokerPolicy.Disabled` (the default). Config is immutable per process.

## 2. State flows (`core`)

```kotlin
val state by GothwadAccounts.state.collectAsStateWithLifecycle()
val discovered by GothwadAccounts.discoveredAccounts.collectAsStateWithLifecycle()
val isRefreshing by GothwadAccounts.isRefreshing.collectAsStateWithLifecycle()
val error by GothwadAccounts.error.collectAsStateWithLifecycle()
val lastDisconnect by GothwadAccounts.lastDisconnectOutcome.collectAsStateWithLifecycle()
```

- **`AccountState`** (host-local credentials):
  - `Initializing` — initial encrypted storage load in progress.
  - `SignedOut` — no locally authorized accounts in this app (`discoveredAccounts` may still contain peer accounts from verified sibling apps).
  - `SignedIn(account, all)` — locally authorized account(s) in this app, with `account` selected for this app only.
  - `StorageCorrupted(reason)` — encrypted storage or Android Keystore key became unreadable; fails closed (no plaintext fallback). Recover via `GothwadAccounts.recoverCorruptedStorage()`.
- **`DiscoveredAccount`** (device-wide zero-token directory):
  - Combines local accounts (`isAuthorizedLocally = true`) and verified sibling app metadata (`isAuthorizedLocally = false`, zero tokens).
- **`DisconnectOutcome`**:
  - `RevokedOnServer` — `POST /oauth/revoke` revoked the entire user/client grant across all installations of that app.
  - `OfflineRemovedLocally` — offline/network failure; removed locally without claiming server revocation.
  - `AlreadyInvalidOnServer` — grant already expired/revoked on server; local credential cleared.

## 3. Lifecycle & first-party mobile management actions (`core`)

```kotlin
// Direct native authentication (zero browser required):
GothwadAccounts.checkIdentifier(identifier)
GothwadAccounts.checkUsername(username)
GothwadAccounts.signInNative(identifier, password)        // Returns Authenticated or MfaRequired
GothwadAccounts.verifyNativeMfa(challenge, code)
GothwadAccounts.signUpNative(input)
GothwadAccounts.forgotPasswordNative(username)
GothwadAccounts.resetPasswordNative(token, newPassword)

// Zero-browser cross-app silent peer SSO (`issue_peer_code` + PKCE /oauth/token):
GothwadAccounts.signInSilentlyWithPeer(peer)

// Secondary Chrome Custom Tabs OAuth 2.0 + S256 PKCE browser fallback:
GothwadAccounts.signInWithBrowser(activity, chooseAccount = true, loginHint = peer.email ?: peer.username)
GothwadAccounts.signIn(activity)

GothwadAccounts.switchTo(userId)
GothwadAccounts.signOut(userId, revokeOnServer = false)      // Local installation sign-out only (default)
GothwadAccounts.disconnectFromServer(userId)                 // Explicit POST /oauth/revoke + local clear -> DisconnectOutcome
GothwadAccounts.removeFromDevice(userId)                     // Local removal + tombstone + verified peer IPC
GothwadAccounts.refreshProfile()                             // Force /oauth/userinfo refresh (extended profile, security, quota)
GothwadAccounts.syncDiscoveredAccounts()
GothwadAccounts.recoverCorruptedStorage()
GothwadAccounts.manageOnWeb(activity, section = "security")
GothwadAccounts.openWebPage(activity, path = "/privacy")
GothwadAccounts.installedVerifiedPeers()

// First-party `gothwad-accounts-android` management APIs (`account.read` / `account.profile.write`):
GothwadAccounts.listWebSessions()
GothwadAccounts.revokeWebSession(sessionId)
GothwadAccounts.revokeOtherWebSessions()
GothwadAccounts.listActivity()
GothwadAccounts.listAuthorizations()
GothwadAccounts.revokeAuthorization(appId)
GothwadAccounts.updateBasicProfile(firstName, lastName, dateOfBirth, gender)
GothwadAccounts.uploadAvatar(imageDataOrBase64, mime = "image/jpeg")
GothwadAccounts.removeAvatar()
GothwadAccounts.updatePhoneNumber(phoneNumber, currentPassword, mfaCode = null)
GothwadAccounts.requestRecoveryEmailChange(recoveryEmail, currentPassword, mfaCode = null)
GothwadAccounts.verifyRecoveryEmailChange(token)
GothwadAccounts.changePassword(currentPassword, newPassword, mfaCode = null)
GothwadAccounts.getMfaStatus()
GothwadAccounts.beginMfaEnrollment(currentPassword)
GothwadAccounts.confirmMfaEnrollment(currentPassword, enrollmentId, code)
GothwadAccounts.regenerateMfaRecoveryCodes(currentPassword, code)
GothwadAccounts.disableMfa(currentPassword, code)
GothwadAccounts.exportAccountDataJson()
GothwadAccounts.deleteAccount(password, mfaCode = null)
GothwadAccounts.fetchTrustedApps(pinnedPublicKey, expectedKeyId)
```

### Removal scope contract

| Method | Scope | Server sessions & other installs |
| --- | --- | --- |
| `signOut(userId)` (`revokeOnServer = false`) | Clears only this installation's local credential | Other installs and browser sessions unaffected |
| `disconnectFromServer(userId)` | Calls `POST /oauth/revoke` then clears local credential | Revokes entire user/client OAuth grant across all installations of that app (reports `OfflineRemovedLocally` if offline) |
| `removeFromDevice(userId)` | Clears local credential + records removal tombstone + notifies verified sibling apps on this device | Other phones and browser sessions remain active until revoked on web |

## 4. Network authorization (`core`)

```kotlin
val client = OkHttpClient.Builder()
    .addNetworkInterceptor(
        GothwadAccounts.authInterceptor(
            allowedHosts = setOf("drive-api.bansify.com"),
        ),
    )
    .build()
```

- **Must be installed with `addNetworkInterceptor`**, never `addInterceptor`; runtime guard throws if installed as an application interceptor.
- Enforces HTTPS default port `443` and checks **every network redirect hop** against `allowedHosts`. Strips `Authorization` on disallowed targets.
- Proactively refreshes tokens within 60 seconds of expiry using per-account single-flight mutexes.

## 5. Shared UI & embeddable widgets (`ui`)

Full-screen account management (matches `accounts.bansify.com/me` 6 sections: `Home`, `Personal info`, `Security`, `Data & privacy`, `People & sharing`, `Connected apps`):

```kotlin
import com.gothwad.accounts.ui.openAccountManagement

GothwadAccounts.openAccountManagement(activity)
GothwadAccountsScreen(onClose = { /* ... */ })
```

See [DISCOVERY_AND_TOKEN_ARCHITECTURE.md](DISCOVERY_AND_TOKEN_ARCHITECTURE.md) and [BROKER_PROTOCOL.md](BROKER_PROTOCOL.md) for cross-app design invariants.
