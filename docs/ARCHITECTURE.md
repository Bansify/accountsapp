# Bansify Multi-App Account Architecture Proposal

**Status**: Architecture & SDK Implementation Baseline (`accountsapp` branch `arena/926fcc7f-accountsapp`)
**Backend Source of Truth**: `Bansify/accounts` (`100% Cloudflare Worker + D1 + R2`, zero Supabase)
**Target Ecosystem**: `accountsapp` (`com.bansify.accounts`), `mailapp` (`com.gothwad.mail`), `drive` (`com.gothwad.drive`), `storeapp` (`com.gothwad.store`), `tubeapp` (`com.gothwad.tube`), and side-by-side verification hosts (`sample:alpha`, `sample:beta`).

---

## 1. Design Goals & Non-Negotiable Principles

1. **Zero Mandatory Hub App / Zero Play Services**: Every Bansify app must install first, last, or alone and remain fully functional without Google Play Services, without a preinstalled system component, and without requiring `com.bansify.accounts` ("Accounts") to be installed.
2. **Three Strictly Separated State Axes**:
   - **Device-Available Accounts (`DiscoveredAccount`)**: Identity-only directory of accounts signed in on installed, cryptographically verified Bansify apps on this device (`userId`, `username`, `name`, `email`, `avatarUrl`, `addedAt`, `lastSeenAt`). Contains **zero tokens**.
   - **App-Local Active Account (`activeUserId`)**: Strictly private to each app's encrypted store. Selecting Account B in `mailapp` **never** switches an already-open `drive` or `storeapp` from Account A to Account B.
   - **Service-Scoped OAuth Grants & Tokens (`Credential`)**: Every app owns its own client-scoped `access_token` (`aud == client_id`) and rotating `refresh_token` (`offline_access`) stored in its own Android Keystore-backed Tink AEAD file inside `noBackupFilesDir`. Refresh tokens are **never** shared or exported across apps.
3. **Existing Accounts Web UI Parity — No Unimplemented Demo Bloat**:
   - Sign-in, signup, Google sign-in linking, passwordless sign-in, passkey registration, TOTP 2-Step Verification, and account recovery reuse `accounts.bansify.com` over **Custom Tabs + Authorization Code + PKCE (`S256`)** — never an embedded third-party WebView, never collecting Google passwords, and never copying browser cookies.
   - Native Compose UI in `:ui` mirrors only the six real sections present on `accounts.bansify.com/me`: **Home**, **Personal info**, **Security**, **Data & privacy**, **People & sharing**, and **Connected apps** (removing unimplemented demo tabs like Payments & Subscriptions, Timeline/YouTube History, Password Manager, and hardcoded demo users).

---

## 2. Empirical Rejection of Multi-Authenticator `AccountManager`

Android's `android.accounts.AccountManager` is structurally incompatible with independently installable, equally privileged first-party apps:

1. **Single-Owner `RegisteredServicesCache`**: `AccountManagerService` indexes authenticators by `android:accountType` (e.g. `com.gothwad.account`) and binds at most **one** winning `AbstractAccountAuthenticator` package per `accountType`. If `mailapp`, `drive`, `storeapp`, and `accountsapp` all declare authenticators for the same `accountType`, Android selects one winner and ignores the rest—there is **no multi-authenticator failover**.
2. **Catastrophic Uninstall Wipe (`onPackagesRemoved`)**: When the user uninstalls whichever app happened to own that `accountType` (for example, `mailapp` was installed first and later uninstalled while `drive` and `storeapp` are still actively used), `AccountManagerService` removes **all accounts** of that `accountType` across the device.
3. **UID / Signature Coupling & Cross-Signer Failures**: Calling `addAccountExplicitly()`, `peekAuthToken()`, or `invalidateAuthToken()` from another package requires either matching authenticator UID or `AccountManager` authenticator signature rules. Separately signed FOSS/debug/legacy builds fail with `SecurityException` or lose access silently.

**Executable Proof**: Tested empirically in `CrossAppBrokerAndAccountManagerProofTest` (`:core` unit/Robolectric suite).

---

## 3. Recommended Architecture: Distributed Verified Peer Directory + Per-App Encrypted Vault

```text
+-----------------------------------------------------------------------------+
|                        Android Device (No Play Services)                    |
|                                                                             |
|  +-----------------------+    +-----------------------+    +--------------+ |
|  | Bansify Mail          |    | Bansify Drive         |    | Kiwi Store   | |
|  | com.gothwad.mail      |    | com.gothwad.drive     |    | com.gothwad  | |
|  |                       |    |                       |    | .store       | |
|  | Vault (Keystore AEAD) |    | Vault (Keystore AEAD) |    | Vault (AEAD) | |
|  |  - Active: Account B  |    |  - Active: Account A  |    |  - Active: A | |
|  |  - Tokens: mail scope |    |  - Tokens: drive scope|    |  - Tokens:   | |
|  |                       |    |                       |    |   store scope| |
|  | AccountDirectoryProv. |<==>| AccountDirectoryProv. |<==>| AccountDir.  | |
|  | (Binder UID + Cert    |    | (Binder UID + Cert    |    | (Binder UID  | |
|  |  verified; NO tokens) |    |  verified; NO tokens) |    |  + Cert)     | |
|  +-----------+-----------+    +-----------+-----------+    +------+-------+ |
|              |                            |                       |         |
|              +-------------+--------------+-----------------------+         |
|                            |  Custom Tabs + S256 PKCE                       |
|                            |  (+ Future Signed Server Handoff)              |
+----------------------------|------------------------------------------------+
                             v
+-----------------------------------------------------------------------------+
|            accounts.bansify.com (Cloudflare Worker + D1 + R2)             |
|  - /oauth/authorize, /oauth/token (PKCE S256, rotating refresh tokens)      |
|  - /oauth/userinfo (oauth_credential_bindings live DB check)                |
|  - /auth/* (cookie-authenticated web session management & MFA/Passkeys)     |
+-----------------------------------------------------------------------------+
```

