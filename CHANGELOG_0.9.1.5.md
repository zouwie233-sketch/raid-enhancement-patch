# CHANGELOG 0.9.1.5

Version: `0.9.1.5-village-favor-gateway-audit-alpha`

## Type

Low-risk VillageFavor gateway boundary version. Existing favor behavior is preserved by delegation.

## Added

- `VillageFavorGateway` as the settlement-facing VillageFavor boundary.
- `VillageFavorGatewayAudit` for explicit architecture and behavior-protection fields.
- A future overload accepting `RaidCompletionResult`.
- `village-favor-gateway` diagnostic records for:
  - favor-level read before reward calculation;
  - before legacy write delegation;
  - after legacy write delegation.

## Runtime routing change

`VictorySettlementController` now calls:

- `VillageFavorGateway.favorLevelFor(...)`;
- `VillageFavorGateway.recordRaidVictory(...)`.

The gateway immediately delegates to the existing `VillageFavorSystem` methods. No favor formula, value, state mutation rule or persistence format is reimplemented in the gateway.

## Not migrated

The runtime settlement path does not yet pass `RaidCompletionResult` into the gateway. The overload exists only as a prepared boundary. Logs must show `raidCompletionResultConsumerMigration=false` and `legacyArgumentsBridgeActive=true`.

## Preserved

- 0.9.1.4 VictorySettlement boundary snapshots;
- 0.9.1.3.1 BossBar audit throttle;
- settlement duplicate guard and key behavior;
- reward order and values;
- VillageFavor values, growth and storage;
- gifts and cooldowns;
- raid waves and Mixin enablement.
