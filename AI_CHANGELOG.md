# AI Changelog

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
