# Account Discovery & Token Issuance Architecture

This document specifies the runtime sequences for first-app login, cross-app verified discovery, per-service token issuance, per-app active selection isolation, and HTTP credential enforcement.

---

## 1. First-Installed App Sign-In Flow (Scenario A)

```text
User (Mail App)          :core (SessionManager)         Custom Tabs Browser         accounts.bansify.com
      |                            |                             |                            |
      |-- Tap "Sign in" ---------->|                             |                            |
      |                            |-- Discover Peers ---------->| (0 peers with accounts)    |
      |                            |-- beginLogin()              |                            |
      |                            |   save encrypted PendingLogin                            |
      |                            |   (state, PKCE verifier)    |                            |
      |                            |-- launch /oauth/authorize ->|                            |
      |                            |   (client_id=gothwad-mail,  |                            |
      |                            |    S256 challenge, scopes)  |--- User signs in (Web/Google|
      |                            |                             |    Passkey/MFA) + consent -->|
      |                            |<-- Redirect to callback ----|<-- ?code=...&state=... ----|
      |<-- AccountCallbackActivity-|                             |                            |
      |                            |-- CallbackValidator (exact  |                            |
      |                            |   scheme/host/path + state) |                            |
      |                            |-- POST /oauth/token ------->|--------------------------->|
      |                            |   (code + code_verifier)    |<-- client-scoped tokens ---|
      |                            |-- GET /oauth/userinfo ----->|--------------------------->|
      |                            |<-- {sub, username, name, email, picture} ----------------|
      |                            |-- Encrypt in Tink Vault     |                            |
      |                            |-- Publish non-secret entry in AccountDirectoryProvider   |
      |<-- SignedIn(A, [A]) -------|                             |                            |
```

---

## 2. Later-Installed App Discovery & Service-Scoped Issuance (Scenarios B, C, D)

```text
Drive App (:core)                    Mail App (AccountDirectoryProvider)      accounts.bansify.com
      |                                            |                                    |
      |-- Startup / Switcher open                  |                                    |
      |-- PackageManager check (<queries> list) -->|                                    |
      |-- Verify Mail package + SHA-256 cert       |                                    |
      |-- ContentResolver.call(METHOD_LIST_ACCOUNTS)                                    |
      |                                            |-- SYNCHRONOUS Binder.getCallingUid |
      |                                            |-- CallerVerifier checks Drive UID  |
      |                                            |   + all UID packages + SHA-256 cert|
      |                                            |-- Read directory metadata ONLY     |
      |<-- [Discovered(A), Discovered(B)] ---------|   (NEVER access/refresh tokens!)   |
      |                                            |                                    |
      |-- User sees "Continue as A" / "Continue as B" / "Add another account"           |
      |-- User picks A in Drive                    |                                    |
      |   (Mail remains active on B!)              |                                    |
      |-- Current mode: Custom Tabs OAuth PKCE --->|----------------------------------->|
      |   (client_id=gothwad-drive, scope=drive,   |                                    |
      |    login_hint=A@bansify.com)               |                                    |
      |<-- Drive-scoped access + refresh tokens ---|<-----------------------------------|
      |                                            |                                    |
      |-- Future Handoff Mode (after server PR):   |                                    |
      |   Drive sends PKCE challenge ------------->|                                    |
      |                                            |-- Server handoff mint (signed proof|
      |                                            |   bound to drive + user A + S256)  |
      |<-- One-time handoff code ------------------|                                    |
      |-- POST /oauth/token (handoff + verifier) ->|----------------------------------->|
```

---

## 3. Authenticated API Client vs. Download / Public Client Separation

```text
Host App Network Layer
  |-- apiClient (OkHttpClient)
  |     |-- .addNetworkInterceptor(GothwadAccounts.authInterceptor(allowedHosts = setOf("drive-api.bansify.com")))
  |     |-- .cache(null)  // or explicitly purged/partitioned per activeUserId
  |     |-- Enforces HTTPS + port 443 + allowedHosts at EVERY redirect hop
  |     '-- Single-flight per-userId refresh lock; never leaks Authorization across 302 redirects
  |
  '-- downloadClient (OkHttpClient)
        |-- NO authInterceptor (never touches SessionManager mutexes or attaches OAuth Bearer)
        '-- Used for CDN/APK/public media/presigned object URLs cleanly
```

---

## 4. Three-State Isolation Model

| State Layer | Storage Location | Visibility Across Apps | Switching Mail to B? |
| --- | --- | --- | --- |
| **1. Available Device Accounts** (`DiscoveredAccount`) | Each installed app's verified `AccountDirectoryProvider` + encrypted directory store | Shared among mutually cert-verified first-party apps (disabled when `BrokerPolicy.Disabled`) | Mail & Drive both see `{A, B}` (and `{C}` once added by Drive) |
| **2. Per-App Selected Account** (`activeUserId`) | Strictly inside each app's own `EncryptedTokenStore` (`noBackupFilesDir`) | **Never shared** | Mail updates `Mail.activeUserId = B`; Drive stays untouched on `A` |
| **3. Per-Service Authorization** (`Credential`) | Strictly inside each app's own `EncryptedTokenStore` (`clientId` + `redirectUri` bound) | **Never shared** | Mail refreshes `aud=gothwad-mail`; Drive refreshes `aud=gothwad-drive` |
