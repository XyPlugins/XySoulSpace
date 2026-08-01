# AI Changelog

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
