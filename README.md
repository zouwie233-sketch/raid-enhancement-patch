# Raid Enhancement Patch

Current version: `0.9.1.12-raid-registration-lifecycle-hotfix`

0.9.1.12 keeps the tested bounded persistent queue but fixes the ground-raider commit path: the entity enters the world exactly once, then vanilla `Raid#joinRaid` performs membership-only registration. The server stopping event checkpoints active batches, and the stopped event clears process-local raid/session/read-model mirrors so a later integrated-server start must validate and restore the sidecar. Existing safe-position validation remains authoritative.

It introduces one server-level tick coordinator and immutable runtime views for BossBar and battle-support consumers. It removes internal reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS` without changing gameplay rules.

## Current architecture-governance delivery

The legacy controllers still own runtime state in this stage. Later releases will migrate one subsystem at a time behind the new boundaries. Wave composition, rewards, VillageFavor, BossBar behavior and enabled Mixins remain compatible with 0.9.1.8. The new `config/raid_enhancement_patch/raid_spawn_queue.properties` file exposes only clamped execution, persistence and batch-diagnostic controls.
