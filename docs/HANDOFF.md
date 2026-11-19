# Bansify Accounts — Project Status & Handoff

Updated 2026-11-16. Strictly separates what is implemented in source, verified locally via static checks, and pending live Android CI / device verification.

## 1. Current Architecture & Implementation Status

### Implemented in this repository

- **`:core` (`com.gothwad.accounts.core`, Compose-free)**:
  - `GothwadConfig`, `Scope` (`profile`, `email`, `offline_access`, `drive`, `drive.read`, `notes`, `chat`), `BrokerPolicy`, `BrokerTrustPolicy`.
  - Three-axis reactive state: `state: StateFlow<AccountState>` (`Initializing`, `SignedOut`, `SignedIn(account, all)`, `StorageCorrupted(reason)`), `discoveredAccounts: StateFlow<List<DiscoveredAccount>>`, `isRefreshing: StateFlow<Boolean>`, `error: StateFlow<AuthFailure?>`.
  - `OAuthClient` with `/oauth/userinfo` parsing including `updated_at` and optional `drive` quota (`used_bytes`, `limit_bytes`), `followRedirects(false)`, `followSslRedirects(false)`, `retryOnConnectionFailure(false)`.
  - `EncryptedTokenStore` (Tink + AndroidKeyStore under `noBackupFilesDir`, strict fail-closed on crypto failure — zero plaintext fallback) with `addedAt`, `lastSeenAt`, and `deviceRemovalTombstones`.
  - `SessionManager` with single-flight per-account token refresh mutex, non-resurrection guarantees on logout during in-flight refresh, `refreshActiveProfile()`, `removeFromDevice()`, and `recoverCorruptedStorage()`.
  - `broker/` package (`BrokerModels`, `TrustVerifier`, `AccountDirectoryProvider`, `PeerBrokerClient`) implementing zero-token distributed cross-app discovery and device-removal tombstones verified by `Binder.getCallingUid()` + `PackageManager` UID & signing-certificate digests (rejecting mixed-UID packages and non-matching signers before any storage read).
  - Hardened `authInterceptor` requiring `addNetworkInterceptor` and verifying HTTPS port 443 + `allowedHosts` across every network redirect hop.

- **`:ui` (`com.gothwad.accounts.ui`, Material 3)**:
  - Exact 6-tab parity with `accounts.bansify.com/me` (`Home`, `Personal info`, `Security`, `Data & privacy`, `People & sharing`, `Connected apps`), backed 100% by real `:core` flows — demo profiles, hardcoded avatars, and invented tabs removed.
  - Real `AvatarImageLoader` with bounded LRU memory cache and graceful initials fallback.
  - Signed-out peer continuation cards (`DiscoveredAccount` -> `"Continue as ..."`), `StorageCorruptedRecoveryScreen`, explicit app-local sign-out vs device-wide removal dialog, `SearchAccountScreen`, and `HelpLegalScreen`.
  - Reusable host embeddable widgets: `GothwadAccountAvatarButton`, `GothwadAccountSwitcherSheet`, `GothwadAccountSettingsCard`, `GothwadAuthStatusBanner`, `SignInButton`, and `openAccountManagement(activity)`.
  - Matching English (`values`) and Hindi (`values-hi`) string resources.

- **`:app` (`com.gothwad.accounts`) & `:sample` (`alpha` / `beta`)**:
  - `:app` hosts standalone `GothwadAccountsScreen` with `SameSigningCertificate(allowDebugBuild = BuildConfig.DEBUG)`.
  - `:sample` demonstrates real sibling-app embedding (`GothwadAccountAvatarButton`, `GothwadAccountSettingsCard`, `GothwadAccountSwitcherSheet`, `GothwadAuthStatusBanner`, `openAccountManagement`) across two independently installable flavors (`alpha` and `beta`).

- **Tests & CI**:
  - `:core` tests: `PkceAndCallbackTest.kt`, `SessionManagerTest.kt`, `OAuthClientSecurityTest.kt` (HTTPS `MockWebServer` + `okhttp-tls`), `CrossAppBrokerAndAccountManagerProofTest.kt` (Robolectric 4.14.1 proof of Android `AccountManager` single-authenticator conflict + real `AccountDirectoryProvider` Binder/UID/cert enforcement + Scenarios A–E end-to-end).
  - `:ui` tests: `AccountDashboardViewModelTest.kt`.
  - `.github/workflows/android-ci.yml`: fixed SDK 36 provisioning directly from preinstalled `$ANDROID_HOME` (removed failing `setup-android@v3`), executable `gradlew` (`100755`), unit tests across `:core` and `:ui`, lint across all modules/flavors, debug APK builds, local Maven publication check, and debug APK + report artifact uploads.

### Verification status

- **Checked locally**: `python3 tools/check_docs.py` and `python3 tools/check_android_scaffold.py`.
- **Android CI / Gradle execution**: Verify on GitHub Actions `Android CI` workflow run after push (sandbox lacks full local Android SDK).
- **Separate repository follow-ups** (see [SERVER_CHANGES.md](SERVER_CHANGES.md)):
  - Register Android public OAuth clients (`gothwad-accounts-android`, `gothwad-accounts-sample-alpha`, `gothwad-accounts-sample-beta`, `kiwi-store`, `gothwad-mail`, `gothwad-drive`, `gothwad-tube`) in `Bansify/accounts`.
  - Add `/oauth/revoke` bearer-token endpoint + `POST /oauth/token` refresh token revocation check + optional device-scoped session metadata in `Bansify/accounts` (`PR-A` / `PR-B`).
  - Once live OAuth clients are registered, exercise physical-device login, cross-flavor `alpha` <-> `beta` discovery, and signed release workflows (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

## 2. Documentation map

- [ARCHITECTURE.md](ARCHITECTURE.md) — ecosystem roles, architecture diagram, constraints.
- [DISCOVERY_AND_TOKEN_ARCHITECTURE.md](DISCOVERY_AND_TOKEN_ARCHITECTURE.md) — why Android `AccountManager` was rejected and how zero-token distributed discovery + per-app tokens work.
- [UNINSTALL_AND_RECOVERY_MATRIX.md](UNINSTALL_AND_RECOVERY_MATRIX.md) — install order, uninstall survival, keystore loss, and token revocation behavior.
- [SECURITY.md](SECURITY.md) — STRIDE threat model and cryptographic invariants.
- [BROKER_PROTOCOL.md](BROKER_PROTOCOL.md) — Peer Directory IPC v1 specification.
- [SERVER_CHANGES.md](SERVER_CHANGES.md) — exact PR plan for `Bansify/accounts`.
- [PUBLIC_API.md](PUBLIC_API.md) — Kotlin/Compose integration API.
- [INTEGRATION.md](INTEGRATION.md) — manifest setup, client table, and verification steps.
- [RELEASES.md](RELEASES.md) — GitHub Releases, JitPack coordinates, and signing secrets checklist.
