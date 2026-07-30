# Raid Enhancement Patch

Current version: `0.9.1.10-bounded-spawn-queue-alpha`

0.9.1.10 keeps the 0.9.1.9 runtime boundary and fixes partial high-pressure wave spawning with a bounded retry queue. Existing safe-position validation remains authoritative.

It introduces one server-level tick coordinator and immutable runtime views for BossBar and battle-support consumers. It removes internal reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS` without changing gameplay rules.

## Current architecture-governance delivery

The legacy controllers still own runtime state in this stage. Later releases will migrate one subsystem at a time behind the new boundaries. Configuration keys, persistence files, wave composition, rewards, VillageFavor, BossBar behavior and enabled Mixins remain compatible with 0.9.1.8; only patch-owned spawn execution is distributed across bounded server ticks.
