# Gothwad Accounts Theme & Color System

This document is the **single source of truth** for light and dark colors, surface hierarchy, and system bar integration in `:ui` (`GothwadAccountsTheme.kt`) and `:app`. Any future UI work or AI-generated changes **must** adhere to these exact tonal scales rather than introducing off-palette blue/gray backgrounds or hardcoded hex colors in composables.

---

## 1. Core Tonal Surfaces

### Light Theme (`#FFFFFF` Tonal Scale)
All light-mode surfaces are neutral steps derived from pure `#FFFFFF` so the app blends seamlessly with Android's light status bar and light 3-button navigation bar.

| Token | Hex | Usage |
| --- | --- | --- |
| `background` / `canvasBackground` | `#FFFFFF` | Root `Scaffold` canvas, behind status bar & 3-button navigation bar |
| `surface` | `#FFFFFF` | `AccountsTopAppBar`, `AccountsTabBar` container |
| `surfaceContainerLowest` | `#FFFFFF` | Inner spotlight cards (hero card inside Control Centre, Home search pill) |
| `surfaceContainerLow` | `#F8F8F8` | Primary grouped cards (`AccountSectionCard`, hero cards, quick-link tiles) |
| `surfaceContainer` | `#F3F3F3` | Floating dialogs/sheets (`AccountSwitcherDialog`), secondary blocks |
| `surfaceContainerHigh` | `#EDEDED` | Icon circle backdrops, inactive filter/scope pills, progress bar tracks |
| `surfaceContainerHighest` | `#E5E5E5` | High-emphasis neutral containers & pressed chips |
| `surfaceVariant` | `#F2F2F2` | Subtle neutral variant fills |
| `cardBorder` / `outlineVariant` | `#E6E6E6` | Hairline 1dp card strokes & `SectionDivider` |
| `outline` | `#757575` | Outlined button borders & field outlines |
| `onBackground` / `onSurface` | `#1A1A1A` | Primary headings & body text |
| `onSurfaceVariant` | `#5C5C5C` | Supporting copy, timestamps, subtle icons |

### Dark Theme (`#212121` Tonal Scale)
All dark-mode surfaces are neutral steps derived from `#212121` so the app blends seamlessly with Android's dark status bar and dark 3-button navigation bar.

| Token | Hex | Usage |
| --- | --- | --- |
| `background` / `canvasBackground` | `#212121` | Root `Scaffold` canvas, behind status bar & 3-button navigation bar |
| `surface` | `#212121` | `AccountsTopAppBar`, `AccountsTabBar` container |
| `surfaceContainerLowest` | `#1B1B1B` | Deep recessed surfaces & sunken badges |
| `surfaceContainerLow` | `#282828` | Primary grouped cards (`AccountSectionCard`, hero cards, quick-link tiles) |
| `surfaceContainer` | `#2E2E2E` | Floating dialogs/sheets (`AccountSwitcherDialog`), secondary blocks |
| `surfaceContainerHigh` | `#353535` | Icon circle backdrops, inactive filter/scope pills, progress bar tracks |
| `surfaceContainerHighest` | `#3F3F3F` | High-emphasis neutral containers & pressed chips |
| `surfaceVariant` | `#303030` | Subtle neutral variant fills |
| `cardBorder` / `outlineVariant` | `#383838` | Hairline 1dp card strokes & `SectionDivider` |
| `outline` | `#9E9E9E` | Outlined button borders & field outlines |
| `onBackground` / `onSurface` | `#F5F5F5` | Primary headings & body text |
| `onSurfaceVariant` | `#BDBDBD` | Supporting copy, timestamps, subtle icons |

---

## 2. Brand & Semantic Accents

| Role | Light Hex | Dark Hex | Usage |
| --- | --- | --- | --- |
| `primary` / `brandPrimary` | `#0B57D0` | `#A8C7FA` | Primary buttons, active links, `"G"` emblem primary arc |
| `primaryContainer` | `#E3EDFD` | `#0842A0` | Selected tab pills, avatar fallback container, hero glow |
| `verifiedGreen` / `verifiedContainer` | `#146C2E` / `#D6F3DE` | `#6DD58C` / `#163B22` | Verified emails, 2-Step On, passkeys, strong posture |
| `warningAmber` / `warningContainer` | `#B06000` / `#FEF3D6` | `#FCC934` / `#3D2E05` | Unverified recovery items, 75%+ quota, recoverable notices |
| `criticalRed` / `criticalContainer` | `#B3261E` / `#FCE8E6` | `#F28B82` / `#4A1C19` | Sign-out/remove actions, account deletion card, 90%+ quota |
| 4-Arc `"G"` Brand Emblem | `#B3261E`, `#B06000`, `#146C2E`, `#0B57D0` | `#F28B82`, `#FCC934`, `#6DD58C`, `#A8C7FA` | Top bar emblem & active account avatar ring |

---

## 3. System Bars & Edge-to-Edge Consistency

1. **Matching System Bars**: Both `MainActivity` and `AccountManagementActivity` configure `enableEdgeToEdge(...)` with light `#FFFFFF` and dark `#212121` `SystemBarStyle`, and `GothwadAccountsTheme` synchronizes `WindowCompat.getInsetsController` light/dark bar icon appearance plus `isNavigationBarContrastEnforced = false` so Android never overlays a mismatched gray scrim over `#FFFFFF` or `#212121`.
2. **No Status Bar Overlap**: `AccountsTopAppBar` applies `WindowInsets.statusBars` and top/horizontal `WindowInsets.displayCutout` padding **inside** its `#FFFFFF` / `#212121` `Surface`, ensuring the battery, signal, and clock bar never collides with app controls.
3. **No 3-Button Nav Overlap**: `Scaffold` uses `containerColor = semantic.canvasBackground` (`#FFFFFF` / `#212121`) and applies `bottom` + `horizontal` `safeDrawing` insets to screen content so lists scroll cleanly above the 3-button or gesture navigation bar.

---

## 4. Typography & Shapes

- **Display / Headings**: `Plus Jakarta Sans` (`res/font/plus_jakarta_sans.ttf`)
- **Body / Captions**: `Inter` (`res/font/inter.ttf`)
- **Shape Scale**: `small = 12.dp`, `medium = 20.dp`, `large = 24.dp`, `extraLarge = 28.dp` (pills use `RoundedCornerShape(50)`).
