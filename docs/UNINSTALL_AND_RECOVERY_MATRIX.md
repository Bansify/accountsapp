# Uninstall, Reinstall & Credential Recovery Matrix

Every state transition preserves **zero mandatory hub app dependency**, **fail-closed encrypted storage**, and **explicit user recovery** without silently wiping sibling apps or downgrading security.

---

## 1. Cross-App Lifecycle Matrix (Scenarios A–E & Uninstall/Reinstall)

| Scenario / Action | Mail (`com.gothwad.mail`) | Drive (`com.gothwad.drive`) | Store (`com.gothwad.store`) | Accounts (`com.gothwad.accounts`) | User Experience & Recovery Path |
| --- | --- | --- | --- | --- | --- |
| **A. Mail installed first; signs in A & B; switches to B** | Active = B; holds `{A, B}` mail-scoped tokens | Not installed | Not installed | Not installed | Immediate standalone login via Custom Tabs PKCE; Mail exposes `{A, B}` metadata to future verified peers. |
| **B. Drive installed later (no Accounts app); chooses A** | Active stays **B** (`mail` tokens intact) | Discovers `{A, B}` from Mail; gets Drive tokens for A (`active = A`) | Not installed | Not installed | Zero re-typing when SSO/handoff applies; Drive selection (`A`) does not switch open Mail (`B`). |
| **C. Drive adds Account C** | Discovers `{A, B, C}` on sync; stays active on **B** | Active = **C**; holds `{A, C}` drive tokens | Not installed | Not installed | Mail shows C in switcher ("Continue as C") without interrupting B. |
| **D. Accounts app installed later** | Intact (`active = B`, `{A, B}` tokens) | Intact (`active = A/C`, drive tokens) | Optional | Discovers `{A, B, C}` from Mail + Drive immediately | Accounts adopts all 3 device accounts cleanly; zero account wipe. |
| **E. Mail uninstalled** | Removed by OS | **Intact** (`{A, C}` tokens + Drive's directory stays live) | **Intact** (works standalone) | **Intact** (still sees Drive/Store accounts) | Unlike shared `AccountManager`, uninstalling the first-installed app never wipes accounts in other apps. |
| **All Gothwad apps uninstalled** | Removed | Removed | Removed | Removed | Local Keystore/AEAD vaults removed by OS (`allowBackup="false"`, `noBackupFilesDir`). Browser Custom Tabs session/cookies stay independent; reinstalling any app uses web SSO or fresh login. |
| **Reinstall same app (same signing key)** | Vault cleared on prior uninstall | Unchanged; peers still hold `{A, C}` | Unchanged | Unchanged | Reinstalled Mail immediately re-discovers accounts from verified installed peers (`Drive`/`Store`/`Accounts`) via `AccountDirectoryProvider`! |
| **App data cleared ("Clear storage")** | Local tokens + active choice wiped | Unchanged | Unchanged | Unchanged | On relaunch, cleared app re-discovers `{A, B, C}` from peers and re-authorizes cleanly. |
| **FOSS / different signing cert build** | Peer discovery disabled (`BrokerPolicy.Disabled`) | Peer discovery disabled if untrusted | Peer discovery disabled if untrusted | Peer discovery disabled if untrusted | **Fail-closed gracefully**: untrusted apps are denied peer metadata by `CallerVerifier`, falling back transparently to independent browser PKCE OAuth. |

---

## 2. Sign-Out Scope & Credential / Keystore Recovery Matrix

| Event / Failure Mode | Local App Vault | Peer Directory / Other Apps | Server (`accounts` D1) | UI State & User Recovery |
| --- | --- | --- | --- | --- |
| **"Sign out of this app"** (`signOut`) | Deletes active app's `Credential` for `userId` | Other apps keep their own scoped tokens; account remains discoverable from remaining peers | Existing browser cookies & other app refresh tokens stay valid | App switches to remaining local account or `SignedOut` (showing peer "Continue as..." chips if other apps still have it). |
| **"Remove from this device"** (`removeFromDevice`) | Deletes local `Credential` + writes removal tombstone (`removedAt`) | Best-effort verified IPC notifies installed peers to clear cached discovery & remove local device entry | Browser session & remote devices remain unless user revokes via `/me#security` | Clear dialog explains: clears Gothwad apps on this phone; links to **Manage sessions on web** for server-side session revocation. |
| **Password changed / Google unlinked / Web grant revoked** | Next refresh or `/oauth/userinfo` fails (`401 invalid_grant` via `oauth_credential_bindings`) | Only affected account loses grant | D1 revokes `oauth_credential_bindings` & refresh tokens | `OAuthException(rejected = true)` prunes dead grant locally, publishes `AuthFailure.SessionExpired`, prompts user **Sign in again**. |
| **Offline / Network outage / Worker 503** | **Preserved** (tokens & profile retained!) | Intact | Intact | Publishes `AuthFailure.Unavailable` (retry banner); **never logs user out** on timeout or `5xx`. |
| **Android Keystore key invalidated / AEAD ciphertext corrupted** | Read fails closed (`AEADBadTagException` / `KeyStoreException`) | Other apps unaffected | Intact | Enters `AccountState.StorageCorrupted` (never falls back to plaintext); offers explicit **Reset damaged local storage & sign in** (`recoverCorruptedStorage()`). |
| **Process killed while Custom Tabs login is open** | `PendingLogin(state, verifier, createdAt)` persisted encrypted before launching browser | Unaffected | Code expires in minutes | On redirect callback, `Application.onCreate()` -> `GothwadAccounts.init()` -> restores `PendingLogin`, validates exact callback/state + 10-min TTL, consumes single-use. |
| **Slow refresh races with `signOut(userId)`** | `generation` epoch + `original` reference check | Unaffected | Rotated token never persisted | `SessionManager` drops late refresh result; **account is never resurrected** (`SessionManagerTest.logoutDuringRefreshCannotResurrectAccount`). |
