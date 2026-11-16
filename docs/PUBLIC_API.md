# Public API

## 1. Initialization (`core`)

Initialize once from `Application.onCreate` (required on every process launch, including callback restoration after process death):

```kotlin
GothwadAccounts.init(
    this,
    GothwadConfig(
        clientId = "gothwad-tube",
        redirectUri = "https://tube.gothwadtech.com/oauth/callback",
        scopes = setOf(Scope.Profile, Scope.Email, Scope.OfflineAccess),
        allowCustomSchemeRedirect = true, // Set false in production apps using verified https App Links
        brokerPolicy = BrokerPolicy.PeerDiscovery(
            trustPolicy = BrokerTrustPolicy.SameSigningCertificate(
                allowDebugBuild = BuildConfig.DEBUG,
            ),
        ),
    ),
)
```

For FOSS or zero-IPC builds, leave `brokerPolicy = BrokerPolicy.Disabled` (the default). Config is immutable per process.

## 2. Three-axis state & flows (`core`)

```kotlin
val state by GothwadAccounts.state.collectAsStateWithLifecycle()
val discovered by GothwadAccounts.discoveredAccounts.collectAsStateWithLifecycle()
val isRefreshing by GothwadAccounts.isRefreshing.collectAsStateWithLifecycle()
val error by GothwadAccounts.error.collectAsStateWithLifecycle()
```

- **`AccountState`** (host-local credentials):
  - `Initializing` — initial encrypted storage load in progress.
  - `SignedOut` — no locally authorized accounts in this app (`discoveredAccounts` may still contain peer accounts from verified sibling apps).
  - `SignedIn(account, all)` — locally authorized account(s) in this app, with `account` selected for this app only.
  - `StorageCorrupted(reason)` — encrypted storage or Android Keystore key became unreadable; fails closed (no plaintext fallback). Recover via `GothwadAccounts.recoverCorruptedStorage()`.
- **`DiscoveredAccount`** (device-wide zero-token directory):
  - Combines local accounts (`isAuthorizedLocally = true`) and verified sibling app metadata (`isAuthorizedLocally = false`, zero tokens).
- **`isRefreshing: StateFlow<Boolean>`**:
  - Separate non-destructive refresh state; active signed-in UI stays visible during `/oauth/userinfo` refreshes.
- **`error: StateFlow<AuthFailure?>`**:
  - Sanitized failures (`BrowserUnavailable`, `InvalidCallback`, `LoginCancelled`, `SessionExpired`, `StorageUnavailable`, `Unavailable`). Dismiss with `clearError()`.

## 3. Lifecycle actions (`core`)

```kotlin
GothwadAccounts.signIn(activity)
GothwadAccounts.signIn(activity, chooseAccount = true)
GothwadAccounts.signIn(activity, loginHint = peer.email ?: peer.username)
GothwadAccounts.switchTo(userId)
GothwadAccounts.signOut(userId, revokeOnServer = true) // Local removal + best-effort /oauth/revoke
GothwadAccounts.removeFromDevice(userId)               // Local removal + tombstone + verified peer IPC + best-effort /oauth/revoke
GothwadAccounts.refreshProfile()         // Force /oauth/userinfo refresh (optional drive quota)
GothwadAccounts.syncDiscoveredAccounts() // Re-query verified peer AccountDirectoryProviders
GothwadAccounts.recoverCorruptedStorage()
GothwadAccounts.manageOnWeb(activity, section = "security") // Opens /me#security in Custom Tabs
GothwadAccounts.openWebPage(activity, path = "/privacy")
GothwadAccounts.installedVerifiedPeers()
```

### Removal scope contract

| Method | Scope | Server sessions |
| --- | --- | --- |
| `signOut(userId)` | Clears only this app's local credential | Unaffected |
| `removeFromDevice(userId)` | Clears local credential + records removal tombstone + notifies verified sibling apps on device | Browser/other-device sessions revoked via `/me#security` |

## 4. Network authorization (`core`)

```kotlin
val client = OkHttpClient.Builder()
    .addNetworkInterceptor(
        GothwadAccounts.authInterceptor(
            allowedHosts = setOf("tube-api.gothwadtech.com"),
        ),
    )
    .build()
```

- **Must be installed with `addNetworkInterceptor`**, never `addInterceptor`; runtime guard throws if installed as an application interceptor.
- Enforces HTTPS default port `443` and checks **every network redirect hop** against `allowedHosts`. Strips `Authorization` on disallowed targets.
- Use a dedicated OkHttp client with HTTP caching disabled (or partitioned per `userId`).
- Proactively refreshes tokens within 60 seconds of expiry. Never call blocking HTTP requests on the Android main thread.

## 5. Shared UI & embeddable widgets (`ui`)

Full-screen account management (matches `accounts.gothwadtech.com/me` 6 sections: `Home`, `Personal info`, `Security`, `Data & privacy`, `People & sharing`, `Connected apps`):

```kotlin
import com.gothwad.accounts.ui.openAccountManagement

GothwadAccounts.openAccountManagement(activity)
// Or render directly in Compose:
GothwadAccountsScreen(onClose = { /* ... */ })
```

Reusable embeddable host components (inherit the caller's `MaterialTheme`):

- `GothwadAccountAvatarButton(accountState, onClick)` — top app bar profile trigger.
- `GothwadAccountSwitcherSheet(...)` — multi-account + peer-discovery switcher dialog with "Continue as...", "Add another account", "Manage your Gothwad Account", and local sign-out.
- `GothwadAccountSettingsCard(...)` — settings screen account summary + sign-in / manage buttons.
- `GothwadAuthStatusBanner(...)` — non-blocking error/offline/session-expired recovery banner.
- `SignInButton(...)` / `GothwadAccountsTheme(...)`.

See [DISCOVERY_AND_TOKEN_ARCHITECTURE.md](DISCOVERY_AND_TOKEN_ARCHITECTURE.md) and [BROKER_PROTOCOL.md](BROKER_PROTOCOL.md) for cross-app design invariants.
