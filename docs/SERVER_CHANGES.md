# Accounts & Ecosystem Backend Contract Changes (Separate Reviewed PR Plan)

**Scope boundary**: No backend files in `GothwadTech/accounts`, `mail`, `drive`, or `store` are altered by `accountsapp`. All production deployments and schema migrations require explicit owner approval in their respective repositories.

---

## 1. Live `accounts` Backend Baseline (Cloudflare Worker + D1)

Verified from `GothwadTech/accounts`:
- **Database**: 100% Cloudflare D1 + R2 (`DEV_MODE = "false"`, `AUTH_HUB_URL = "https://accounts.gothwadtech.com"`, `APP_DOMAIN = "gothwadtech.com"`).
- **OAuth 2.0**: `/oauth/authorize`, `/oauth/token` (S256 PKCE mandatory for public clients; HS256 `oauth_access` 1h + rotating `offline_access` refresh 30d), `/oauth/userinfo` (checks live `oauth_credential_bindings` table).
- **Management (`/auth/*` & `/oauth/authorizations` & `/oauth/revoke`)**: Currently require `token_use === 'session'` cookies (not OAuth `oauth_access` tokens). `/auth/signin` strips JSON tokens unless `X-Gothwad-Proxy-Token` is sent server-side without browser headers.

---

## 2. Required Backend PRs (Ordered Rollout)

### PR 1 — `accounts`: Android Client Registrations & App Links (Required for Live Login)
Register or update public OAuth clients in D1 `ecosystem_apps` (zero APK `client_secret`) with exact redirects and allowed scopes:

| App / Host | `client_id` | Allowed `redirect_uris` | Scopes |
| --- | --- | --- | --- |
| Accounts Android (`:app`) | `gothwad-accounts-android` | `https://accounts.gothwadtech.com/android/callback` (App Link), `com.gothwad.accounts:/oauth/callback` | `profile email offline_access` |
| Sample Alpha (`:sample:alpha`) | `gothwad-accounts-sample-alpha` | `com.gothwad.accounts.sample.alpha:/oauth/callback` | `profile email offline_access` |
| Sample Beta (`:sample:beta`) | `gothwad-accounts-sample-beta` | `com.gothwad.accounts.sample.beta:/oauth/callback` | `profile email offline_access` |
| Gothwad Mail (`mailapp`) | `gothwad-mail` | `https://mail.gothwadtech.com/oauth/callback`, `com.gothwad.mail:/oauth/callback` | `profile email mail offline_access` |
| Gothwad Drive (`drive`) | `gothwad-drive` | `https://drive.gothwadtech.com/oauth/callback`, `com.gothwad.drive:/oauth/callback` | `profile email drive drive.read offline_access` |
| Kiwi Store (`storeapp`) | `kiwi-store` | Retain `kiwistore://oauth/callback` + `https://kiwistore.gothwadtech.com/oauth/callback`, `com.gothwad.store:/oauth/callback` | `profile email offline_access` |
| PlusTube (`tubeapp`) | `gothwad-tube` | `https://tube.gothwadtech.com/oauth/callback`, `com.gothwad.tube:/oauth/callback` | `profile email offline_access` |

Also publish `/.well-known/assetlinks.json` with production SHA-256 signing fingerprints on every App Link host.

### PR 2 — `accounts`: Native Token Revocation, Session Listing & Scoped `/me` APIs
1. Allow native OAuth bearer revocation (`POST /oauth/revoke` with valid `oauth_access` or `refresh_token`, bound by `client_id` + `user_id`), and optional explicit `device_wide=true` revocation with proper reauthentication/scope.
2. Add explicit management scopes (e.g. `account.read`, `account.profile.write`) gated by consent/first-party classification so native `:ui` screens can load/edit profile photo, DOB/gender, device sessions (`/auth/sessions`), recent activity (`/auth/activity`), and connected grants (`/oauth/authorizations`) directly without forcing Custom Tabs.
3. Sensitive actions (password, MFA, recovery email, delete account) must always require fresh credential/MFA reauthentication.

### PR 3 — `accounts`: Signed Trusted-App Registry & Server-Signed Cross-App Handoff
1. Add `first_party boolean NOT NULL DEFAULT false` and `android_cert_sha256` allowlist in `ecosystem_apps`.
2. Serve signed public `GET /api/apps/trusted` (`version`, `expires_at`, `signature`, pinned Ed25519/ECDSA verification key in SDK, anti-rollback check).
3. Add single-use, challenge-bound handoff exchange (verified proof that caller owns trusted installation -> one-time code bound to target `client_id`, `user_id`, `code_challenge`, and allowed scopes; source refresh token never leaves source app).

### PR 4 — Resource Workers (`mail`, `drive`, `store`) Audience & Scope Enforcement
1. **`mail/worker/src/session.ts`**: Verify JWT/userinfo `aud == "gothwad-mail"` (or verified mail-authorized client) and require `mail` scope — never accept generic `kiwi-store` or basic `profile` tokens as Mail authorization.
2. **`drive/worker/src/auth.ts`**: Accept `Authorization: Bearer` OAuth access tokens (verifying `aud == "gothwad-drive"` and `drive` / `drive.read` scopes cleanly, distinguishing `drive.read` read-only vs `drive` read-write) alongside web session cookies.
3. **`store/worker/src/auth.ts`**: Verify `aud == "kiwi-store"` and distinguish `401` invalid token from `503` Accounts outage.
