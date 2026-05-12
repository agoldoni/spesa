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
- **`data/`** — Room database (`spesa.db`) with four entities: `MemberEntity`, `ProductEntity`, `ListItemEntity`, `FavoriteEntity`. Schema is at version 2. All entities use String UUID primary keys and an `updatedAt: Long` (epoch ms) used by the sync layer for last-write-wins conflict resolution. The DB is wired with `fallbackToDestructiveMigration()` — schema bumps drop the local data. `SpesaRepository` is the only write path: it owns the dedup logic ("adding a product already in the list increments its quantity"), the favorites toggle, refreshes `updatedAt` on every mutation and mirrors every change to the `SyncSource`. `ActiveMemberStore` persists the currently-selected member identity in `SharedPreferences`.
- **`sync/`** — `SyncSource` interface with one implementation, `MqttSyncSource` (HiveMQ Mqtt3 client). When `MqttConfig.isConfigured` is false the source stays disconnected and all push/delete calls are no-ops. Topics live under `sync/{groupId}/{members,products,list_items,favorites}/{id}`; messages are JSON-serialized entities (Gson) published with `retain=true`, while a delete is signaled by an empty payload on the same retained topic. Conflict resolution is last-write-wins via `updatedAt`. On connect the client subscribes to all four topic filters and republishes its full local state so other clients converge. `MqttConfig` persists broker host/port/credentials/groupId/TLS in `SharedPreferences` (`mqtt_config`).
- **`di/`** — Hilt `@SingletonComponent` module providing `AppDatabase`, the four DAOs and the `SyncSource` (always `MqttSyncSource`).
- **`ui/`** — Single Compose screen `ShoppingScreen` with one `ShoppingViewModel`. The header includes a gear icon that opens `MqttConfigActivity` (Compose Activity) for editing broker settings; saving the config calls `SyncSource.reconnectIfNeeded()`. Components: `MemberAvatar`, `QuantityStepper`, `FavoriteChip`. Theme uses Material 3 with primary `#1D9E75`.

**Key conventions:**
- UI language is Italian throughout (labels, dates, pluralization).
- Debug builds use application ID suffix `.debug` and label "Spesa (Debug)".
- Per-buildType launcher background colors: debug `#FF1565C0` (blue), release `#FF388E3C` (green).
- Quantities are integers ≥ 1; the stepper's `−` is disabled at 1. Removal uses the trash icon only — no "mark as done".
- Adding a product whose name (case-insensitive) already exists in `list_items` increments the existing quantity instead of inserting a duplicate. Lookup is by `nameKey = name.lowercase()` on `ProductEntity`.
- Two members are seeded on first launch: `M` (verde `#1D9E75`, id `m`) and `L` (blu `#1976D2`, id `l`). The active member persists in `SharedPreferences` (`spesa_prefs/active_member_id`).
- Dependencies are managed via version catalog in `gradle/libs.versions.toml`.
- Java 17 source/target compatibility, Kotlin 2.0, KSP for Room and Hilt code generation.

## MQTT sync (opt-in at runtime)

The sync layer is configured at runtime via the gear icon in the header (`MqttConfigActivity`). Required fields: broker host, port (default 8883), group id, TLS toggle; optional: username/password. The toggle "Sincronizzazione attiva" must be on. After saving, the client (re)connects and:
1. Subscribes to `sync/{groupId}/{kind}/#` for every `kind ∈ {members, products, list_items, favorites}`.
2. Republishes every local entity to its retained topic so freshly-joined peers converge.
3. Routes incoming messages through last-write-wins on `updatedAt`. Empty retained payloads are interpreted as delete signals.

Multiple installations sharing the same `groupId` (and pointing at the same broker) automatically synchronize members, products, list items and favorites in real time.

## Future work

- Drag & drop reordering of favorite chips (currently order is insertion order; reorder is exposed via `SpesaRepository.reorderFavorites` but no UI affordance yet).
- Stable per-install MQTT client identifier (currently a fresh UUID per session — fine for retained topics but breaks `cleanSession=false` semantics).
- Authentication / per-broker ACLs scoped to `groupId` (today the broker is trusted to enforce isolation).
