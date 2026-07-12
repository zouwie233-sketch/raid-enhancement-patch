# 0.9.1.7 Hotfix Architecture Boundary

## Shared compatibility cache

`com.noah.raidenhancement.compat.CachedReflection`

- Caches class, method, constructor and field lookup.
- Caches lookup failures as negative results.
- Performs accessibility preparation once.
- Contains no Minecraft or NeoForge imports.
- Intended only for optional/descriptor-sensitive compatibility code.

## Golem rollback work ownership

`GolemBlockRollbackGuard` owns an identity-keyed map of server levels. Each level owns:

- one rollback snapshot per golem UUID;
- coalesced cleanup zones keyed by restored block coordinates;
- bounded, round-robin block checks;
- bounded cleanup-zone processing.

No world-wide entity or block scan was added.

## Collision safety

A changed block is not restored while the triggering golem's bounding box intersects that block. If bounding-box access cannot be resolved, restoration fails safe by waiting rather than sealing the entity. Collision-blocked entries are abandoned after a bounded delay.

## Villager protection maintenance

- Effect member resolution is cached.
- Existing protection state controls effect refresh cadence.
- Direct damage-event refreshes only rebuild effects when their remaining duration crosses the configured threshold.
- Health-clamp maintenance is idempotent within one server game tick.

## Deferred boundary

Safe extra-wave spawning is a separate module-level change. It must validate each final entity location and must not be folded into the rollback performance hotfix.
