# README_STEP_9_0913_BOSSBAR_MODULE_BOUNDARY

## Version

`0.9.1.3-bossbar-module-boundary-alpha`

## Goal

Add diagnostic-only BossBar module boundary markers without changing player-visible BossBar behavior.

## Virtual boundary names

- `BossBarDisplayManager`: [REP] independent BossBar creation, title, visible state, player binding.
- `BossBarCleanupController`: completed / stopped / victory cleanup with same-dimension guard.
- `BossBarVanillaSuppressor`: vanilla Raid BossBar hide and victory rebind suppression.
- `BossBarAuditLogger`: diagnostic-only boundary fields.
- `VictoryBarAttachGuard`: behavior retained; blocks vanilla victory bar reattach.

## Explicit non-goals

- No real extraction of the state machine in this stage.
- No progress, waveChange, or baselineReset change.
- No key format normalization.
- No VillageFavor, reward, gift, wave, or persistence change.
- No ServerBossEventRaidTitleMixin restoration.
