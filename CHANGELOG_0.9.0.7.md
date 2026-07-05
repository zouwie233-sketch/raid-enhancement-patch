# CHANGELOG_0.9.0.7

## raid_enhancement_patch-0.9.0.7-bossbar-dimension-safe-cleanup-alpha

### Status

Alpha test build. Not stable.

### Main purpose

Fix the 0.9.0.6 regression where BossBar cleanup could run from the wrong dimension and remove an active Overworld raid BossBar during an End tick.

### Fixed / changed

- Added a dimension guard to `cleanupInactive`.
- Added a dimension guard to `suppressVanillaForCleanup`.
- Cross-dimension cleanup attempts now log `skipped-different-dimension` instead of `inactive-detected`.
- Nether/End ticks should no longer mark Overworld REP BossBars inactive.
- Kept `[REP]` temporary BossBar marker.
- Kept `BossBarCleanupAudit` logging.
- Kept same-wave upward refill suppression from 0.9.0.6.
- Kept completion cleanup goal from 0.9.0.6, but constrained it to the owning dimension.

### Preserved from previous versions

- 0.9.0.3 RaidInstanceKey settlement fix.
- 0.9.0.5 visible-authority audit path.
- Independent `[REP]` BossBar path.
- Raids Enhanced compatibility mixins.
- Wave-8 crash guard mixins.

### Not changed

- settlementKey / RaidInstanceKey
- VillageFavor
- rewards
- villager gifts
- raid wave counts
- RaidWaveAuthority
- RaidWaveExpansionController
- BossBar progress algorithm
- persistent data
- `ServerBossEventRaidTitleMixin` remains disabled

### Known risk

0.9.0.7 still needs in-game validation. The expected outcome is that 0.9.0.5 visual behavior returns while 0.9.0.6 cleanup no longer misfires across dimensions.
