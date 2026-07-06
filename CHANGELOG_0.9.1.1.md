# CHANGELOG 0.9.1.1

Version: `0.9.1.1-key-service-audit-alpha`

## Type

Low-risk architecture governance / key audit build.

## Added

- Added `RaidKeyService` as a read-only key boundary helper.
- Added `KeyDebugService` as a read-only key diagnostic facade.
- Added key-boundary audit fields to `RaidKeyDiagnostics` startup, raid discovery, settlement, favor-record, and BossBar diagnostic lines.
- Added dimension duplication audit warnings that only report suspicious key strings and do not rewrite or normalize active gameplay keys.

## Preserved

- 0.9.1.0 BossBar behavior.
- `[REP]` independent BossBar visible authority.
- Same-wave refill suppression.
- Dimension-safe cleanup.
- `VictoryBarAttachGuard` vanilla victory BossBar rebind suppression.
- `settlementKeyMode=raidInstance` behavior.
- VillageFavor, rewards, villager gifts, raid wave counts, and persistence behavior.

## Not changed

- No VillageKey drift fix in this version.
- No VillageFavor behavior change.
- No settlement key rewrite.
- No BossBar progress algorithm change.
- No raid wave or reward balance change.
- No SavedData migration.
