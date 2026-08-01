# Changelog 0.9.1.11-raid-spawn-persistence-beta

- Persist active bounded spawn batches in the existing raid lifecycle sidecar.
- Restore only validated remaining slots, retry counters, anchors, accounting and reservations.
- Persist the vanilla raid numeric ID to prevent a later raid at the same village from inheriting stale work.
- Reject corrupt batches atomically instead of restoring a partial plan.
- Coalesce lifecycle changes into at most one atomic sidecar replacement per processed level tick.
- Add a 200-tick retry backoff after sidecar write failure.
- Add clamped `raid_spawn_queue.properties` execution, persistence and diagnostic controls.
- Keep `SafeRaidSpawnResolver`, wave composition and enabled Mixins unchanged.

