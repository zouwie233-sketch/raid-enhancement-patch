# Delivery report 0.9.1.11-raid-spawn-persistence-beta

## Scope

GitHub-ready source delivery for restart recovery and operational hardening of
the tested 0.9.1.10 bounded raid spawn queue.

## Implemented

- Versioned dependency-free queue snapshots and bounded Properties codec.
- Recovery of remaining slots, attempts, anchors, accounting and reservations.
- Stable native raid ID guard against stale same-village inheritance.
- Whole-batch corruption rejection and backward-compatible empty restore for old sidecars.
- Coalesced atomic sidecar replacement with write-failure backoff.
- Clamped server config for per-raid slots, per-level slots and retry limits.
- Optional low-noise batch-completion diagnostics.
- Queue cleanup when the authoritative raid state completes.

## Preserved

`SafeRaidSpawnResolver` is byte-identical to 0.9.1.10. Wave composition,
BossBar behavior, rewards, settlement keys, VillageFavor and enabled Mixins are
unchanged. Restored work uses the same bounded server-authoritative executor.

## Validation

- Java 21 queue/persistence/config contracts: PASS.
- Runtime architecture/static contract: PASS.
- JSON parsing: PASS.
- Controller syntax/signature smoke: PASS (external NeoForge symbols intentionally stubbed).

## Build authority

The included GitHub Actions workflow performs the authoritative NeoForge Gradle
build and uploads `build/libs/*.jar`. Minecraft restart and crash-soak testing
remain required before removing the beta label.

