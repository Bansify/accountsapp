# Integration & Verification

## 1. Gradle & JitPack modules

Add JitPack in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then depend on the shared UI + Core SDK (`:ui` exposes `:core` transitively via `api(project(":core"))` and its POM dependency):

```kotlin
implementation("com.github.Bansify.accountsapp:ui:<tag>")
// Or headless auth + broker + token refresh only:
implementation("com.github.Bansify.accountsapp:core:<tag>")
```

Both `project.group` and `MavenPublication.groupId` are configured as `com.github.Bansify.accountsapp` in `:core` and `:ui`, matching the `Bansify/accountsapp` repository for clean JitPack builds and `:ui -> :core` transitive POM resolution.

## 2. Host manifest setup

`:core` automatically merges:
- Exported `AccountCallbackActivity` (without hardcoded intent filters — each host defines its exact callback URI),
- Host-scoped `broker.AccountDirectoryProvider` (`android:authorities="${applicationId}.bansify.account.directory"`, `exported="true"`),
- Bounded `<queries>` allowlist for first-party Bansify packages (`com.bansify.accounts`, `com.bansify.mail`, `com.gothwad.drive`, `com.gothwad.store`, `com.gothwad.tube`, `com.bansify.accounts.sample.alpha`, `com.bansify.accounts.sample.beta`).

`:ui` automatically merges:
- `AccountManagementActivity` (`exported="false"`, `windowSoftInputMode="adjustResize"`),
- `AccountSignInActivity` (`exported="false"`, `windowSoftInputMode="adjustResize"`).

Each host app adds its exact OAuth callback filter to `AccountCallbackActivity` in its own `AndroidManifest.xml`:

```xml
<activity
    android:name="com.bansify.accounts.core.AccountCallbackActivity"
    android:exported="true">
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

For production HTTPS App Links, publish `https://<host>/.well-known/assetlinks.json` containing the actual release signing SHA-256 fingerprint for that app's `applicationId`.

## 3. Shared SDK entry points for sibling apps

1. **Initialize once in `Application.onCreate()`**:
   ```kotlin
   GothwadAccounts.init(
       application = this,
       config = GothwadConfig(
           clientId = "gothwad-mail",
           redirectUri = "https://mail.bansify.com/oauth/callback",
           scopes = setOf(
               Scope.Profile,
               Scope.Email,
               Scope.OfflineAccess,
               Scope.Mail,
               Scope.AccountRead,
               Scope.AccountProfileWrite,
           ),
           allowDebugBuild = BuildConfig.DEBUG,
       ),
   )
   ```
   - `Scope.AccountRead` + `Scope.AccountProfileWrite` allow first-party client apps (`gothwad-mail`, `gothwad-drive`, `kiwi-store`, `gothwad-tube`) to display the full in-app native Bansify Account Centre when opened via `GothwadAccounts.openAccountManagement(activity)` (backed by `Bansify/accounts` first-party `/mobile/*` endpoints).
   - If the token was issued without those scopes (e.g., before backend rollout or for sample clients), `AccountDashboardViewModel` automatically detects `403` / `insufficient_scope` and downgrades cleanly to authenticated web `/me` links without errors or crashes.

2. **Supply fresh Bearer tokens to host APIs**:
   ```kotlin
   val token: String? = GothwadAccounts.currentAccessToken()
   ```
   `currentAccessToken()` transparently refreshes expiring tokens using the active user's refresh token before returning.

3. **Shared Sign-In, Sign-Up, MFA & Peer SSO screen**:
   ```kotlin
   GothwadAccounts.openSignIn(
       activity = activity,
       startStep = NativeAuthStep.IDENTIFIER, // or NativeAuthStep.SIGN_UP
       appName = "Bansify Mail",
   )
   ```
   Or embed `@Composable GothwadSignInScreen(...)` inside your Compose navigation graph.

4. **Shared Account Switcher & Account Management**:
   - Render `GothwadAccountAvatarButton` in toolbars/headers and `GothwadAccountSwitcherSheet` on tap.
   - Route "Manage your Bansify Account" / Settings account management to `GothwadAccounts.openAccountManagement(activity)` — it always opens inside the host app using shared `:ui` screens (never requiring the standalone Accounts APK to be installed).

## 4. OAuth client registrations

