# README_INSTALL_0.9.1.2

Version: `0.9.1.2-key-audit-polish-alpha`

## Purpose

This is a small Key audit polish build. It preserves the tested 0.9.1.0 / 0.9.1.1 gameplay behavior and only makes key diagnostics easier to read.

## Install

1. Stop Minecraft / the server.
2. Remove the old `raid_enhancement_patch` jar from `mods`.
3. Copy this jar into `mods`:

`raid_enhancement_patch-0.9.1.2-key-audit-polish-alpha.jar`

4. For a clean diagnostic test, delete only:

`config/raid_enhancement_patch/key_diagnostics.log`

Do not delete:

- `key_diagnostics.properties`
- `key_diagnostics.default.properties`

## What changed

0.9.1.2 keeps the 0.9.1.1 read-only Key Service audit and adds clearer fields:

- `keyAuditPolish=0.9.1.2`
- `actualKeyFormatUnchanged=true`
- `keyFormatChange=false`
- `dimensionDuplicationAny=true/false`
- `dimensionDuplicationSummary=raidInstanceKey:...,villageKey:...,settlementKey:...,favorRecordKey:...`
- `dimensionDuplicationRecommendation=audit-only-defer-normalization-to-future-key-format-version` when duplicated dimension tokens are detected

## What this version intentionally does not do

- Does not normalize or rewrite key strings.
- Does not fix VillageKey center drift.
- Does not alter settlementKey behavior.
- Does not alter BossBar behavior.
- Does not alter `VictoryBarAttachGuard`.
- Does not alter VillageFavor, rewards, gifts, waves, or persistence.

## Test focus

Confirm 0.9.1.1 behavior did not regress and the new audit polish fields appear in `key_diagnostics.log`.
