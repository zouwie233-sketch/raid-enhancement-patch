# README_INSTALL_0.9.1.0

Version:

`0.9.1.0-victory-bar-attach-guard-alpha`

## Purpose

This is a small BossBar stabilization candidate promoted from the verified 0.9.0.9 victory attach guard line.

It keeps the current verified behavior:

- `[REP]` independent BossBar remains the player-visible raid bar.
- First wave kill progress decreases normally.
- Next wave starts with a full refill.
- Same-wave refill suppression remains enabled.
- Dimension-safe cleanup remains enabled.
- Vanilla `event.minecraft.raid.victory.full` re-attach attempts are blocked by `VictoryBarAttachGuard`.
- The 0.9.0.3 `settlementKeyMode=raidInstance` fix is preserved.

## Install

1. Stop the game/server.
2. Remove the older `raid_enhancement_patch` JAR from the `mods` folder.
3. Put this file into the `mods` folder:

`raid_enhancement_patch-0.9.1.0-victory-bar-attach-guard-alpha.jar`

4. Start the game/server.
5. Confirm `latest.log` shows:

`Raid Enhancement Patch 0.9.1.0-victory-bar-attach-guard-alpha`

## Recommended clean test

Before testing, delete:

`config/raid_enhancement_patch/key_diagnostics.log`

Then run one raid and check/upload:

- `latest.log`
- `config/raid_enhancement_patch/key_diagnostics.log`

## Expected diagnostics

Look for:

- `version=0.9.1.0-victory-bar-attach-guard-alpha`
- `skipped-different-dimension`
- `VictoryBarAttachGuard`
- `victory-bar-attach-blocked` if vanilla victory BossBar attempts to rebind
- `victory-attach-guard-summary`
- `settlementKeyMode=raidInstance`

There should be no:

- `duplicate-blocked-by-history` in the current test session
- `@raidInstance:fallback`
- repeated reward settlement
- crash stacktrace from this mod