### How First-Installed-App & Scenarios A–E Work

- **Scenario A (Mail installed first, no Accounts app)**: Mail sees zero verified peers with accounts. User signs into Account A and Account B via Custom Tabs PKCE (`client_id=gothwad-mail`). Mail stores `{A, B}` tokens in Mail's encrypted vault, sets `activeUserId`, and exposes non-secret directory records `{A, B}` via `com.gothwad.mail.gothwad.account.directory` (`AccountDirectoryProvider`).
- **Scenario B (Drive installed later, no Accounts app)**: On launch, Drive queries installed known peers via `PackageManager` (`<queries>` allowlist, no `QUERY_ALL_PACKAGES`), verifies Mail's signing certificate, and Mail verifies Drive's Binder UID + package + SHA-256 certificate before returning `{A, B}` directory metadata. Drive renders **"Continue as A"**, **"Continue as B"**, or **"Add another account"**. Tapping **"Continue as A"** runs PKCE `/oauth/authorize` for `client_id=gothwad-drive` (`drive` scope, leveraging browser SSO if present or clean login without copying cookies) to issue Drive's own tokens. Mail stays independently selected on **B**!
- **Scenario C (Drive adds Account C)**: Drive signs in Account C, stores C's tokens in Drive's vault (`Drive.activeUserId = C`), and advertises C in Drive's `AccountDirectoryProvider`. When Mail refreshes peer discovery, Mail learns `{A, B, C}` while remaining active on **B**.
- **Scenario D (Accounts app installed later)**: `com.bansify.accounts` queries installed verified peers (`mailapp`, `drive`, `storeapp`), adopts the union `{A, B, C}` in its device-available list immediately, and never overwrites or wipes Mail's or Drive's local state.
- **Scenario E (Mail uninstalled; or Store/Mail/Drive running alone)**: Uninstalling Mail removes only Mail's app sandbox. Drive, Store, and Accounts continue holding their own tokens and sharing directory metadata with each other without interruption.

---

## 4. Module Boundaries in `Bansify/accountsapp`

| Module | Package / App ID | Responsibility |
| --- | --- | --- |
| `:core` | `com.bansify.accounts.core` | Headless SDK (zero Compose): `GothwadConfig`, `GothwadAccounts`, `SessionManager`, `EncryptedTokenStore`, `OAuthClient`, `CallbackValidator`, `Pkce`, `AccountDirectoryProvider`, `CallerVerifier`, `TrustRegistry`, `PeerBrokerClient`, OkHttp `authInterceptor`. |
| `:ui` | `com.bansify.accounts.ui` | Shared M3 Compose account management experience (`GothwadAccountsScreen`, 6 web-aligned tabs) + embeddable host widgets (`GothwadAccountAvatarButton`, `GothwadAccountSwitcherSheet`, `GothwadAccountSettingsCard`, `GothwadAuthStatusBanner`) + EN/HI strings. |
| `:app` | `com.bansify.accounts` | Optional standalone Bansify Accounts Android app hosting `GothwadAccountsScreen` cleanly. |
| `:sample` | `com.bansify.accounts.sample.{alpha,beta}` | Side-by-side multi-app verification hosts testing independent active accounts, embedded toolbar widgets, and peer discovery. |

---

## 5. Cross-Repo Audit & Required Backend-Contract Boundaries

Based on direct inspection of `Bansify/accounts`, `mail`, `mailapp`, `drive`, `store`, `storeapp`, and `tubeapp` (no external repo was modified or deployed):

1. **`accounts` (`cloudflare-worker` + D1)**:
   - Public OAuth PKCE (`/oauth/authorize`, `/oauth/token`, `/oauth/userinfo`) is live and validates `oauth_credential_bindings` against active password hashes and grants.
   - `ecosystem_apps` currently seeds `kiwi-store` with app redirects, while `gothwad-mail`, `gothwad-drive`, and `gothwad-tube` have `redirect_uris = '[]'`, and `gothwad-accounts-android` / sample clients require registration (see [SERVER_CHANGES.md](SERVER_CHANGES.md)).
   - `/oauth/authorizations`, `/oauth/revoke`, and `/auth/*` management routes (`update-profile`, `sessions`, `activity`, `mfa`, `passkeys`, `google`, `export`, `delete-account`) require `token_use === 'session'` cookies today; therefore `:ui` routes sensitive mutations cleanly to `accounts.bansify.com/me` in Custom Tabs until scoped native management endpoints are approved and added to `accounts`.
2. **Sibling Resource Workers (`mail`, `drive`, `store`)**:
   - Resource servers **must** validate both OAuth `aud == <expected_service_client_id>` and required service scopes (`mail`, `drive` / `drive.read`, etc.), never treating `/oauth/userinfo` identity success alone as cross-service authorization.
   - `drive/worker/src/auth.ts` must accept scoped OAuth Bearer tokens alongside web cookie sessions; `mail/worker/src/session.ts` and `store/worker/src/auth.ts` must verify client audience and scope.
3. **Sibling Android Apps (`mailapp`, `storeapp`)**:
   - `storeapp` currently mixes `StoreNetwork.downloadClient` with `authInterceptor`, attaches Bearer tokens broadly, and stores tokens in plaintext `SharedPreferences`; adopting `:core` fixes redirect token leakage, mutex deadlocks, and plaintext storage.
   - `mailapp` is currently a local Room seed prototype with no `INTERNET` permission and release `debug.keystore`; it should integrate `:core` + `:ui` on a proper release key when wired to `mail`.
