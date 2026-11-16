# Integration & Verification

## 1. Gradle modules

```kotlin
implementation(project(":ui")) // Includes :core; shared full-screen management UI + host widgets
// Or headless:
implementation(project(":core"))
```

Maven / JitPack publication targets are configured as multi-module coordinates (`com.github.GothwadTech.accountsapp:core:<tag>` and `com.github.GothwadTech.accountsapp:ui:<tag>`). Verify an external consumer resolution build before referencing a release tag in production apps.

## 2. Host manifest setup

`:core` automatically merges:
- Exported `AccountCallbackActivity` (without hardcoded intent filters — each host defines its exact callback URI),
- Host-scoped `broker.AccountDirectoryProvider` (`android:authorities="${applicationId}.gothwad.account.directory"`, `exported="true"`),
- Bounded `<queries>` allowlist for first-party Gothwad packages (`com.gothwad.accounts`, `com.gothwad.mail`, `com.gothwad.drive`, `com.gothwad.store`, `com.gothwad.tube`, `com.gothwad.accounts.sample.alpha`, `com.gothwad.accounts.sample.beta`).

Each host app adds its exact OAuth callback filter to `AccountCallbackActivity` in its own `AndroidManifest.xml`:

```xml
<activity
    android:name="com.gothwad.accounts.core.AccountCallbackActivity"
    android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="tube.gothwadtech.com"
            android:path="/oauth/callback" />
    </intent-filter>
</activity>
```

For production HTTPS App Links, publish `https://<host>/.well-known/assetlinks.json` containing the actual release signing SHA-256 fingerprint for that app's `applicationId`.

## 3. OAuth client registrations

| Host | `client_id` | `redirect_uri` (dev / prod) | Required scopes |
| --- | --- | --- | --- |
| Standalone (`:app`) | `gothwad-accounts-android` | `com.gothwad.accounts:/oauth/callback` | `profile email offline_access drive.read` |
| Sample alpha | `gothwad-accounts-sample-alpha` | `com.gothwad.accounts.sample.alpha:/oauth/callback` | `profile email offline_access` |
| Sample beta | `gothwad-accounts-sample-beta` | `com.gothwad.accounts.sample.beta:/oauth/callback` | `profile email offline_access` |
| Kiwi Store (`storeapp`) | `kiwi-store` | `com.gothwad.store:/oauth/callback` | `profile email offline_access` |
| PlusTube (`tubeapp`) | `gothwad-tube` | `https://tube.gothwadtech.com/oauth/callback` (or `com.gothwad.tube:/oauth/callback`) | `profile email offline_access` |
| Mail (`mailapp`) | `gothwad-mail` | `https://mail.gothwadtech.com/oauth/callback` (or `com.gothwad.mail:/oauth/callback`) | `profile email offline_access` |
| Drive (`drive`) | `gothwad-drive` | `https://drive.gothwadtech.com/oauth/callback` (or `com.gothwad.drive:/oauth/callback`) | `profile email offline_access drive` |

All mobile clients are **public PKCE (`S256`)** clients with no embedded `client_secret` in the APK.

## 4. Build & test commands

```sh
python3 tools/check_docs.py && python3 tools/check_android_scaffold.py

./gradlew \
  :core:testDebugUnitTest :ui:testDebugUnitTest \
  :core:lintDebug :ui:lintDebug :app:lintDebug :sample:lintAlphaDebug :sample:lintBetaDebug \
  :app:assembleDebug :sample:assembleAlphaDebug :sample:assembleBetaDebug \
  :core:publishReleasePublicationToMavenLocal :ui:publishReleasePublicationToMavenLocal
```

## 5. Device verification matrix

Verify on physical Android devices / emulators:

1. **Independent install order**: Install `sampleAlphaDebug`, sign in with 2 accounts, switch active account. Install `sampleBetaDebug` afterwards (`app` NOT installed). Verify Beta sees both accounts via `AccountDirectoryProvider`, authorizes its chosen account separately without modifying Alpha's active selection or refresh tokens.
2. **Uninstall resilience**: Uninstall Alpha while Beta + `app` remain installed; verify neither Beta nor `app` loses sessions or crashes.
3. **Browser & callback resilience**: Existing SSO session, fresh signup, chooser, consent denial, toolbar close without callback, process death between Custom Tab launch and callback delivery, and replay of a consumed callback URI.
4. **Corrupted storage recovery**: Simulate Keystore invalidation (`StorageCorrupted`) -> verify fail-closed state and clean reset via `recoverCorruptedStorage()`.
5. **Localization & accessibility**: Verify English (`values`) and Hindi (`values-hi`), Dark Mode, dynamic font scaling, and TalkBack focus order across all 6 management tabs and sample widgets.

See [UNINSTALL_AND_RECOVERY_MATRIX.md](UNINSTALL_AND_RECOVERY_MATRIX.md) and [SERVER_CHANGES.md](SERVER_CHANGES.md) for backend rollout details.
