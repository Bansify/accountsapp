# Releases, Publishing & Deployment

## Repository output split

| Repository | Output | Destination |
| --- | --- | --- |
| `GothwadTech/accounts` | Web pages (`/signin`, `/me`, `/privacy`, `/terms`) | Cloudflare Pages |
| `GothwadTech/accounts` | OAuth + Auth API Worker | Cloudflare Workers |
| `GothwadTech/accounts` | Schema migrations (`0001`+) | Existing Supabase / D1 DBs |
| `GothwadTech/accountsapp` | `:core` + `:ui` AARs, POMs, sources | Maven / JitPack (`com.github.GothwadTech.accountsapp:{core,ui}:<tag>`) |
| `GothwadTech/accountsapp` | Signed standalone Gothwad Accounts APK (`com.gothwad.accounts`) | GitHub Releases |

Android requires no separate Cloudflare backend deployment; both web and Android talk directly to `https://accounts.gothwadtech.com/api`.

## CI & Release automation

1. **`.github/workflows/android-ci.yml`** (PR/push, read-only permissions):
   - Validates docs & scaffold (`tools/check_docs.py`, `tools/check_android_scaffold.py`).
   - Provisions Android SDK 36 (`platforms;android-36`, `build-tools;36.0.0`) directly from `$ANDROID_HOME`.
   - Runs `:core:testDebugUnitTest`, `:ui:testDebugUnitTest`, lint across `:core`, `:ui`, `:app`, `:sample:alpha`, `:sample:beta`, debug APK builds, and `publishReleasePublicationToMavenLocal`.
   - Uploads debug APKs and HTML test/lint reports via `actions/upload-artifact@v4`.

2. **Future signed `release.yml`** (version tag / protected `release` environment):
   - Uses org secrets (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) matching sibling repositories (`tubeapp`, `storeapp`).
   - Verifies APK signature via `apksigner verify` and attaches SHA-256 checksums + APK to GitHub Releases.
   - Missing secrets fail the release job rather than generating ephemeral keys.

3. **JitPack (`jitpack.yml`)**:
   - Multi-module coordinates:
     - `com.github.GothwadTech.accountsapp:core:<tag>`
     - `com.github.GothwadTech.accountsapp:ui:<tag>`
   - Verify clean external resolution from `plusTube` / `storeapp` / `mailapp` before promoting a production tag.

## Owner pre-release checklist

- Configure protected `release` environment and grant org signing secrets (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) without ever committing secrets or debug fingerprints to production trust lists.
- Record public SHA-256 release signing certificate fingerprints in `BrokerTrustPolicy.PinnedSigners` (if apps use distinct release keys) or keep them on the shared org key (`SameSigningCertificate`).
- Register Android public OAuth clients + redirect URIs in `GothwadTech/accounts` and publish `/.well-known/assetlinks.json` for HTTPS App Link hosts.
- Confirm production Worker settings (`DEV_MODE` off, `https://accounts.gothwadtech.com` origins) before shipping consumer apps.
