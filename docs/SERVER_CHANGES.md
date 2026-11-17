# Accounts & Ecosystem Backend Status (Aligned with `GothwadTech/accounts`)

**Scope boundary**: No backend files in `GothwadTech/accounts`, `mail`, `drive`, or `store` are altered from `accountsapp`.

---

## 1. Landed `GothwadTech/accounts` Contract (`migrations/0014_first_party_native.sql` & `docs/NATIVE_ACCOUNTS.md`)

Verified directly against `GothwadTech/accounts`:
- **First-party OAuth clients (`0014_first_party_native.sql`)**:
  - `gothwad-accounts-android` (`com.gothwad.accounts`) — `https://accounts.gothwadtech.com/oauth/callback`, `com.gothwad.accounts:/oauth/callback`, scopes `profile email offline_access drive.read account.read account.profile.write`.
  - `gothwad-accounts-sample-alpha` / `gothwad-accounts-sample-beta` — custom scheme callbacks, `profile email offline_access`.
  - `gothwad-mail`, `gothwad-drive`, `kiwi-store`, `gothwad-tube` — exact package/redirect/scope registrations.
- **Extended `GET /oauth/userinfo`**:
  - Returns `given_name`, `family_name`, `date_of_birth`, `gender`, `phone_number`, `phone_number_verified`, `recovery_email`, `recovery_email_verified`, `security` (`has_password`, `password_changed_at`, `mfa_enabled`, `passkey_count`, `authorization_count`, `session_count`), and `drive` (`used_bytes`, `limit_bytes`, `write_access`) gated by granted scopes.
- **Native `POST /oauth/revoke` (`routes/oauth-native-revoke.ts`)**:
  - Requires `Authorization: Bearer <access_token>` plus form `client_id`, `token`, optional `token_type_hint` (`refresh_token` / `access_token`), revoking the entire user/client grant.
- **Direct Native Authentication & Zero-Browser Peer SSO (`routes/mobile-auth.ts`)**:
  - `POST /mobile/auth/check-identifier`, `POST /mobile/auth/check-username`
  - `POST /mobile/auth/signin` (`200` tokens or `202` `mfa_required`), `POST /mobile/auth/signin/mfa`
  - `POST /mobile/auth/signup`, `POST /mobile/auth/forgot-password`, `POST /mobile/auth/reset-password`
  - `POST /mobile/auth/peer-code` (Bearer-authenticated first-party peer PKCE code minting)
- **First-party `/mobile/*` endpoints (`gothwad-accounts-android` audience only, `routes/mobile.ts` & `routes/mobile-security.ts`)**:
  - `GET /mobile/sessions` (`account.read`), `POST /mobile/sessions/revoke`, `POST /mobile/sessions/revoke-others` (`account.profile.write`)
  - `GET /mobile/activity` (`account.read`), `GET /mobile/authorizations` (`account.read`), `POST /mobile/authorizations/revoke` (`account.profile.write`)
  - `PATCH /mobile/profile` (`first_name`, `last_name`, `date_of_birth`, `gender`; `account.profile.write`)
  - `POST /mobile/avatar` & `DELETE /mobile/avatar`
  - `POST /mobile/phone`, `POST /mobile/recovery-email/request`, `POST /mobile/recovery-email/verify`, `POST /mobile/password`
  - `GET /mobile/mfa/status`, `POST /mobile/mfa/enroll`, `POST /mobile/mfa/confirm`, `POST /mobile/mfa/recovery-codes`, `POST /mobile/mfa/disable`
  - `GET /mobile/export`, `POST /mobile/delete-account`
- **Discovery & App Links**:
  - `GET /.well-known/assetlinks.json` (served by Worker from `ANDROID_ASSETLINKS_JSON` or DB fingerprints).
  - `GET /api/apps/trusted` (returns `signed: false` until `TRUSTED_APPS_SIGNING_JWK` + `TRUSTED_APPS_KEY_ID` are configured; unsigned responses must never authorize brokers).

---

## 2. Remaining Production Deployment / Ops Prerequisites (Owner Action)

1. **Accounts Cloudflare routing & fingerprints**:
   - Route exact public `https://accounts.gothwadtech.com/.well-known/assetlinks.json` directly to the `accounts` Worker (outside `/api/*`, no auth/redirects).
   - Apply `migrations/0014_first_party_native.sql` (if not already applied remotely) and populate lowercase 64-hex `android_cert_sha256` fingerprints for `com.gothwad.accounts` (and sibling Android apps) in `ecosystem_apps` once the release `.jks` key is finalized.
   - Optionally configure `TRUSTED_APPS_SIGNING_JWK` (`EdDSA`/`ES256`) + `TRUSTED_APPS_KEY_ID` if consumers verify `/api/apps/trusted` signatures.
2. **Resource Workers (`mail`, `drive`, `store`)**:
   - Ensure each sibling service Worker validates JWT `aud` + client scopes (`mail` for Mail, `drive` / `drive.read` for Drive, `kiwi-store` for Store) before accepting OAuth bearer tokens directly.
