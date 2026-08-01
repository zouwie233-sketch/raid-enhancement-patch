# Persistent bounded raid spawn architecture

## Authority and ownership

`RaidExtraWaveController` remains server-authoritative. Each raid state owns one
`RaidSpawnWorkQueue`; clients do not plan, restore or spawn attackers.

## Persistence boundary

The queue exposes immutable dependency-free snapshots. The separate
`RaidSpawnQueuePersistenceCodec` serializes those snapshots into the existing
raid lifecycle Properties sidecar. Minecraft entities and world references are
never serialized.

The persisted contract contains:

- active immutable batch plans and numbered slots;
- remaining slot order and attempt counters;
- successful/exhausted accounting;
- anchor positions and retry rotation state;
- idempotency reservations;
- stable vanilla raid numeric identity.

Old sidecars have no queue format key and decode as an empty queue. A malformed
batch is rejected as a whole. Other valid raid lifecycle metadata remains usable.

## Write behavior

Enqueue, bounded drain and terminal cleanup mark lifecycle metadata dirty. All
changes in one processed level tick are coalesced into one file replacement.
The sidecar is written to a temporary sibling and atomically moved where the
filesystem supports it. A failed write uses a 200-tick retry backoff.

## Safety and performance boundaries

Restoration does not scan entities or chunks. Recovered slots re-enter the same
bounded executor and must pass `SafeRaidSpawnResolver` immediately before world
insertion. Per-raid, per-level and per-slot limits are clamped server settings.

## Crash consistency boundary

A normal save/stop and restart is recoverable. The sidecar is protected against
partial-file replacement. Cross-file atomicity between Minecraft entity saves and
the lifecycle sidecar is not available, so power loss during the narrow interval
between world persistence and queue checkpoint persistence remains a beta soak-test
case; no claim of transactional exactly-once spawning is made.

