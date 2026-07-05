# Step 9 / 0.9.0.3 - Settlement RaidInstanceKey Hotfix

## Version

`0.9.0.3-settlement-raidinstance-key-alpha`

## Goal

This release is a narrow settlement-key hotfix. It changes only the duplicate-guard key used by `VictorySettlementController`.

## What changed

- Victory settlement history now uses a raid-instance settlement key derived from the active `RaidSession`:
  - dimension id
  - raid/session key
  - session raid id
  - session created game time
- The old village-center settlement key is no longer allowed to block later raids in the same village.
- If an old history entry exists, diagnostics logs `legacy-history-present-ignored` instead of returning early.
- VillageFavor continues to receive the original village center coordinates and therefore continues using the VillageKey / player-village favor record path.
- 0.9.0.2 KeyDiag / BossBarDiag diagnostics are retained.

## What did not change

- BossBar progress algorithm was not changed.
- BossBar baseline / waveChange logic was not changed.
- Raid waves were not changed.
- Rewards were not changed.
- Villager gifts were not changed.
- Mixin config was not changed.
- Persistence was not globally migrated.
- `RaidExtraWaveController` was not restructured.

## Expected log signs

On a same-village second raid, the expected settlement diagnostics are:

- `phase=before-history-check`
- `settlementKeyMode=raidInstance`
- `settlementKey=...@raidInstance:...#created=<gameTime>`
- optionally `phase=legacy-history-present-ignored` for old center-key history
- no `phase=duplicate-blocked-by-history`
- no `phase=duplicate-blocked-by-raid-instance-history` unless the same raid instance is actually being submitted twice
- `phase=accepted-before-rewards`
- `favor-record` entries after successful settlement

## Testing focus

1. Complete one raid in a village.
2. Complete a second raid in the same village.
3. Confirm the second raid is not blocked by old settlement history.
4. Confirm VillageFavor records grow after the second completion.
5. Confirm same-raid duplicate settlement is still blocked if it ever happens.
