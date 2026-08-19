# AI Changelog

## 1.1.13

- Fixed duplicate delivery when one player pickup dispatches both Bukkit pickup event variants on 1.12.2.
- Routed `EntityPickupItemEvent` and `PlayerPickupItemEvent` through one ownership/deposit path.
- Added a bounded 40-tick consumed-entity claim to cancel the second event after a successful deposit.
- Kept automatic pickup MythicMobs-only and event-driven; no nearby ground scan was reintroduced.
- Updated release metadata and user/AI documentation to 1.1.13.

## 1.1.12

- Restricted automatic storage delivery to final drops from MythicMobs `MythicMobDeathEvent` and configured `ssdrops`.
- Removed ordinary Bukkit death matching, `ItemSpawnEvent` correlation and all nearby-player ground-item scanning.
- Leaves ordinary mob drops, player-thrown items, naturally spawned items and every other unowned ground entity to vanilla or other plugins.
- Makes `PlayerPickupItemEvent` return immediately unless the Item entity has an MM ownership record.
- Preserved the owned-drop delay, original-killer protection, bounded per-tick queue, notification coalescing and full ItemStack/NBT delivery.
- Restores or physically drops any MM event stack that cannot enter the ownership path, preventing silent item loss.
- Removed `pickup.range` and `pickup.scan-interval-ticks` from the default configuration; legacy keys are ignored.
- Updated release metadata and user/AI documentation to 1.1.12.

## 1.1.11

- Fixed player-facing withdrawal messages showing a namespaced XyItems ID even when the ItemStack has an actual custom display name.
- Changed normal `id`, `name` and `display` modes to prefer the real custom name, then fall back to a custom library ID or vanilla friendly name as appropriate.
- Preserved strict namespaced output through the explicit `raw-id` diagnostic mode.
- Applied the shared name selection behavior to withdrawal, manual deposit, soul-shop and auto-pickup messages without changing item data or storage identity.
- Added event-driven mob-drop ownership tracking: a bounded death-batch cache correlates the final death drops with nearby `ItemSpawnEvent` entities by world, position, complete ItemStack similarity and remaining amount.
- Bound matched entities to the original killer so leaving the configured scan range cannot transfer the drop to another nearby player's auto pickup.
- Added one server-wide due queue which processes only due owned drops every tick; no per-player or per-item scheduler is created and the existing range scan interval is unchanged.
- Bounded actual owned-drop deposits to `pickup.max-owned-pickups-per-tick` (default 32, clamped 1-512); excess due entries remain queued for later ticks instead of creating a main-thread burst.
- Added configurable `pickup.mob-drop-delay-ticks` with a clamped 1-200 range and a 10-tick default, preserving the visible ground-drop effect before storage delivery.
- Protected owned drops from another player's pickup, hopper/entity pickup and cross-owner merging while ownership is active; failed delivery releases ownership without deleting the entity.
- Changed MythicMobs `ssdrops` from immediate direct deposit to an owned ground entity using the same delay and delivery path.
- Kept the death matcher bounded to 2 seconds and 2048 pending death batches; owned records have a 200-tick safety expiry when another plugin extends pickup delay.
- Ignored cancelled death batches, consumed spawn matches from the earliest spawn priority, and released remaining entity pickup delays when the plugin is disabled.
- Treats an `ssdrops` spawn attempt consumed or cancelled by another item plugin as handled, preventing a fallback second spawn and duplicate delivery.
- Replaced per-byte `String.format` calls in storage-key SHA-256 encoding with an equivalent lowercase hex lookup, preserving every existing key while reducing allocation in the deposit hot path.
- Kept already-ready entities owned while they wait behind the per-tick budget; the 200-tick safety expiry now applies only while another plugin keeps `pickupDelay` above zero.
- Added eleven regression tests: four for item-name selection, six for death-drop matching/queue behavior and one standard SHA-256 compatibility vector. The suite now contains 16 tests.
- Updated release metadata and user/AI documentation to 1.1.11.

## 1.1.10

