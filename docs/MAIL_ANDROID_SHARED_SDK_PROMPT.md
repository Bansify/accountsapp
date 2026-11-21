# Bansify Mail Android (`Bansify/mailapp`): Migrate to Shared `Bansify/accountsapp` SDK Prompt

Paste the prompt below into the coding agent working in **`Bansify/mailapp`**.
Do not run this prompt inside `Bansify/accounts` or `Bansify/accountsapp`.

---

Work only in this `Bansify/mailapp` repository on the active session branch. First read `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/gothwad/mail/MailApplication.kt`, `app/src/main/java/com/gothwad/mail/MainActivity.kt`, `app/src/main/java/com/gothwad/mail/data/remote/MailApiClient.kt`, `app/src/main/java/com/gothwad/mail/ui/navigation/MailNavGraph.kt`, `app/src/main/java/com/gothwad/mail/ui/viewmodel/MailViewModel.kt`, `app/src/main/java/com/gothwad/mail/ui/screens/InboxScreen.kt`, `app/src/main/java/com/gothwad/mail/ui/screens/SettingsScreen.kt`, `tools/check_android_scaffold.py`, `tools/check_docs.py`, and `.github/workflows/android-ci.yml` before editing.

## Why `Bansify/mailapp` Should Adopt the Shared SDK

`Bansify/mailapp` currently maintains its own duplicated copy of OAuth PKCE, Tink EncryptedTokenStore, ContentProvider peer broker (`auth/broker/*`), callback activity, signed-out onboarding screen, native auth sheet, and account switcher dialog.

`Bansify/accountsapp` now publishes `:core` and `:ui` via JitPack (`com.github.Bansify.accountsapp:ui:<tag>`) with:
- **Shared headless auth & broker (`:core`)**: `GothwadAccounts.init(...)`, `GothwadAccounts.currentAccessToken()`, `AccountCallbackActivity` (`exported="true"`), and host-scoped `AccountDirectoryProvider` (`${applicationId}.gothwad.account.directory`).
- **Shared Sign-In / Sign-Up / MFA / Peer SSO screen (`:ui`)**: `GothwadAccounts.openSignIn(activity, ...)` / `@Composable GothwadSignInScreen(...)` — automatically returns when sign-in or adding an account completes.
- **Shared Account Switcher & Toolbar Widgets (`:ui`)**: `GothwadAccountAvatarButton`, `GothwadAccountSwitcherSheet`, `GothwadAuthStatusBanner`, `GothwadSignInButton`.
- **Shared In-App Account Centre (`:ui`)**: `GothwadAccounts.openAccountManagement(activity)` — works natively inside `mailapp` (using `account.read` + `account.profile.write` scopes when enabled on `Bansify/accounts`, and gracefully falling back to authenticated web `/me` without crashing if `403 insufficient_scope` occurs), with no requirement to install the standalone Accounts APK.

---

## Mandatory Compatibility & Policy Invariants

1. **Keep `applicationId = "com.gothwad.mail"`** (plus `applicationIdSuffix = ".debug"` in `debug` buildType) and `${gothwadRedirectScheme}` unchanged.
2. **Keep `OAuth client_id = "gothwad-mail"`** and redirects (`https://mail.bansify.com/oauth/callback` for release / `com.gothwad.mail:/oauth/callback` or `appLink` scheme matching manifest).
   - Request scopes:
     ```kotlin
     setOf(
         Scope.Profile,
         Scope.Email,
         Scope.OfflineAccess,
         Scope.Mail,
         Scope.AccountRead,
         Scope.AccountProfileWrite,
     )
     ```
   - **Token migration note**: `accountsapp` stores tokens in `gothwad-<sha>.enc` / alias `gothwad.accounts.<sha>`, whereas Mail's legacy `EncryptedTokenStore` used `gothwad-mail-<sha>.enc`. Upgrading users re-authenticate cleanly (or via one-tap peer SSO if another updated Bansify app holds a session).
