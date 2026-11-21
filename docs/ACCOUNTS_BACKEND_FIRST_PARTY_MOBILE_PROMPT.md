# Bansify Accounts Backend (`Bansify/accounts`): Allow First-Party Apps to Manage Accounts Natively Prompt

Paste the prompt below into the coding agent working in **`Bansify/accounts`**.
Do not run this prompt inside `Bansify/accountsapp` or `Bansify/mailapp`.

---

Work only in this `Bansify/accounts` repository on the active session branch. First read `cloudflare-worker/src/first-party.ts`, `cloudflare-worker/src/routes/mobile.ts`, `cloudflare-worker/src/routes/mobile-security.ts`, `cloudflare-worker/src/routes/oauth.ts`, `cloudflare-worker/src/oauth-access.ts`, `cloudflare-worker/src/native-db.ts`, `migrations/0014_first_party_native.sql`, `tools/generate-first-party-sql.mjs`, `tests/first-party-oauth.test.mjs`, `tests/mobile-oauth.test.mjs`, and `tests/native-bootstrap-isolation.test.mjs` before editing.

## Why `Bansify/accounts` Needs This Update

The shared Android SDK (`Bansify/accountsapp` `:core` + `:ui`) now embeds the full Bansify Sign-In / Sign-Up / MFA / Peer-SSO flow **and** the full in-app Bansify Account Centre (`GothwadAccounts.openAccountManagement`) directly inside every first-party Android app (`Bansify Mail`, `Bansify Drive`, `Kiwi Store`, `Bansify Tube`), even when the standalone Bansify Accounts APK is not installed.

Previously, `Bansify/accounts`:
1. Only granted `"account.read"` and `"account.profile.write"` to `gothwad-accounts-android` in `officialClients()` (`cloudflare-worker/src/first-party.ts`) and `migrations/0014_first_party_native.sql`.
2. Hard-locked `/api/mobile/*` (`cloudflare-worker/src/routes/mobile.ts:37`) to `ctx.client.id !== ACCOUNTS_ANDROID || ctx.payload.aud !== ACCOUNTS_ANDROID`, returning `403 insufficient_scope` even if another official first-party Android app holds valid `"account.read"` / `"account.profile.write"` scopes.
3. Hardcoded `'From Accounts Android'` in activity log details across `cloudflare-worker/src/routes/mobile.ts` and `cloudflare-worker/src/routes/mobile-security.ts`.

---

## Mandatory Security & Compatibility Invariants

1. **Keep `gothwad-accounts-sample-alpha` and `gothwad-accounts-sample-beta` UNPRIVILEGED (`profile`, `email`, `offline_access` ONLY):**
   - Do **NOT** grant `account.read` or `account.profile.write` to sample apps or untrusted/third-party apps (`tests/mobile-oauth.test.mjs` explicitly asserts that `gothwad-accounts-sample-alpha` is blocked with `403` / `400`).
2. **Never break an already-applied `migrations/0014_first_party_native.sql` ledger in Cloudflare D1:**
   - Wrangler's D1 migration ledger treats `0014_first_party_native.sql` as already applied in production. Ship a **new** additive migration `migrations/0015_first_party_account_management.sql` rather than relying solely on modifying `0014`.
3. **Never undo operator de-approval (`first_party = 0` or `active = 0`):**
   - `firstPartySeeds()` in `cloudflare-worker/src/first-party.ts` and `migrations/0015_first_party_account_management.sql` must preserve manual `first_party = 0`, `active = 0`, and `android_cert_sha256` updates, as asserted by `tests/first-party-oauth.test.mjs`.
4. **Enforce strict audience + binding + scope checks on every `/api/mobile/*` request:**
   - Every caller must satisfy `firstPartyAllowed(ctx.client)`, be present in an explicit server-side account-management allowlist (`ACCOUNT_MANAGEMENT_CLIENTS`), match `ctx.payload.aud === ctx.client.id`, and include `account.read` (for `GET`) or `account.profile.write` (for non-`GET`).
   - Do **not** loosen `/api/mobile/auth/peer-code` (`routes/mobile-auth.ts`) — keep PKCE code binding and `firstPartyAllowed` intact.

---

## Exact File-by-File Changes Required in `Bansify/accounts`

### 1. First-Party Policy & Migration (`cloudflare-worker/src/first-party.ts` + `migrations/0015_first_party_account_management.sql`)

