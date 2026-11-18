# Bansify Accounts (Android)

Optional standalone **Bansify Accounts** app (`com.gothwad.accounts`) and shared Android Account SDK (`:core` + `:ui`) for the Bansify ecosystem (`Mail`, `Drive`, `Kiwi Store`, `PlusTube`, `Notes`, `Calendar`). Minimum Android API: 26.

## Overview

- **Zero mandatory central app**: Installing standalone `com.gothwad.accounts` is optional. Every Bansify app works independently on day one, in any install order, and keeps functioning if any other Bansify app is uninstalled.
- **Per-app hardware-backed credentials**: Each app holds its own OAuth client-scoped tokens (`EncryptedTokenStore` backed by Tink + AndroidKeyStore under `noBackupFilesDir`, strictly fail-closed). Refresh tokens are never shared across apps.
- **Distributed cross-app discovery**: With `BrokerPolicy.PeerDiscovery`, verified sibling Bansify apps exchange zero-token account directory snapshots (`AccountDirectoryProvider`) verified via `Binder.getCallingUid()` + `PackageManager` signing-certificate fingerprints so users see `"Continue as <name>"` across apps without duplicating sign-in friction.
- **FOSS / untrusted builds**: Setting `BrokerPolicy.Disabled` disables all cross-app IPC and falls back cleanly to Custom Tabs PKCE browser login.
- **Authentic web sign-in & shared M3 management UI**: Login/signup/passkeys/2FA/password updates happen inside existing `accounts.gothwadtech.com` web flows via Custom Tabs PKCE (`S256`). Native `:ui` provides the full 6-section account dashboard matching `accounts.gothwadtech.com/me` plus embeddable toolbar avatar buttons, switcher sheets, settings cards, and error recovery banners in English and Hindi.

## Modules

| Module | Responsibility |
| --- | --- |
| `:core` | OAuth PKCE client, `EncryptedTokenStore`, `SessionManager`, `PeerBrokerClient`, `AccountDirectoryProvider`, network-hop-safe `authInterceptor` (zero Compose dependency) |
| `:ui` | Shared Material 3 `BansifyAccountsScreen`, `AccountManagementActivity`, `BansifyAccountAvatarButton`, `BansifyAccountSwitcherSheet`, `BansifyAccountSettingsCard`, EN/HI strings |
| `:app` | Standalone `com.gothwad.accounts` launcher app |
| `:sample` | Multi-flavor (`alpha` + `beta`) host app demonstrating embedded SDK integration and cross-app discovery side by side |

## Quick Checks & Build

```sh
python3 tools/check_docs.py && python3 tools/check_android_scaffold.py

./gradlew \
  :core:testDebugUnitTest :ui:testDebugUnitTest \
  :core:lintDebug :ui:lintDebug :app:lintDebug :sample:lintAlphaDebug :sample:lintBetaDebug \
  :app:assembleDebug :sample:assembleAlphaDebug :sample:assembleBetaDebug \
  :core:publishReleasePublicationToMavenLocal :ui:publishReleasePublicationToMavenLocal
```

## Documentation

- [Project status & handoff](docs/HANDOFF.md)
- [Architecture & system boundaries](docs/ARCHITECTURE.md)
- [Cross-app discovery & token isolation](docs/DISCOVERY_AND_TOKEN_ARCHITECTURE.md)
- [Uninstall, upgrade & recovery matrix](docs/UNINSTALL_AND_RECOVERY_MATRIX.md)
- [Security model (STRIDE)](docs/SECURITY.md)
- [Peer Directory IPC protocol](docs/BROKER_PROTOCOL.md)
- [Public API](docs/PUBLIC_API.md)
- [Theme & color system](docs/THEME_SYSTEM.md)
- [Host integration & testing](docs/INTEGRATION.md)
- [Accounts backend PR plan](docs/SERVER_CHANGES.md)
- [Releases & JitPack publishing](docs/RELEASES.md)
