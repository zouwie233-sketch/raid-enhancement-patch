# CHANGELOG 0.9.1.2

Version: `0.9.1.2-key-audit-polish-alpha`

## Type

Low-risk Key audit polish build.

## Added

- Added `keyAuditPolish=0.9.1.2` diagnostic marker.
- Added `actualKeyFormatUnchanged=true` and `keyFormatChange=false` diagnostic markers.
- Added `dimensionDuplicationAny` and `dimensionDuplicationSummary` fields.
- Added explicit recommendation text when duplicated dimension tokens are detected.

## Preserved

- 0.9.1.1 Key Service audit behavior.
- 0.9.1.0 BossBar behavior.
- `[REP]` independent BossBar visible authority.
- Dimension-safe cleanup.
- `VictoryBarAttachGuard` vanilla victory BossBar rebind suppression.
- `settlementKeyMode=raidInstance` behavior.
- VillageFavor, rewards, villager gifts, raid wave counts, and persistence behavior.

## Not changed

- No real key format normalization.
- No VillageKey drift fix.
- No VillageFavor behavior change.
- No settlement key rewrite.
- No BossBar progress algorithm change.
- No raid wave or reward balance change.
- No SavedData migration.
