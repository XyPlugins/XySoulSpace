# AI Changelog

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
