# Raid Enhancement Patch

Current version: `0.9.1.11-raid-spawn-persistence-beta`

0.9.1.11 keeps the tested bounded retry queue and persists its remaining work through the existing raid lifecycle sidecar. Validated batches resume after server restart without rebuilding already-checkpointed completed slots. Existing safe-position validation remains authoritative.

It introduces one server-level tick coordinator and immutable runtime views for BossBar and battle-support consumers. It removes internal reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS` without changing gameplay rules.

## Current architecture-governance delivery

The legacy controllers still own runtime state in this stage. Later releases will migrate one subsystem at a time behind the new boundaries. Wave composition, rewards, VillageFavor, BossBar behavior and enabled Mixins remain compatible with 0.9.1.8. The new `config/raid_enhancement_patch/raid_spawn_queue.properties` file exposes only clamped execution, persistence and batch-diagnostic controls.