3. **Preserve `.github/workflows/android-ci.yml`, `.github/workflows/release.yml`, and `tools/check_docs.py` invariants**:
   - Keep `README.md` required sections (`## Architecture`, `### Endpoints used`, `## Build and test`, `## Manual device checks`), all documented API endpoints, and all 4 signing secret names (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
   - Update `tools/check_android_scaffold.py` to expect `:core`'s merged components (`com.gothwad.accounts.core.AccountCallbackActivity` and `com.gothwad.accounts.core.broker.AccountDirectoryProvider` with `${applicationId}.gothwad.account.directory`) instead of the deleted local `.auth.*` classes.
4. **Build config check (`app/build.gradle.kts`)**:
   - Ensure `minSdk >= 26`, `compileSdk = 36`, `allowDebugBuild = BuildConfig.DEBUG` in `GothwadConfig(...)` (needed for `.debug` sibling discovery testing), and configure `settings.gradle.kts` with `maven("https://jitpack.io")`.
   - Use the latest `v1.0.*` release tag from `Bansify/accountsapp` that includes `:core` `currentAccessToken()` and `:ui` `GothwadAccounts.openSignIn(...)` (check `gh release list --repo Bansify/accountsapp --limit 5` or git tags if needed; do **not** use mutable `-SNAPSHOT` for releases).

---

## Exact File-by-File Changes Required in `Bansify/mailapp`

### 1. Gradle & Dependencies (`settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`)

1. **`settings.gradle.kts`**:
   - Add `maven("https://jitpack.io")` to `dependencyResolutionManagement.repositories`.
2. **`app/build.gradle.kts` (and `gradle/libs.versions.toml`)**:
   - Add `implementation("com.github.Bansify.accountsapp:ui:<release-tag>")`.
   - Note: `:core` comes transitively through `:ui`. Retain `tink`, `security-crypto`, `browser`, `okhttp`, `kotlinx-serialization-json` only if Mail's own Mail API / Room / tests still directly reference them; remove unused OAuth/broker dependencies if redundant.

### 2. Android Manifest (`app/src/main/AndroidManifest.xml`)

1. Keep `<queries>` or let `:core`'s manifest merger supply the bounded Bansify `<queries>` packages and `com.gothwad.accounts.core.broker.AccountDirectoryProvider`.
2. Remove the old `.auth.AccountCallbackActivity` and `.auth.broker.AccountDirectoryProvider` entries from `app/src/main/AndroidManifest.xml`, and replace them with the host callback binding on `:core`'s activity:
   ```xml
   <activity
       android:name="com.gothwad.accounts.core.AccountCallbackActivity"
       android:exported="true"
       android:launchMode="singleTop"
       android:theme="@android:style/Theme.Translucent.NoTitleBar">
       <intent-filter>
           <action android:name="android.intent.action.VIEW" />
           <category android:name="android.intent.category.DEFAULT" />
           <category android:name="android.intent.category.BROWSABLE" />
           <data android:scheme="${gothwadRedirectScheme}" />
       </intent-filter>
       <intent-filter android:autoVerify="true">
           <action android:name="android.intent.action.VIEW" />
           <category android:name="android.intent.category.DEFAULT" />
           <category android:name="android.intent.category.BROWSABLE" />
           <data
               android:scheme="https"
               android:host="mail.bansify.com"
               android:path="/oauth/callback" />
       </intent-filter>
   </activity>
   ```

### 3. Application & Token Provider Wiring (`MailApplication.kt`, `MainActivity.kt`, `MailApiClient.kt`)

1. **`MailApplication.kt`**:
   - Initialize `GothwadAccounts.init(this, GothwadConfig(...))` in `onCreate()`:
     - `clientId = "gothwad-mail"`
     - `redirectUri = "https://mail.bansify.com/oauth/callback"` (note: match whatever `redirectUri` default Mail uses for production App Links vs custom scheme, ensuring `com.gothwad.mail:/oauth/callback` works smoothly on debug builds if App Links aren't verified on `.debug` — e.g. `if (BuildConfig.DEBUG) "com.gothwad.mail:/oauth/callback" else "https://mail.bansify.com/oauth/callback"`, keeping both registered in `Bansify/accounts`!)
     - `scopes = setOf(Scope.Profile, Scope.Email, Scope.OfflineAccess, Scope.Mail, Scope.AccountRead, Scope.AccountProfileWrite)`
     - `allowDebugBuild = BuildConfig.DEBUG`
   - Wire `MailApiClient(tokenProvider = MailAccessTokenProvider { GothwadAccounts.currentAccessToken() })`.
2. **`MailViewModel.kt`**:
   - Replace `GothwadMailAuth`, `MailAuthAccount`, `MailAuthFailure`, `DisconnectOutcome`, `DiscoveredAccount` (local copy), and `AuthScreenState` with `com.gothwad.accounts.core.GothwadAccounts`, `AccountState`, `GothwadAccount`, `AuthFailure`, `com.gothwad.accounts.core.DiscoveredAccount`.
   - Observe `GothwadAccounts.state` and `GothwadAccounts.discoveredAccounts` directly:
     - When `GothwadAccounts.state` emits `AccountState.SignedIn`, sync `MailRepository` accounts (`activeAccount` + `all` accounts + remove stale Room accounts no longer in `all`).
     - When `switchAccount(email)`: find matching `userId` in `(GothwadAccounts.state.value as? AccountState.SignedIn)?.all` and call `GothwadAccounts.switchTo(userId)`.
     - Sign-out / Disconnect / Remove on device:
       - Sign out local account: `GothwadAccounts.signOut(userId, revokeOnServer = false)`
       - Disconnect Mail grant on server: `GothwadAccounts.signOut(userId, revokeOnServer = true)`
       - Remove account + broadcast purge to peers: `GothwadAccounts.removeFromDevice(userId)` + `repository.deleteAccountData(email)`
     - Remove local native auth form state machine (`nativeAuthState`, `openNativeSignIn`, `submitNativeIdentifier`, `submitNativePassword`, `verifyNativeMfa`, `submitNativeSignUp`, etc.) — `GothwadAccounts.openSignIn(...)` handles all of it inside `:ui`!

### 4. Replace Duplicated UI with Shared `:ui` Screens & Widgets

1. **Signed-out & Sign-In flow (`MailNavGraph.kt`, delete `ui/screens/SignedOutScreen.kt` & `ui/screens/NativeAuthSheet.kt`)**:
   - When `accountState is AccountState.Initializing`: show a clean loading spinner.
   - When `accountState is AccountState.StorageCorrupted`: show a concise recovery card calling `GothwadAccounts.recoverCorruptedStorage()`.
   - When `accountState is AccountState.SignedOut`: either render `GothwadSignInScreen(appName = "Bansify Mail", allowWhenAlreadySignedIn = false, onClose = {}, ...)` directly inline (zero extra taps, instant peer-account cards + email/password/MFA/signup + browser fallback!) OR show a lightweight Mail branded welcome card with `GothwadSignInButton(onClick = { activity?.let { GothwadAccounts.openSignIn(it, appName = "Bansify Mail") } })`.
     - **Recommended**: Embedding `@Composable GothwadSignInScreen(...)` or a Mail-branded hero + `GothwadSignInButton` opening `GothwadAccounts.openSignIn(activity, appName = "Bansify Mail")` removes 100% of duplicate auth forms!
2. **Account Avatar & Switcher (`InboxScreen.kt`, delete `ui/components/AccountSwitcherDialog.kt`)**:
   - Replace the top-bar account avatar chip with `GothwadAccountAvatarButton(...)` (or wire Mail's existing avatar click to open `GothwadAccountSwitcherSheet(...)`).
   - Replace Mail's custom `AccountSwitcherDialog` with `:ui`'s `GothwadAccountSwitcherSheet`:
     - `onSwitchAccount` -> `viewModel.switchAccountByUserId(userId)`
     - `onContinueWithPeerAccount` -> launch a coroutine calling `GothwadAccounts.signInSilentlyWithPeer(peer)`, inspecting its `Result` and falling back to `GothwadAccounts.openSignIn(activity, startStep = NativeAuthStep.PASSWORD, initialIdentifier = peer.email ?: peer.username.orEmpty(), appName = "Bansify Mail")` on failure
     - `onAddAnotherAccount` -> `GothwadAccounts.openSignIn(activity, appName = "Bansify Mail")`
     - `onManageGothwadAccount` -> `GothwadAccounts.openAccountManagement(activity)`
     - `onSignOutThisApp` -> `viewModel.signOutUser(account.userId)`
     - `onOpenSettings` -> navigate to Mail's Settings screen
     - `onOpenPrivacy` / `onOpenTerms` -> `GothwadAccounts.openWebPage(activity, "/privacy")` / `"/terms"`
3. **Settings screen (`SettingsScreen.kt`)**:
   - Keep Mail-specific preferences (swipe actions, density, retention, notifications, signature, vacation responder, theme) untouched.
   - Route `"Manage your Bansify Account"` directly to `GothwadAccounts.openAccountManagement(activity)` (no custom local WebView/URL launcher needed).
   - Optionally include `GothwadAccountSettingsCard(...)` or keep Mail's clean `SettingsRow` entries wired to `GothwadAccounts`.

### 5. Remove Obsolete Duplicated Local Auth Code & Update Scaffold/Tests

1. Delete local auth classes replaced by `:core` and `:ui`:
   - `app/src/main/java/com/gothwad/mail/auth/*` (including `GothwadMailAuth.kt`, `GothwadEndpoints.kt`, `EncryptedTokenStore.kt`, `AccountCallbackActivity.kt`, `MailAuthSessionManager.kt`, `NativeAuthClient.kt`, `MailAuthModels.kt`, `Pkce.kt`, `CallbackValidator.kt`, `CallbackBudget.kt`, `auth/broker/*`) — extract any tiny Mail-specific constant (`MAIL_API_BASE_URL = "https://mail.bansify.com/api"`, `DEFAULT_SIGNATURE`, etc.) into `data/remote/MailApiClient.kt` or `MailEndpoints.kt` first!
   - `app/src/main/java/com/gothwad/mail/ui/screens/SignedOutScreen.kt`
   - `app/src/main/java/com/gothwad/mail/ui/screens/NativeAuthSheet.kt`
   - `app/src/main/java/com/gothwad/mail/ui/components/AccountSwitcherDialog.kt`
2. Delete obsolete tests for the deleted local auth layer (`PkceTest.kt`, `CallbackValidatorTest.kt`, `MailAuthSessionManagerTest.kt`, `NativeAuthTest.kt`, `PeerBrokerDiscoveryTest.kt` — all covered by `:core` and `:ui` tests in `accountsapp`), and keep/add Mail integration tests in `MailApiClientTest.kt`, `MailRepositorySyncTest.kt`, and `MailViewModelAuthWiringTest.kt` verifying:
   - `MailApiClient` fetches tokens cleanly from `MailAccessTokenProvider`.
   - `MailViewModel` syncs Room accounts with `AccountState.SignedIn` and purges local mail caches on `GothwadAccounts.removeFromDevice(userId)`.
3. Update `tools/check_android_scaffold.py` so it verifies `:core` manifest components (`com.gothwad.accounts.core.AccountCallbackActivity`), `com.github.Bansify.accountsapp` dependency, `https://mail.bansify.com/oauth/callback`, and release workflow requirements without failing on deleted `.auth.*` files.

---

## Verification, PR Merge & Final Response Instructions

1. Run and pass all checks locally:
   ```bash
   python3 tools/check_android_scaffold.py
   python3 tools/check_docs.py
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```
2. Verify `.github/workflows/android-ci.yml` can resolve `com.github.Bansify.accountsapp:ui:<tag>` from `https://jitpack.io` during CI!
3. Commit your changes, push to the active branch (`git push -u origin <active-branch>`), open a pull request to `main`, wait for CI to pass, and merge with `gh pr merge <number> --merge`.
