# Step 9 — 0.9.1.5 VillageFavor Gateway Audit

Version: `0.9.1.5-village-favor-gateway-audit-alpha`

## Goal

Create one explicit boundary between victory settlement and the VillageFavor subsystem without changing existing behavior.

## Boundary

`VictorySettlementController` may ask the gateway to:

- read a player's current favor level before reward XP calculation;
- record one accepted raid victory for the eligible players.

The gateway delegates these calls to `VillageFavorSystem`, which remains the runtime authority in 0.9.1.5.

## Prepared future interface

`VillageFavorGateway.recordRaidVictory(ServerLevel, RaidCompletionResult, Collection<Player>, String, boolean)` is available for future migration. It is not selected by the current settlement bytecode.

## Forbidden changes in this version

- no favor value or growth change;
- no VillageFavorState or VillageFavorRecord format change;
- no gift/cooldown change;
- no VillageKey stabilization;
- no key normalization;
- no reward, BossBar, wave or persistence change;
- no Mixin enablement change.

## Expected gateway audit sequence for one eligible player

1. `gatewayPhase=read-favor-level-before-reward`;
2. `gatewayPhase=before-delegate-legacy`;
3. existing `favor-record` before/after records;
4. `gatewayPhase=after-delegate-legacy`.

The exact order around the existing favor-record lines proves that the gateway delegates rather than reimplementing the write.
