# Threat Model & Security Requirements

This document defines the STRIDE threat model and enforced security controls for `GothwadTech/accountsapp` and integrating Gothwad Android clients.

---

## 1. Security Invariants (Non-Negotiable)

| Threat Category | Attack Vector | Enforced Mitigation |
| --- | --- | --- |
| **S — Spoofing (Caller App)** | Rogue APK on device mimics a trusted package name via `Intent.getStringExtra` or tries to query `AccountDirectoryProvider` for account metadata. | `CallerVerifier` captures `Binder.getCallingUid()` **synchronously** before async dispatch, resolves **all** packages for that UID (`PackageManager.getPackagesForUid`), rejects mixed-UID packages unless trusted, and validates release SHA-256 certificate digests via `SigningInfo.hasSigningCertificate` (API 28+) / `GET_SIGNATURES` (API 26–27). Never trusts `Intent` caller claims. |
| **S — Spoofing (Callback / Scheme Hijack)** | Malicious app registers the same custom scheme `com.gothwad.mail:/oauth/callback` to intercept an OAuth authorization `code`. | 1. Prefer verified HTTPS App Links (`android:autoVerify="true"`, `.well-known/assetlinks.json`).<br>2. **S256 PKCE** is mandatory for all public clients (`accounts` Worker rejects non-S256 requests): an intercepted `code` is useless without the encrypted `code_verifier` stored inside the legitimate app's `PendingLogin`.<br>3. `CallbackValidator` enforces exact scheme/authority/path, duplicate-key rejection, `code xor error`, constant-time `state` match, and 10-minute single-use expiration. |
| **T — Tampering (Encrypted Vault & Backups)** | Rooted backup extraction, ADB backup (`allowBackup`), or ciphertext swapping across apps/endpoints. | All tokens live inside `context.noBackupFilesDir` (`AtomicFile`), encrypted with Tink AEAD backed by Android Keystore (`android-keystore://...`) using AAD binding `"$packageName|$clientId|$authBaseUrl|$redirectUri"`. `android:allowBackup="false"` in manifests. Keystore corruption fails closed into `AccountState.StorageCorrupted` (zero plaintext fallback). |
| **T — Tampering (Shared `accountType` Collision)** | Malicious or second app declares the same `AccountManager` `android:accountType` to hijack authenticator ownership or cause uninstall-wipe denial of service. | We **reject** multi-authenticator shared `AccountManager` registration. Each app owns its isolated `TokenStore` and host-scoped `ContentProvider` authority (`${applicationId}.gothwad.account.directory`), with mutual certificate verification before IPC. |
| **I — Information Disclosure (Token Leakage)** | Bearer header leaked across HTTP 302 redirects, shared OkHttp caches, download clients, world-readable files, or cross-app IPC. | 1. `GothwadAccounts.authInterceptor` **requires** `addNetworkInterceptor` (checked at runtime via `chain.connection() != null`), enforcing `https`, port `443`, and explicit `allowedHosts` at **every redirect hop**, stripping `Authorization` on disallowed targets.<br>2. Separate `downloadClient` must never attach `authInterceptor`.<br>3. `AccountDirectoryProvider` stores & returns **metadata only** — `access_token` and `refresh_token` never cross IPC.<br>4. HTTP caches are disabled (`null`) or cleared/partitioned per `userId`. |
| **I — Information Disclosure (FOSS / Untrusted Mode)** | Unverified or FOSS build queries peer directory or leaks device accounts. | `BrokerPolicy.Disabled` disables both peer queries and the exported `AccountDirectoryProvider` completely (fails with `SecurityException` before touching disk). |
| **E — Elevation of Privilege (Service & Management Confusion)** | Treating `/oauth/userinfo` identity confirmation as authorization for `mail` or `drive`, or mutating password/MFA with basic identity scopes. | 1. Resource Workers (`mail`, `drive`, `store`) must independently enforce `aud == client_id` and service scopes (`mail`, `drive`, etc.).<br>2. High-privilege `/auth/*` mutations (password, MFA, passkeys, recovery email, delete account) require reauthenticated web sessions on `accounts.gothwadtech.com/me` via Custom Tabs until explicit scoped native APIs pass backend security review. |
| **R / D — Replay, Race & DoS** | Concurrent API calls triggering duplicate refresh token rotation (causing family revocation), or slow refresh resurrecting a signed-out account. | 1. `SessionManager` enforces per-`userId` `refreshLocks` so 20+ concurrent callers rotate once.<br>2. Monotonic `generation` epoch + `original` credential check prevents late refresh from writing back a signed-out account.<br>3. Transient `5xx`/`IOException` preserves session offline; only `400..499` `error == "invalid_grant"` clears dead grants.<br>4. IPC callers have strict payload limits and non-blocking bounded dispatch. |

---

## 2. Explicit Prohibited Patterns

- **Never** use `android:sharedUserId` across apps.
- **Never** use `MODE_WORLD_READABLE`, external storage, or unencrypted `SharedPreferences` for tokens or session state.
- **Never** send tokens, auth codes, or user session identifiers inside Android `Broadcast` intents (`sendBroadcast`).
- **Never** collect Google passwords or render third-party Google/Gothwad login inside an embedded `WebView`.
- **Never** copy cookies out of Chrome/Custom Tabs or place tokens into redirect URLs.
- **Never** add debug keystore SHA-256 fingerprints to production `TrustRegistry` lists.
- **Never** embed OAuth client secrets or backend JWT signing keys inside APKs or SDK libraries.