1. **`cloudflare-worker/src/first-party.ts`**:
   - Export an explicit allowlist constant for native account management:
     ```ts
     export const ACCOUNT_MANAGEMENT_CLIENTS = new Set([
       ACCOUNTS_ANDROID,
       'gothwad-mail',
       'gothwad-drive',
       'kiwi-store',
       'gothwad-tube',
     ]);
     ```
   - Export a helper:
     ```ts
     export function canManageAccountClient(clientId: string): boolean {
       return ACCOUNT_MANAGEMENT_CLIENTS.has(clientId);
     }
     ```
   - In `officialClients(domain)`:
     - Keep `gothwad-accounts-android` with `[...profile, 'drive.read', 'account.read', 'account.profile.write']`.
     - Keep `gothwad-accounts-sample-alpha` and `gothwad-accounts-sample-beta` with `profile` (`['profile', 'email', 'offline_access']`) ONLY.
     - Add `'account.read'` and `'account.profile.write'` to `gothwad-mail`, `gothwad-drive`, `kiwi-store`, and `gothwad-tube`:
       - `gothwad-mail`: `[...profile, 'mail', 'account.read', 'account.profile.write']`
       - `gothwad-drive`: `[...profile, 'drive', 'drive.read', 'account.read', 'account.profile.write']`
       - `kiwi-store`: `[...profile, 'account.read', 'account.profile.write']`
       - `gothwad-tube`: `[...profile, 'account.read', 'account.profile.write']`
   - In `firstPartySeeds(db, domain)`:
     - Keep the existing `14_first_party_native` seed statements intact, and add a gated `15_first_party_account_management` upgrade statement that updates `allowed_scopes` ONLY for rows where `id IN ('gothwad-accounts-android', 'gothwad-mail', 'gothwad-drive', 'kiwi-store', 'gothwad-tube')` AND `NOT EXISTS (SELECT 1 FROM auth_schema_versions WHERE version = '15_first_party_account_management')` (without modifying `first_party`, `active`, or `android_cert_sha256`), then inserts `'15_first_party_account_management'` into `auth_schema_versions`.

2. **`migrations/0015_first_party_account_management.sql` & `tools/generate-first-party-sql.mjs`**:
   - Create `migrations/0015_first_party_account_management.sql` updating `allowed_scopes` for `gothwad-mail`, `gothwad-drive`, `kiwi-store`, and `gothwad-tube` (and recording `'15_first_party_account_management'` in `auth_schema_versions`).
   - Check `tests/first-party-oauth.test.mjs:32` (`assert.equal(readFileSync('migrations/0014_first_party_native.sql', 'utf8'), generateFirstPartySql('bansify.com'))`):
     - Keep `migrations/0014_first_party_native.sql` immutable (or have `tools/generate-first-party-sql.mjs` expose both the frozen migration-14 generator and migration-15 SQL generator, updating `tests/first-party-oauth.test.mjs` if needed) so `0014` stays safe for deployed D1 instances while `first-party-oauth.test.mjs` passes cleanly.

### 2. `/api/mobile/*` Gate & Activity Logging (`cloudflare-worker/src/routes/mobile.ts` & `mobile-security.ts`)

1. **`cloudflare-worker/src/routes/mobile.ts`**:
   - Replace the hardcoded `ACCOUNTS_ANDROID` check on line 37:
     ```ts
     const write = request.method !== 'GET';
     if (!firstPartyAllowed(ctx.client) ||
       !canManageAccountClient(ctx.client.id) ||
       ctx.payload.aud !== ctx.client.id ||
       !ctx.scopes.includes(write ? 'account.profile.write' : 'account.read')) {
       return wrap(json({ error: 'insufficient_scope' }, 403));
     }
     ```
   - Verify `source_session_id`, `accessGate(ctx)`, and `/oauth/userinfo` (`routes/oauth.ts`) continue binding properly for all `ACCOUNT_MANAGEMENT_CLIENTS`.
2. **Accurate client attribution in activity logs (`routes/mobile.ts` & `routes/mobile-security.ts`)**:
   - Define `const clientLabel = \`From ${ctx.client.name || 'Bansify Android'}\`;` and replace all hardcoded `'From Accounts Android'` strings across `mobile.ts` and `mobile-security.ts` with `clientLabel` (and pass `clientLabel` or `ctx` into `handleMobileSecurityRoute` cleanly).

### 3. Tests (`tests/first-party-oauth.test.mjs` & `tests/mobile-oauth.test.mjs`)

1. **`tests/first-party-oauth.test.mjs`**:
   - Assert that `gothwad-mail`, `gothwad-drive`, `kiwi-store`, and `gothwad-tube` include `'account.read'` and `'account.profile.write'` in `allowed_scopes`, while `gothwad-accounts-sample-alpha` and `gothwad-accounts-sample-beta` remain strictly `['profile', 'email', 'offline_access']`.
   - Test applying `migrations/0014_first_party_native.sql` followed by `migrations/0015_first_party_account_management.sql` on an existing DB, confirming operator de-approval (`first_party = 0`, `active = 0`) and `android_cert_sha256` remain preserved.
2. **`tests/mobile-oauth.test.mjs`**:
   - Add an end-to-end test where a signed-in user authorizes `gothwad-mail` with `profile email offline_access mail account.read account.profile.write`:
     - `GET /oauth/userinfo` returns extended `security`, `phone_number`, and `recovery_email` fields.
     - `GET /api/mobile/sessions`, `GET /api/mobile/activity`, `GET /api/mobile/authorizations`, and `PATCH /api/mobile/profile` succeed with `200`, and activity logs record `'From Bansify Mail'`.
     - Cross-audience token (`aud != ctx.client.id` or token without `account.read`) still fails with `403`.
     - `gothwad-accounts-sample-alpha` continues to fail `/oauth/authorize` for `account.read` (`400`) and fails `/api/mobile/sessions` (`403`).

---

## Verification, PR Merge & Final Response Instructions

1. Run the full `Bansify/accounts` test suite (`npm test` / `node --test tests/*.test.mjs`).
2. Commit your changes, push to the active branch (`git push -u origin <active-branch>`), open a pull request to `main`, and wait for CI before merging (`gh pr merge <number> --merge`).
3. Note for production rollout: apply `migrations/0015_first_party_account_management.sql` to Cloudflare D1 (`wrangler d1 migrations apply`) alongside deploying the Worker.
