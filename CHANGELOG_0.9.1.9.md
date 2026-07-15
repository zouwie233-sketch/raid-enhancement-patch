# Changelog 0.9.1.9-runtime-boundary-alpha

## Architecture

- Added `RaidTickCoordinator` as the only `LevelTickEvent` owner.
- Preserved the 0.9.1.8 post-tick order: villager protection, raid/settlement/BossBar, then battle support and golems.
- Added the zero-allocation `RaidRuntimeView` interface for BossBar access to the active native Raid handle.
- Added `VillageSecurityRuntimeView` for battle-support item lookup.
- Removed reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS`.
- Removed the unconfigured `ServerBossEventRaidTitleMixin` and its otherwise-unused title-override helper. Enabled Mixins are unchanged.
- Added a GitHub Actions architecture-contract check before Gradle build.

## Compatibility

- No wave-plan, spawn-composition, reward, VillageFavor, BossBar algorithm, configuration-key or persistence-format change.
- Minecraft remains 1.21.1, NeoForge remains 21.1.234, and Java remains 21.
- Raids Enhanced and fdlib dependency declarations are unchanged.

## Validation boundary

- Static runtime-boundary contract: passed locally.
- Local Gradle build: blocked before compilation because the local system Java is 11 and the project requires Java 21.
- GitHub Actions clean build and in-game regression testing remain required.
