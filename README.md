# Raid Enhancement Patch

Current version: `0.9.2.1-cross-save-lifecycle-isolation-alpha`

0.9.2.1 closes the cross-save persistence leak found by the A -> B -> A test. A concrete `MinecraftServer` owns one `RaidRuntimeContext`, and that context now owns both diagnostic isolation and a save-scoped lifecycle repository backed by the current save's Overworld `SavedData`. The old version-global `config/raid_enhancement_patch/raid_session_lifecycle.properties` file is never imported automatically. Raid composition, bounded spawn processing, safe-spawn validation, BossBar, settlement and VillageFavor behavior are unchanged.

It introduces one server-level tick coordinator and immutable runtime views for BossBar and battle-support consumers. It removes internal reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS` without changing gameplay rules.

## Current architecture-governance delivery

The legacy controllers still own runtime state in this stage. Later releases will migrate one subsystem at a time behind the new boundaries. Wave composition, rewards, VillageFavor, BossBar behavior and enabled Mixins remain compatible with 0.9.1.8. The new `config/raid_enhancement_patch/raid_spawn_queue.properties` file exposes only clamped execution, persistence and batch-diagnostic controls.
