# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (requires signing env vars)
./gradlew clean                  # Clean build artifacts
./install-all.sh                 # Install debug APK on all connected devices
./install-all.sh --build         # Build + install
```

Release signing requires env vars: `KEYSTORE_FILE` (default `~/.android/release-key.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (default `release`), `KEY_PASSWORD`.

There are no tests configured in this project.

## Architecture

Single-module Android app (`app/`) using MVVM with Jetpack Compose. Package: `it.agoldoni.spesa`.

**Layers:**
- **`data/`** — Room database (`spesa.db`) with four entities: `MemberEntity`, `ProductEntity`, `ListItemEntity`, `FavoriteEntity`. Schema is at version 1. All entities use String UUID primary keys to keep parity with Firebase keys. `SpesaRepository` is the only write path: it owns the dedup logic ("adding a product already in the list increments its quantity"), the favorites toggle, and mirrors every change to the configured `SyncSource`. `ActiveMemberStore` persists the currently-selected member identity in `SharedPreferences`.
- **`sync/`** — `SyncSource` interface with two implementations: `LocalOnlySyncSource` (no-op, used when Firebase is not configured) and `FirebaseSyncSource` (Realtime Database under `/spesa/default/{members,products,list_items,favorites}`). The DI module picks the impl based on `BuildConfig.FIREBASE_ENABLED`, which is wired automatically based on the presence of `app/google-services.json` at build time.
- **`di/`** — Hilt `@SingletonComponent` module providing `AppDatabase`, the four DAOs and the `SyncSource`.
- **`ui/`** — Single Compose screen `ShoppingScreen` with one `ShoppingViewModel`. Components: `MemberAvatar`, `QuantityStepper`, `FavoriteChip`. Theme uses Material 3 with primary `#1D9E75`.

**Key conventions:**
- UI language is Italian throughout (labels, dates, pluralization).
- Debug builds use application ID suffix `.debug` and label "Spesa (Debug)".
- Per-buildType launcher background colors: debug `#FF1565C0` (blue), release `#FF388E3C` (green).
- Quantities are integers ≥ 1; the stepper's `−` is disabled at 1. Removal uses the trash icon only — no "mark as done".
- Adding a product whose name (case-insensitive) already exists in `list_items` increments the existing quantity instead of inserting a duplicate. Lookup is by `nameKey = name.lowercase()` on `ProductEntity`.
- Two members are seeded on first launch: `M` (verde `#1D9E75`, id `m`) and `L` (blu `#1976D2`, id `l`). The active member persists in `SharedPreferences` (`spesa_prefs/active_member_id`).
- Dependencies are managed via version catalog in `gradle/libs.versions.toml`.
- Java 17 source/target compatibility, Kotlin 2.0, KSP for Room and Hilt code generation.

## Firebase setup (optional)

The Firebase RTDB sync is opt-in. To enable it, drop a valid `app/google-services.json` (download from the Firebase console for app id `it.agoldoni.spesa` or `it.agoldoni.spesa.debug`) into `app/`. The Gradle script detects the file's presence and:
1. Applies the `com.google.gms.google-services` plugin
2. Sets `BuildConfig.FIREBASE_ENABLED = true` so Hilt provides `FirebaseSyncSource` instead of the no-op
3. `app/google-services.json` is gitignored — never committed

Without the file, the app builds and runs local-only.

## Future work

- Drag & drop reordering of favorite chips (currently order is insertion order; reorder is exposed via `SpesaRepository.reorderFavorites` but no UI affordance yet).
- Multi-household support (currently the Firebase root is hardcoded to `spesa/default`).
- Authentication / per-user RTDB rules (current schema assumes the database is private to the household).
