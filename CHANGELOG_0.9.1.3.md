# CHANGELOG 0.9.1.3

Version: `0.9.1.3-bossbar-module-boundary-alpha`

## Type

Low-risk BossBar module boundary audit build.

## Added

- Added `BossBarAuditLogger` as a diagnostic-only boundary helper.
- Added BossBar boundary audit fields to BossBar authority / cleanup / victory attach guard logs.
- Added explicit behavior-unchanged markers:
  - `bossBarBehaviorUnchanged=true`
  - `bossBarProgressAlgorithmChanged=false`
  - `waveChangeChanged=false`
  - `baselineResetChanged=false`
  - `settlementKeyChanged=false`
  - `keyFormatChanged=false`
  - `serverBossEventRaidTitleMixinRestored=false`

## Preserved

- 0.9.1.0 BossBar / settlementKey behavior.
- 0.9.1.2 Key audit polish fields.
- `[REP]` independent BossBar path.
- Same-wave refill suppression.
- Dimension-safe cleanup.
- `VictoryBarAttachGuard` vanilla victory bar rebind suppression.
- `settlementKeyMode=raidInstance` behavior.

## Not changed

- No BossBar progress algorithm change.
- No waveChange / baselineReset change.
- No key format normalization.
- No `dimensionDuplicationAny` fix.
- No VillageKey drift fix.
- No VillageFavor, reward, gift, wave, persistence, or Mixin behavior change.
