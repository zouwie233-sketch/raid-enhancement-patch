# README_INSTALL_0.9.1.2

Version: `0.9.1.2-key-audit-polish-alpha`

## Purpose

This is the 0.9.1.2 Key Service audit build. It preserves the tested 0.9.1.0 BossBar / settlementKey safety anchor and adds read-only key-boundary diagnostics.

## Install

1. Stop Minecraft / the server.
2. Remove the old `raid_enhancement_patch` jar from the `mods` folder.
3. Copy this jar into `mods`:

`raid_enhancement_patch-0.9.1.2-key-audit-polish-alpha.jar`

4. For a clean diagnostic test, delete only:

`config/raid_enhancement_patch/key_diagnostics.log`

Do not delete:

- `key_diagnostics.properties`
- `key_diagnostics.default.properties`

## What changed

Added source and compiled classes:

- `RaidKeyService`
- `KeyDebugService`

`RaidKeyDiagnostics` now appends read-only audit fields such as:

- `keyServiceStage=0.9.1.2-key-audit-polish-alpha`
- `keyAuditMode=readOnly-no-gameplay-behavior-change`
- `keyBoundarySummary=RaidInstanceKey(...);VillageKey(...);SettlementKey(...);FavorRecordKey(...)`
- `dimensionDuplication=false` or warning fields when a key appears to duplicate its dimension token

## What must not change

This build must not change:

- `settlementKeyMode=raidInstance`
- BossBar visibility / progress / refill behavior
- `VictoryBarAttachGuard`
- VillageFavor behavior
- rewards and villager gifts
- raid wave counts
- `RaidWaveAuthority` / `RaidWaveExpansionController`
- persistent data format
- `ServerBossEventRaidTitleMixin` enablement

## Test focus

Check that 0.9.1.0 gameplay behavior is unchanged and logs now contain key service audit fields.
