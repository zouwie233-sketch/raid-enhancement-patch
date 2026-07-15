# Runtime boundary foundation

0.9.1.9 is a strangler-style architecture step. It does not replace the tested controllers.

## Tick ownership

`RaidTickCoordinator` is the only NeoForge level-tick subscriber. It delegates to the existing controllers in the same effective order as 0.9.1.8. Each legacy failure boundary remains grouped so normal behavior is unchanged.

## Read-only boundaries

- `RaidRuntimeView` exposes only the runtime key, opaque native Raid handle and completion flag. `ExtraWaveState` implements it directly, avoiding hot-path allocations.
- `VillageSecurityRuntimeView` exposes only immutable coordinates and security-golem UUIDs needed when a player uses a battle-support item.

Presentation and item modules must not reflect into controller-private maps. The architecture verification script enforces this rule.

## Deferred work

- Move active state from global static maps into a server-scoped registry.
- Establish one authoritative `RaidRuntime` state machine.
- Replace sidecar runtime persistence with versioned SavedData and conservative legacy migration.
- Add a global server-tick work budget and cross-tick spawn queue.
- Restrict reflection to optional compatibility adapters.
- Replace JSON-ish reward parsing with validated Codec/data-pack loading.

Each deferred item requires its own regression-tested release; they must not be combined into one rewrite.
