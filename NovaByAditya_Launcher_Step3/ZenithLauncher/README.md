# Nova by Aditya (launcher)

Native Kotlin + Jetpack Compose Android launcher. App ID `com.aditya.nova`, min SDK 26, target 34.
Built in steps following *Nova by Aditya – Complete Launcher Plan*. Status is listed honestly below.

## Feature matrix
| Feature | Status |
|---|---|
| Home role (default launcher), Home-button return | Working (needs device test) |
| App discovery, launch, live refresh on install/remove | Working (needs device test) |
| App drawer: search (name/initials), A–Z scroller, web fallback | Working |
| Loading / error+retry / empty states for app list | Working |
| Dock (up to 5, configurable) | Working |
| Folders: create, rename, add/remove, delete, survive missing apps | Working (Step 1) |
| Theme engine (token based), 12 themes: Obsidian Ultra, Cyberpunk X, Titanium, Arctic Glass, Matrix, Deep Space, Old Money, Liquid Chrome, Synthwave, Crimson, Aurora, Hologram OS | Working (Steps 1 and 3) |
| Theme tokens include colours, corner radius, font family and accent-edge glow; all 12 pass WCAG 4.5:1 text contrast | Working (Step 3) |
| Icon style (original / monochrome), clock styles (light / bold / compact), wallpaper dimming | Working (Step 3) |
| High-contrast panels, reduce motion, press feedback, springy drawer transition | Working (Step 3) |
| Starter layouts: Minimal, Productivity, Gaming (onboarding + Settings) | Working (Step 3); results depend on installed apps |
| Live theme preview with Apply / Cancel, accent: theme / wallpaper / custom | Working (Step 1) |
| Onboarding (welcome, theme, default Home) | Working (Step 1) |
| Reduce motion, haptics switch, version info, feedback, reset | Working (Step 1) |
| Versioned local storage (schema 2) | Working |
| Wallpaper picker | Working (system chooser) |
| Gestures: swipe up/down, long-press empty space = edit Home | Working; configurable gestures planned (Step 4) |
| Home grid: apps and folders placed per cell, up to 5 pages | Working (Step 2) |
| Edit mode: drag to move/swap, remove, Undo, Cancel, Done, add/remove page | Working (Step 2) |
| Grid size (3–6 columns, 4–7 rows), items reflow when it shrinks | Working (Step 2) |
| Layout persistence (one atomic write, schema 3), missing apps pruned at launch | Working (Step 2) |
| Moving an item between pages | Via item menu (drag across pages not supported) |
| Icon packs, per-app icon override | Planned (Step 4) |
| Backup / restore, theme import/export | Planned (Step 4) |
| Scheduled themes, notification dots | Planned (Step 4) |
| Real Android widgets (AppWidgetHost) | Planned (Step 5) |
| Usage insights (opt-in) | Planned (Step 5) |
| Accessibility audit, release signing, device matrix | Planned (Step 6) |

## Build
1. Android Studio: open folder, sync, Build > Build APK(s). Or `gradle assembleRelease` (Gradle 8.7, JDK 17).
2. GitHub: push the folder; `.github/workflows/build.yml` uploads the APK as an artifact.

The release build is signed with the debug key for easy install. Use a protected signing key for public release.
