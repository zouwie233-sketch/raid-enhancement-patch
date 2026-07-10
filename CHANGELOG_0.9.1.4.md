# CHANGELOG 0.9.1.4

Version: `0.9.1.4-victory-settlement-boundary-audit-alpha`

## Type

Low-risk victory-settlement boundary audit. No gameplay behavior change intended.

## Added

- Immutable diagnostic projection: `RaidCompletionResult`.
- Diagnostic-only boundary helper: `VictorySettlementBoundaryAudit`.
- A concise `victory-settlement-boundary` audit record at:
  - `before-history-check`;
  - `accepted-before-rewards`.
- One-snapshot visibility for:
  - raid instance key;
  - village key;
  - dimension and center;
  - victory outcome;
  - eligible player UUIDs;
  - omen level;
  - total waves;
  - completion game time.

## Boundary status

In 0.9.1.4, `RaidCompletionResult` is an audit projection only. Existing systems still consume their previous arguments and state. The new record is not allowed to decide settlement, rewards, favor, cleanup, keys, or persistence.

## Preserved

- 0.9.1.3.1 BossBar diagnostic throttle.
- `[REP]` BossBar behavior and VictoryBarAttachGuard.
- `settlementKeyMode=raidInstance` duplicate prevention.
- Reward order and reward values.
- VillageFavor write timing and values.
- Real key formats and `dimensionDuplicationAny` technical-debt status.
- Raid wave logic and persistent data.

## Not changed

- `VictorySettlementController` runtime settlement algorithm.
- `VillageSecurityController` victory detection.
- settlement history file format.
- RaidInstanceKey, VillageKey, settlementKey, or favorRecordKey.
- BossBar progress, `waveChange`, `baselineReset`, cleanup, or vanilla suppression.
- Mixin enablement.
