# Raid registration and server lifecycle hotfix architecture

## Spawn commit boundary

`ServerLevel#addFreshEntity` remains the single authoritative world-insertion
commit for patch-owned ground raiders. Only after that call succeeds does the
controller invoke vanilla `Raid#joinRaid` with the already-spawned flag set.
Vanilla therefore registers the entity in the wave membership tables without
calling `addFreshEntityWithPassengers` a second time.

The order remains:

1. resolve and validate a loaded safe position;
2. finalize spawn data, equipment and persistence flags;
3. commit the entity to the server world once;
4. register native raid membership;
5. publish patch bookkeeping.

A post-commit bookkeeping failure still consumes the queue slot, preventing a
retry from duplicating an entity that is already in the world.

## Server lifecycle ownership

`RaidServerLifecycleEvents` is a dedicated event adapter. On
`ServerStoppingEvent`, it asks `RaidExtraWaveController` to force-checkpoint all
active bounded queues into the existing lifecycle sidecar. On
`ServerStoppedEvent`, it clears process-local controller, session and encounter
read-model state.

The stop cleanup never deletes or rewrites the sidecar. A later integrated or
dedicated server instance must load and validate persisted metadata instead of
silently reusing static objects from the previous server in the same JVM.

## Preserved boundaries

No tick listener, entity scan, reflection lookup loop, spawn count, retry limit,
wave composition or safe-position rule is added or changed. The new work occurs
only on the two server lifecycle events.
