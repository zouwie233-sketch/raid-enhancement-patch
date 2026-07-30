# Bounded raid spawn execution

## Ownership

`RaidExtraWaveController` remains the server-authoritative raid state owner.
Each `ExtraWaveState` owns one `RaidSpawnWorkQueue`; there is no client-side
planning or spawning.

## Plan and execution boundary

Wave composition produces immutable batch anchors and idempotent numbered
slots. Execution consumes only a bounded number of slots per level tick.
Successful slots are removed permanently. Failed slots rotate through anchors
and re-enter the queue without repeating successful slots.

`RaidSpawnTickBudget` shares a second admission cap across all raid queues in a
level. This prevents multiple simultaneous raids from multiplying per-raid
work into an unbounded server-tick spike.

## Safety boundary

The queue chooses when and where to attempt a planned slot. It cannot authorize
world insertion. `SafeRaidSpawnResolver` still decides whether the concrete
entity's real bounding box can occupy a loaded, in-border, collision-free,
fluid-free, hazard-free and supported position.

## Failure semantics

- Partial success: keep only failed slots pending.
- Search/budget/loaded-area failure: rotate and retry within fixed limits.
- Entire custom plan exhausted: roll back the logical wave reservation and use
  the existing delayed fresh-plan retry.
- Partial terminal exhaustion: keep successful attackers and report one batch
  summary; never duplicate them.
- Player absent: do not drain queued work.
- Native wave advanced: close remaining optional native slots rather than spawn
  stale reinforcements.

## Deferred

The runtime queue is deliberately not added to the legacy Properties format.
Crash-consistent pending-slot recovery requires versioned SavedData or stable
slot entity identities and must be implemented as a separate migration. Until
then, restart-during-drain remains an alpha test case and stable promotion gate.
