# Releases, Publishing & Deployment

## Repository output split

| Repository | Output | Destination |
| --- | --- | --- |
| `Bansify/accounts` | Web pages (`/signin`, `/me`, `/privacy`, `/terms`) + Worker API | Cloudflare Pages & Workers |
| `Bansify/accounts` | Schema migrations (`0001`–`0014_first_party_native.sql`+) | Cloudflare D1 |
| `Bansify/accountsapp` | `:core` + `:ui` AARs, POMs, sources | Maven / JitPack (`com.github.Bansify.accountsapp:{core,ui}:<tag>`) |
| `Bansify/accountsapp` | Signed standalone Bansify Accounts APK (`com.gothwad.accounts`) & Play Store AAB | GitHub Releases |

Android requires no separate Cloudflare backend deployment; both web and Android talk directly to `https://accounts.bansify.com/api`.

## CI & Release automation

1. **`.github/workflows/android-ci.yml`** (PR/push, read-only permissions):
   - Validates docs & scaffold (`tools/check_docs.py`, `tools/check_android_scaffold.py`).
   - Provisions Android SDK 36 (`platforms;android-36`, `build-tools;36.0.0`) directly from `$ANDROID_HOME`.
   - Runs `:core:testDebugUnitTest`, `:ui:testDebugUnitTest`, lint across `:core`, `:ui`, `:app`, `:sample:alpha`, `:sample:beta`, debug + release APK builds, and `publishReleasePublicationToMavenLocal`.
   - Uploads debug APKs and HTML test/lint reports via `actions/upload-artifact@v4`.

2. **`.github/workflows/release.yml`** (every push to `main`/`master`, `v*` tag, or manual `workflow_dispatch`):
   - Automatically increments `versionCode` (or uses manual input) and sets `versionName` (`1.0.<run_number>` or tag).
   - Builds `:app:assembleRelease`, `:app:assembleDebug`, and `:app:bundleRelease`.
   - Uses repository secrets (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) matching `GothwadTech/browser` and `GothwadTech/launcher`.
   - Aligns APK with `zipalign`, signs with `apksigner`, verifies with `apksigner verify --verbose --print-certs`, signs Play Store AAB with `jarsigner`, prints the public Release SHA-256 fingerprint for `assetlinks.json` / D1 `ecosystem_apps`, creates `SHA256SUMS.txt`, uploads build artifacts, and publishes a GitHub Release via `softprops/action-gh-release@v2`.
   - Missing keystore secrets fail explicitly rather than generating throwaway release keys.

3. **JitPack (`jitpack.yml`)**:
   - Multi-module coordinates:
     - `com.github.Bansify.accountsapp:core:<tag>`
     - `com.github.Bansify.accountsapp:ui:<tag>`
   - Verify clean external resolution from `plusTube` / `storeapp` / `mailapp` before promoting a production tag.

## Owner pre-release checklist

- Add the 4 repository signing secrets in GitHub (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) using a dedicated `.jks` keystore for `com.gothwad.accounts` (keep existing published sibling apps on their existing keystores).
- Record the release APK's public SHA-256 certificate fingerprint (printed in `.github/workflows/release.yml` Job Summary) in `Bansify/accounts` D1 `ecosystem_apps.android_cert_sha256` (lowercase hex) and route `https://accounts.bansify.com/.well-known/assetlinks.json` directly to the `accounts` Worker outside `/api/*`.
- Confirm production Worker settings (`DEV_MODE = "false"`, `https://accounts.bansify.com` origins) before distributing the release APK externally.
