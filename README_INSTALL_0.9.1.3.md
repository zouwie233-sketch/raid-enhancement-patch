# README_INSTALL_0.9.1.3

Version: `0.9.1.3-bossbar-module-boundary-alpha`

## Purpose

This is a BossBar module boundary audit build. It preserves the tested 0.9.1.0 BossBar / settlementKey safety anchor and the 0.9.1.2 Key audit polish fields, then adds diagnostic-only BossBar boundary markers.

## Install

1. Stop Minecraft / the server.
2. Remove the old `raid_enhancement_patch` jar from `mods`.
3. Copy this jar into `mods`:

`raid_enhancement_patch-0.9.1.3-bossbar-module-boundary-alpha.jar`

4. For a clean diagnostic test, delete only:

`config/raid_enhancement_patch/key_diagnostics.log`

Do not delete:

- `key_diagnostics.properties`
- `key_diagnostics.default.properties`

## What changed

0.9.1.3 adds diagnostic-only BossBar boundary fields:

- `bossBarBoundaryStage=0.9.1.3-bossbar-module-boundary-alpha`
- `bossBarAuditMode=boundary-only-no-gameplay-behavior-change`
- `bossBarBehaviorUnchanged=true`
- `bossBarProgressAlgorithmChanged=false`
- `waveChangeChanged=false`
- `baselineResetChanged=false`
- `settlementKeyChanged=false`
- `keyFormatChanged=false`
- `serverBossEventRaidTitleMixinRestored=false`
- named boundary summaries for display, cleanup, vanilla suppress, audit logging, and VictoryBarAttachGuard.

## What this version intentionally does not do

- Does not change BossBar progress math.
- Does not change `waveChange` or `baselineReset`.
- Does not change `VictoryBarAttachGuard` behavior.
- Does not change `settlementKey`, `RaidInstanceKey`, `VillageKey`, or key formats.
- Does not fix `dimensionDuplicationAny`.
- Does not fix VillageKey center drift.
- Does not change VillageFavor, rewards, villager gifts, raid waves, persistence, or the disabled title mixin.

## Test focus

Confirm 0.9.1.2 behavior did not regress and the new BossBar boundary audit fields appear in `key_diagnostics.log`.