- Added a slot `51` auto-pickup control with enabled, disabled and globally-disabled presentations; another player's admin view only permits pagination and closing.
- Centralized permission, global-setting and per-player-setting checks across `PlayerPickupItemEvent`, the bounded nearby-entity scan and MythicMobs `ssdrops`.
- Changed ground-item handling to deposit first and remove the entity only after a successful storage write; failed deposits leave existing entities untouched.
- Routes failed or disabled MythicMobs `ssdrops` delivery back to the mob death location.
- Added XyCore-backed creation for `xyitems:*`, `mythicmobs:*` and `minecraft:*` drop IDs while preserving legacy bare MythicMobs IDs.
- Added standalone Bukkit/MythicMobs fallbacks for `minecraft:*` and `mythicmobs:*`, plus fail-closed validation for invalid amounts, non-finite chances and out-of-range probabilities.
- Replaced per-death MythicMobs configuration traversal with an immutable rule snapshot built at startup and reload.
- Added configurable, bounded notification coalescing keyed by item ID and actual display name, preserving quality names and colors in player messages.
- Retired the separate MythicMobs and legacy pickup-message keys in favor of the unified `pickup.notification-*` settings; legacy files remain loadable without duplicate notices.
- Uses the XyCore player prefix when available and the local XySoulSpace prefix as the standalone fallback.
- Batches open-GUI refreshes per scan tick and continues to use one server-wide pickup scan task rather than per-player tasks.
- Reload now refreshes the XyCore bridge and cached MythicMobs rules, then cancels and recreates the single scanner with the latest interval.
- Replaced configurable-title GUI detection with a private holder session, blocked drag injection, and kept owner/page/key state scoped to each window.
- Added revision-aware storage snapshots and serialized repository IO so a concurrent autosave cannot clear newer pickup mutations; reload now also applies the latest autosave interval.
- Clamped range scans to 64 blocks and aligned OP behavior with command permission handling.
- Added tests covering pickup defaults, dirty-state persistence, isolation between player storage instances and stale-save revision handling.
- Updated version metadata, default configuration and all user/AI documentation to 1.1.10.

## 1.1.9

- Restored the legacy Shift-left deposit-all semantics: all matching items in player inventory slots `0..35` are deposited, rather than only the clicked stack.
- Matches candidates through `ItemKeys.keyOf`, preserving the storage system's complete ItemStack identity and legacy internal-lore cleanup behavior.
- Uses the current view's bottom inventory for lower-inventory click validation on Paper/Spigot 1.12.2.
- Keeps the one-tick revalidation and per-player pending guard from 1.1.8.
- Performs one bounded 36-slot scan and one storage/save/message/refresh sequence per Shift-left action; no repeating task was added.
- Updated version metadata and user documentation to 1.1.9.

## 1.1.8

- Moved manual lower-inventory deposits out of the cancelled `InventoryClickEvent` body and into a one-tick scheduled task.
- Added a per-player pending-deposit guard for manual GUI deposits.
- Revalidates the clicked slot on execution with `ItemStack#isSimilar` before mutating inventory or storage.
- Keeps deposit controls unchanged: left = 1, right = up to 64, shift-left = clicked stack.
- Added centralized stripping of trailing Chinese full stops from `Text.sendRaw` and `Text.sendLocalRaw`.
- Updated default config messages and version metadata to 1.1.8.

## 1.1.7

- Added click-to-deposit from the lower player inventory while a SoulSpace GUI is open.
- Deposit controls: left click deposits 1, right click deposits up to 64, shift-left deposits the whole stack; shift-right is ignored.
- Kept top inventory slots cancelled and protected; withdrawal only happens from a top slot when the cursor is empty.
- Restored action lore display by default while keeping it isolated to GUI display copies.
- Changed player-facing item-name fallback order so custom display names win over vanilla material names when provider identification falls back to `minecraft:*`.
- Kept drag-to-deposit out of scope to preserve a simple and predictable interaction model.

## 1.1.6

- Added a generated `VanillaMaterialNames` utility with 463 Bukkit 1.12.2 base `Material` display names.
- Switched `XySoulSpacePlugin#vanillaDisplayName` from a tiny built-in map to the generated full map.
- Kept `messages.vanilla-names` as the highest-priority override layer.
- Corrected several non-inventory/legacy base names for player-facing readability, including water, lava, bed, redstone components and potion variants.
- Kept runtime behavior lightweight: one `Material.name()` lookup and one read-only `HashMap#get` per player-facing vanilla item message.
- Did not change storage format, autosave behavior, XyCore matching, XyItems/MythicMobs ID display, or GUI safety changes from 1.1.4.

