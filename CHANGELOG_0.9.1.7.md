# Changelog 0.9.1.7

Version: `0.9.1.7-reflection-cache-hotfix-alpha`

## P0 server-thread hotfix

- Added `CachedReflection`, a shared compatibility-member cache with positive and negative caching for classes, methods, constructors and fields.
- Removed repeated reflection member discovery from `GolemBlockRollbackGuard`, `MobEffectCompat`, `BattleSupportEvents` and `VillagerProtectionEvents` tick paths.
- Replaced the global golem rollback lists with per-level work queues.
- Deduplicated rollback snapshots by triggering golem UUID; repeated damage now refreshes one task instead of creating overlapping snapshots.
- Added a hard per-level block-check budget and fair queue rotation.
- Added bounded cleanup-zone processing and coalescing by restored block position.
- Delays block restoration while the triggering golem still intersects the target block.
- Drops collision-blocked restoration after a bounded delay instead of sealing a golem back into terrain.
- Throttled protected-villager effect refreshes using existing refresh intervals.
- Added same-game-tick health-clamp maintenance suppression.
- Replaced the uncached optional `HEALTH_BOOST` removal search with `MobEffectCompat` cached lookup.

## Explicitly unchanged

- Raid wave counts and extra-wave trigger rules.
- BossBar progress, reset, visibility and victory-bar suppression.
- Settlement keys, RaidInstanceKey, VillageKey and settlement history.
- Rewards, XP, gifts, cooldowns and VillageFavor.
- Golem combat statistics and Raids Enhanced combat behavior.
- Enabled Mixin list and configuration values/defaults.

## Deferred root-cause follow-up

The custom extra-wave spawn-position algorithm still lacks full per-entity collision-box validation. That design defect is intentionally deferred to a separate safe-spawn version after this P0 performance hotfix is profiled and function-tested.
