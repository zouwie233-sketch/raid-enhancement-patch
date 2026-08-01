# Raid Enhancement Patch

Current version: `0.9.2.0-architecture-runtime-context-alpha`

0.9.2.0 begins the approved architecture roadmap with a server-scoped runtime lifecycle boundary. A concrete `MinecraftServer` now owns one `RaidRuntimeContext`; the first migrated state is diagnostic-only so the tested raid wave, queue, registration, safe-spawn, BossBar, settlement and VillageFavor behavior stays unchanged. Starting, stopping, checkpoint and close transitions are explicit, and the diagnostic rate-limit map no longer crosses integrated-server sessions.

It introduces one server-level tick coordinator and immutable runtime views for BossBar and battle-support consumers. It removes internal reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS` without changing gameplay rules.

## Current architecture-governance delivery

The legacy controllers still own runtime state in this stage. Later releases will migrate one subsystem at a time behind the new boundaries. Wave composition, rewards, VillageFavor, BossBar behavior and enabled Mixins remain compatible with 0.9.1.8. The new `config/raid_enhancement_patch/raid_spawn_queue.properties` file exposes only clamped execution, persistence and batch-diagnostic controls.
