# README_STEP_9_0911_KEY_SERVICE_AUDIT

## Version

`0.9.1.2-key-audit-polish-alpha`

## Goal

Start the 0.9.1.x architecture governance line by adding a read-only key service boundary. This version helps future AI and human maintainers distinguish:

- `RaidInstanceKey`: single-raid settlement identity
- `VillageKey`: long-term village identity
- `SettlementKey`: duplicate settlement guard; must remain raidInstance-mode
- `FavorRecordKey`: player plus village long-term favor record

## Explicit non-goals

- Does not fix VillageKey center drift.
- Does not alter settlementKey behavior.
- Does not alter BossBar behavior.
- Does not alter VillageFavor behavior.
- Does not alter rewards, gifts, waves, or persistence.

## Why this exists

The project is now past the 0.9.0.x BossBar stabilization line and has a 0.9.1.0 safety anchor. Before changing VillageFavor or persistence, the key boundary must become easier to inspect and harder to misuse.
