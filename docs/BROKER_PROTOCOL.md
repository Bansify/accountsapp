# Distributed Cross-App Broker Protocol (Peer Directory v1 + Server Handoff Plan)

## 1. Overview — Zero Central App, Mutual Certificate Trust

Every Bansify Android app using `:core` (`accountsapp`, `mailapp`, `drive`, `storeapp`, `tubeapp`, `sample.alpha`, `sample.beta`) participates symmetrically as both a **Peer Directory Provider** and a **Peer Directory Client**:

- **No `QUERY_ALL_PACKAGES`**: Manifest `<queries>` declares only explicit first-party package IDs (`com.gothwad.accounts`, `com.gothwad.mail`, `com.gothwad.drive`, `com.gothwad.store`, `com.gothwad.tube`, `com.gothwad.accounts.sample.alpha`, `com.gothwad.accounts.sample.beta`).
- **Host-Scoped Authorities**: Each host exposes `${applicationId}.gothwad.account.directory` (`AccountDirectoryProvider`), preventing Android manifest authority collisions across installed apps.
- **No Secret Leakage**: The directory holds **metadata only** (`userId`, `username`, `name`, `email`, `avatarUrl`, `addedAt`, `lastSeenAt`, `sourcePackage`, `removedFromDeviceAt`). Access tokens and refresh tokens never cross IPC.

---

## 2. Two Operating Tiers

| Tier | Policy (`BrokerPolicy`) | Behavior | Backend Dependency |
| --- | --- | --- | --- |
| **Disabled (FOSS / Untrusted)** | `BrokerPolicy.Disabled` | `AccountDirectoryProvider` rejects all callers with `SecurityException`; `PeerBrokerClient` never queries siblings. | None (browser PKCE only). |
| **Peer Directory v1 (Active in `:core`)** | `BrokerPolicy.PeerDiscovery(trustPolicy)` | Mutually verifies caller & target Binder UID + package + SHA-256 cert. Discovers device accounts (`DiscoveredAccount`), shows "Continue as ...", triggers target app's own PKCE `/oauth/authorize`, syncs device-removal tombstones cleanly. | Existing `accounts` OAuth 2.0 endpoints (`/oauth/authorize`, `/oauth/token`, `/oauth/userinfo`). |
| **Zero-Prompt Server Handoff (Gated Future Extension)** | Enabled after `accounts` backend PR | Target app creates PKCE verifier/challenge + nonce; verified peer requests single-use server handoff code bound to `(targetClientId, userId, codeChallenge, allowedScopes)`; target redeems directly with `code_verifier`. | Requires reviewed `accounts` Worker + D1 PR ([SERVER_CHANGES.md](SERVER_CHANGES.md)). |

---

## 3. IPC Methods & Security Gates

Every `AccountDirectoryProvider.call(method, arg, extras)` call executes synchronously:
1. Check `BrokerPolicy` — fail immediately if `Disabled`.
2. Capture `Binder.getCallingUid()` **before** any async work.
3. Call `CallerVerifier.verifyBinderCaller(...)`:
   - Resolve `packageManager.getPackagesForUid(callingUid)`.
   - Reject missing packages or mixed-UID apps unless every package in that UID passes trust verification.
   - Verify SHA-256 signing certificate via `SigningInfo.hasSigningCertificate` (API 28+) or `GET_SIGNATURES` (API 26–27).
4. Enforce bounded payload limits and dispatch only:
   - `METHOD_LIST_ACCOUNTS` (`"gothwad.directory.list_v1"`): Returns serialized `DirectorySnapshot` (active accounts + tombstone map; zero tokens).
   - `METHOD_REMOVE_FROM_DEVICE` (`"gothwad.directory.remove_v1"`): Records signed timestamped tombstone for `userId` and revokes that app's local credentials when user chooses **"Remove from this device"** (never invoked for local "Sign out of this app").

---

## 4. State Machine

```text
Unavailable / Untrusted / Disabled ──> BrowserFallback (independent Custom Tabs PKCE)

DiscoverPeers (verified PackageManager + Cert)
  ├─> VerifyTargetProviderCert
  ├─> ContentResolver.call(METHOD_LIST_ACCOUNTS)  [Provider verifies Caller UID + Cert]
  ├─> Merge non-secret accounts by immutable `userId` & apply device-removal tombstones
  └─> Publish DiscoveredAccount list to UI / Chooser
        ├─ User picks existing local account -> App-local switchTo(userId) (NO network/peer side-effect)
        ├─ User picks discovered peer account -> Launch OAuth PKCE for THIS app's client_id + scopes
        └─ User taps "Add another account" -> Launch OAuth PKCE (choose=1)
```
