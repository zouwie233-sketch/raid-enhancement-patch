# README_INSTALL_0.9.0.7

## Version

`raid_enhancement_patch-0.9.0.7-bossbar-dimension-safe-cleanup-alpha`

## Environment

- Minecraft 1.21.1
- NeoForge 21.1.234 or later in the 21.1.x line
- Java 21
- Optional compatibility targets: Raids Enhanced, fdlib

## Install

1. Back up the test world.
2. Remove older `raid_enhancement_patch-*.jar` files from `mods/`.
3. Put only `raid_enhancement_patch-0.9.0.7-bossbar-dimension-safe-cleanup-alpha.jar` into `mods/`.
4. Start the game once and confirm `latest.log` shows `0.9.0.7-bossbar-dimension-safe-cleanup-alpha`.
5. Keep KeyDiag enabled for this alpha test.

## KeyDiag settings

Check:

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

Recommended values:

```properties
enabled=true
log.raidDiscovery=true
log.settlement=true
log.favor=true
log.bossbar=true
bossbar.visibleAuthorityAudit.enabled=true
bossbar.visibleAuthorityAudit.temporaryRepTitleMarker=true
```

## What this version changes

0.9.0.7 is a narrow fix for the 0.9.0.6 cross-dimension cleanup regression.

It keeps the 0.9.0.5-confirmed `[REP]` independent BossBar path and the 0.9.0.6 cleanup goal, but adds a dimension guard:

- `cleanupInactive` may only clean a BossBar owned by the current ticking dimension.
- Nether/End ticks must not clear Overworld raid BossBars.
- cross-dimension cleanup attempts are logged as `skipped-different-dimension`.
- `suppressVanillaForCleanup` is also dimension-guarded.

## What this version does not change

This version does not modify:

- settlementKey / RaidInstanceKey
- VictorySettlementController
- VillageFavor
- villager gifts
- rewards
- raid wave count
- RaidWaveAuthority
- RaidWaveExpansionController
- wave-8 crash guard mixins
- persistent data
- ServerBossEventRaidTitleMixin enablement

## Test focus

1. Player can see the `[REP]` BossBar again.
2. First wave decreases after kills.
3. Second wave visually refills.
4. Second wave decreases after kills.
5. End/Nether ticks do not clean an Overworld BossBar.
6. Cross-dimension cleanup lines say `skipped-different-dimension`, not `inactive-detected`.
7. Raid completion removes the `[REP]` bar.
8. No no-`[REP]` vanilla BossBar remains after completion, or logs clearly explain its source.
9. `settlementKeyMode=raidInstance` still appears.
10. No `duplicate-blocked-by-history`.
11. No `@raidInstance:fallback`.
12. No repeated rewards.
13. No crash.
