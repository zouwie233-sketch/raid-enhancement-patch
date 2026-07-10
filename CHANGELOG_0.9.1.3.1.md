# CHANGELOG 0.9.1.3.1

Version: `0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha`

## Type

Small diagnostic-output hotfix. No gameplay or BossBar behavior change intended.

## Changed

- `hide-vanilla` audit records are no longer emitted every tick.
- Full BossBar boundary declarations are emitted on BossBar creation; recurring records use compact boundary fields.
- Repeated `hide-vanilla` records are emitted only for:
  - first observation;
  - visible reappearance;
  - suppression failure;
  - debounced observed-state changes;
  - 200-tick periodic summaries.
- Periodic/state-change records include `suppressedRepeatCount`.
- Player-binding records now contain player name and UUID only; full GameProfile texture/signature payloads are no longer written.

## Preserved

- `[REP]` independent BossBar creation, title, progress and player binding.
- First-wave decrease and new-wave one-time refill.
- Same-dimension cleanup and `skipped-different-dimension` protection.
- Vanilla BossBar hiding execution frequency.
- VictoryBarAttachGuard behavior.
- `settlementKeyMode=raidInstance` and all Key audit behavior.

## Not changed

- BossBar progress algorithm, `waveChange`, or `baselineReset`.
- settlementKey, RaidInstanceKey, VillageKey, or real key format.
- VillageFavor, rewards, villager gifts, raid wave counts, persistence, or Mixins.