| Host | `client_id` | `redirect_uri` (dev / prod) | Scopes |
| --- | --- | --- | --- |
| Standalone (`:app`) | `gothwad-accounts-android` | `https://accounts.bansify.com/oauth/callback`, `com.bansify.accounts:/oauth/callback` | `profile email offline_access drive.read account.read account.profile.write` |
| Sample alpha | `gothwad-accounts-sample-alpha` | `com.bansify.accounts.sample.alpha:/oauth/callback` | `profile email offline_access` |
| Sample beta | `gothwad-accounts-sample-beta` | `com.bansify.accounts.sample.beta:/oauth/callback` | `profile email offline_access` |
| Kiwi Store (`storeapp`) | `kiwi-store` | `kiwistore://oauth/callback`, `https://kiwistore.bansify.com/oauth/callback`, `com.gothwad.store:/oauth/callback` | `profile email offline_access account.read account.profile.write` |
| PlusTube (`tubeapp`) | `gothwad-tube` | `https://tube.bansify.com/oauth/callback`, `com.gothwad.tube:/oauth/callback` | `profile email offline_access account.read account.profile.write` |
| Mail (`mailapp`) | `gothwad-mail` | `https://mail.bansify.com/oauth/callback`, `com.bansify.mail:/oauth/callback` | `profile email offline_access mail account.read account.profile.write` |
| Drive (`drive`) | `gothwad-drive` | `https://drive.bansify.com/oauth/callback`, `com.gothwad.drive:/oauth/callback` | `profile email offline_access drive drive.read account.read account.profile.write` |

All mobile clients are **public PKCE (`S256`)** clients with no embedded `client_secret` in the APK.

## 5. Build & test commands

```sh
python3 tools/check_docs.py && python3 tools/check_android_scaffold.py

./gradlew \
  :core:testDebugUnitTest :ui:testDebugUnitTest \
  :core:lintDebug :ui:lintDebug :app:lintDebug :sample:lintAlphaDebug :sample:lintBetaDebug \
  :app:assembleDebug :sample:assembleAlphaDebug :sample:assembleBetaDebug \
  :core:publishReleasePublicationToMavenLocal :ui:publishReleasePublicationToMavenLocal
```

## 6. Device verification matrix

Verify on physical Android devices / emulators:

1. **Independent install order**: Install `sampleAlphaDebug`, sign in with 2 accounts, switch active account. Install `sampleBetaDebug` afterwards (`app` NOT installed). Verify Beta sees both accounts via `AccountDirectoryProvider`, authorizes its chosen account separately without modifying Alpha's active selection or refresh tokens.
2. **Shared sibling-app flows**: In Mail / Drive / Store (both with and without standalone Accounts installed), verify `GothwadAccounts.openSignIn(...)` handles native password + MFA + sign-up + peer silent SSO + browser fallback, `GothwadAccountSwitcherSheet` switches/adds/signs out accounts cleanly, and `GothwadAccounts.openAccountManagement(...)` opens the full in-app Bansify Account Centre.
3. **Uninstall resilience**: Uninstall Alpha while Beta + `app` remain installed; verify neither Beta nor `app` loses sessions or crashes.
4. **Browser & callback resilience**: Existing SSO session, fresh signup, chooser, consent denial, toolbar close without callback, process death between Custom Tab launch and callback delivery, and replay of a consumed callback URI.
5. **Corrupted storage recovery**: Simulate Keystore invalidation (`StorageCorrupted`) -> verify fail-closed state and clean reset via `recoverCorruptedStorage()`.
6. **Localization & accessibility**: Verify English (`values`) and Hindi (`values-hi`), Dark Mode, dynamic font scaling, and TalkBack focus order across all 6 management tabs, sign-in flow, and sample widgets.

See [UNINSTALL_AND_RECOVERY_MATRIX.md](UNINSTALL_AND_RECOVERY_MATRIX.md), [SERVER_CHANGES.md](SERVER_CHANGES.md), [ACCOUNTS_BACKEND_FIRST_PARTY_MOBILE_PROMPT.md](ACCOUNTS_BACKEND_FIRST_PARTY_MOBILE_PROMPT.md), and [MAIL_ANDROID_SHARED_SDK_PROMPT.md](MAIL_ANDROID_SHARED_SDK_PROMPT.md) for cross-repo rollout details.
