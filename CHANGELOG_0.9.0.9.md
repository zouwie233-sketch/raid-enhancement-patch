# CHANGELOG 0.9.0.9

## Version

`0.9.0.9-victory-bar-attach-guard-alpha`

## Changed

- Added a post-completion `VictoryBarAttachGuard` around the vanilla Raid victory BossBar.
- The guard keeps a short same-dimension reference to the vanilla victory BossBar after the managed `[REP]` entry is removed.
- If the vanilla `event.minecraft.raid.victory.full` BossBar becomes visible again or rebinds players, the guard hides it and removes players again.
- Added diagnostic phases:
  - `victory-attach-guard-start`
  - `victory-bar-attach-blocked`
  - `victory-attach-guard-stable-zero`
  - `victory-attach-guard-summary`
  - `victory-attach-guard-skipped-different-dimension`

## Preserved

- 0.9.0.3 RaidInstanceKey settlement fix.
- 0.9.0.5 `[REP]` independent BossBar visibility diagnostics.
- 0.9.0.7 dimension-safe cleanup.
- 0.9.0.8 final victory suppress logs.

## Not changed

- settlementKey / RaidInstanceKey.
- VillageFavor.
- rewards.
- villager gifts.
- raid wave counts.
- RaidWaveAuthority.
- RaidWaveExpansionController.
- baselineReset.
- waveChange.
- core BossBar progress algorithm.
- wave-8 crash guard mixins.
- persistent data.
- ServerBossEventRaidTitleMixin remains disabled.

## Known issues

- This is not a stable release.
- VillageFavor center/key drift remains a separate future issue and is not fixed here.
