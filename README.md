# Raid Enhancement Patch

Current version: `0.9.1.9-runtime-boundary-alpha`

0.9.1.9 is the first behavior-preserving architecture foundation based on the user-tested 0.9.1.8 safe-spawn alpha.

It introduces one server-level tick coordinator and immutable runtime views for BossBar and battle-support consumers. It removes internal reflection into `RaidExtraWaveController.STATES` and `VillageSecurityController.SESSIONS` without changing gameplay rules.

## Current architecture-governance delivery

The legacy controllers still own runtime state in this stage. Later releases will migrate one subsystem at a time behind the new boundaries. Configuration keys, persistence files, wave plans, spawning, rewards, VillageFavor, BossBar behavior and enabled Mixins remain compatible with 0.9.1.8.
