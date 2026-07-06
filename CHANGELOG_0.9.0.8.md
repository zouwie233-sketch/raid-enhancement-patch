# CHANGELOG_0.9.0.8

## Version

`0.9.0.8-bossbar-victory-vanilla-final-suppress-alpha`

## Purpose

Fix the remaining 0.9.0.7 issue where the vanilla raid victory BossBar can reappear or rebind players after raid completion, creating a no-`[REP]` bar on screen.

## Changed

- Extended the same-dimension post-completion vanilla victory BossBar suppress window.
- Added denser final suppress checks for vanilla victory BossBars whose title matches raid victory strings such as `event.minecraft.raid.victory.full`.
- Added `final-victory-bar-suppress` diagnostics with before/after visibility and player counts.
- Added `vanilla-victory-reappeared` diagnostics when the vanilla victory bar is observed with players or visible state during the suppress window.
- Added `final-victory-suppress-summary`, `cleanup-stable-zero-confirmed`, and hard-window cleanup logs.
- Kept 0.9.0.7 dimension-safe cleanup behavior; cross-dimension cleanup must still log `skipped-different-dimension` and skip clearing.

## Preserved

- 0.9.0.3 `RaidInstanceKey` settlement fix.
- 0.9.0.5 `[REP]` independent BossBar visible-authority path.
- 0.9.0.7 dimension-safe cleanup guard.
- Same-wave upward refill suppression.
- `ServerBossEventRaidTitleMixin` remains disabled.

## Not changed

- `settlementKey`
- `RaidInstanceKey`
- `VillageFavor`
- villager gifts
- rewards
- raid wave count
- `RaidWaveAuthority`
- `RaidWaveExpansionController`
- baselineReset
- waveChange
- BossBar progress algorithm
- wave-8 crash guard
- persistent data migration

## Known status

Not yet game-tested by the user at the time of packaging.
