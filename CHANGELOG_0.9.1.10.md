# Raid Enhancement Patch 0.9.1.10-bounded-spawn-queue-alpha

## Fixed

- Hard Omen V wave 11 no longer attempts all 62 patch-owned attackers in one
  server tick.
- Partial safe-spawn success no longer commits and discards every remaining
  main, side or Raids Enhanced special slot.
- Native-wave side reinforcements and native special packs use the same bounded
  retry execution path.
- World insertion is now the spawn commit point. A later bookkeeping failure
  cannot report the slot as failed and create a duplicate on retry.
- Ground raiders join the native Raid only after successful world insertion, so
  a failed insertion cannot leave a ghost wave member.

## Execution limits

- At most 8 slots from one raid queue are attempted per level tick.
- At most 16 slots across all raid queues in one level are admitted per tick.
- One slot rotates through planned anchors for at most 12 attempts.
- All-failed custom plans roll back and use the existing fresh-wave retry path.

## Preserved safety authority

`SafeRaidSpawnResolver` is byte-identical to 0.9.1.9. Loaded chunks, world
border, build height, real entity collision, fluids, hazards, stable ground
support, per-entity checks and per-tick search checks remain mandatory.

## Unchanged gameplay

- Difficulty/omen wave table and compositions
- BossBar/HUD rules
- Victory suppression and settlement
- Rewards and VillageFavor
- Villager protection and golem rollback
- Persistence file format and enabled Mixins

## Status

Alpha regression build. Do not promote to a stable server baseline until the
Hard Omen V, dense-terrain, multiplayer and restart checklist passes.