## 1.1.5

- Added vanilla material display-name fallback for player-facing item messages.
- Added `messages.vanilla-names` config with configurable `Material -> display name` mappings.
- Changed `item-name-mode: id` semantics for player messages: custom providers still show namespaced IDs, while vanilla minecraft stacks prefer configured/built-in friendly names.
- Added `raw-id` mode for strict namespaced ID debugging.
- Added a small built-in 1.12.2 vanilla material name map for common items, with config overrides taking precedence.
- Kept storage format, GUI behavior, XyCore matching and autosave behavior unchanged.

## 1.1.4

- Restricted quick-store to PlayerInventory slots `0..35` instead of using `getContents()`, preventing armor, offhand and client-mapped equipment slots from being stored.
- Added per-viewer GUI slot-to-storage-key mapping in `SoulSpaceGui`; withdrawal now uses the original stored template from `SoulStorage#getItem`.
- Removed visible internal GUI lore as a withdrawal dependency and added config toggles for amount/action/key lore display.
- Added defensive cleanup of legacy internal GUI lore before hashing and storing item templates.
- Added `XySoulSpacePlugin#itemDisplayName` and XyCore-backed item-id display for player-facing item messages.
- Updated command, pickup, MythicMobs bridge and shop messages to use the new item display rule.
- Added XyItems as a soft dependency so XyCore item providers are normally registered before XySoulSpace resolves item IDs.
- Kept the storage format, autosave behavior and API transaction model unchanged.

## 1.1.3

- Split XySoulSpace chat prefix routing by message semantics.
- Player gameplay results still use XyCore prefix when available; administrative help, permission, usage, reload, status and admin feedback use the local XySoulSpace prefix.
- Added `Text.sendLocal` and `Text.sendLocalRaw` for explicit local-prefix command feedback.
- Kept the soft dependency and standalone fallback behavior unchanged.
- Did not change storage, shop, pickup, MythicMobs bridge or batch material transaction behavior.

## 1.1.2

- Added unified player-chat prefix resolution: prefer XyCore `getMessagePrefix()` when XyCore is enabled.
- Preserved standalone behavior by falling back to XySoulSpace `messages.prefix` when XyCore is missing, disabled, or too old.
- Routed scattered player messages through `Text.sendRaw`, including commands, auto pickup, MythicMobs drops, soul shop, decomposition, and item-library feedback.
- Kept console/log output under the XySoulSpace plugin name.
- Continued to target Java 8 and Paper/Spigot 1.12.2 only.

## 1.1.1

- Added complete namespaced item-id counting backed by XyCore 0.3.10 matching.
- Added all-or-nothing batch withdrawal planning under the storage synchronization boundary.
- Added immutable withdrawal receipts containing the exact removed ItemStack templates and amounts.
- Added percentage refunds with deterministic floor rounding.
- Kept the old cost-key methods for existing shop behavior; new XyForgeCrafting code must not use display-name matching.
- Kept XyCore as a soft dependency for standalone soul-space behavior, while the new namespaced transaction API safely reports no matches when XyCore is unavailable.
- Continued to target Java 8 and Paper/Spigot 1.12.2 only.

## 1.1

- Rebuilt the legacy SoulSpace jar as a maintainable XY-series project.
- Renamed plugin package to `org.xyplugin.xysoulspace`.
- Standardized the command surface around `/xyss`.
- Replaced immediate save-on-every-pickup behavior with cached local YAML storage and dirty autosaves.
- Added `XySoulSpaceApi` for future XyForge, strengthening, exchange and activity plugins.
- Added `XySoulSpaceItemDepositEvent`.
- Reimplemented soul storage GUI, pickup, item library, decomposition and basic shop features.
- Added soft XyCore detection while keeping YAML as the default storage backend.
- Added reflection-based MythicMobs `ssdrops` compatibility.
- Added clear README usage, config, command and upgrade notes.
