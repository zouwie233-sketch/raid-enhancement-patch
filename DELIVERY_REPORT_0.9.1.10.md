# Delivery report 0.9.1.10-bounded-spawn-queue-alpha

## Scope

GitHub-ready source delivery for the bounded raid spawn queue hotfix. This
package is intended to be extracted into a repository root and built by the
included GitHub Actions workflow with Java 21.

## Implemented

- Per-raid idempotent spawn batches with numbered slots.
- Cross-tick retries with planned-anchor rotation.
- Per-raid limit of 8 admitted slots per level tick.
- Shared per-level limit of 16 admitted slots per tick.
- Main, side, native reinforcement and Raids Enhanced special paths use the
  bounded executor.
- World insertion is the slot commit point; bookkeeping failure cannot duplicate
  a successfully inserted entity.
- Ground entities join the native Raid after successful world insertion.
- All-failed custom batches roll back to the existing fresh-plan retry path.

## Preserved

The 0.9.1.9 `SafeRaidSpawnResolver` is byte-identical. Collision, loaded chunk,
world border, build height, fluids, hazards, stable support, concrete bounding
box checks and safety budgets remain enabled and mandatory.

Wave composition, rewards, settlement, BossBar, VillageFavor, persistence file
format and enabled Mixins are unchanged.

## Source validation

- Dependency-free Java 21 spawn-queue contract: PASS.
- 62-slot retry and no-duplicate contract: PASS.
- Per-raid and shared per-level admission limits: PASS.
- Runtime architecture/static safety contract: PASS.
- JSON parse check: PASS.

## Build authority

The included GitHub Actions workflow installs Temurin Java 21, runs the static
architecture check, compiles/runs the spawn-queue contract and then runs Gradle
`clean build`. The resulting JAR must still pass the supplied Minecraft test
checklist before stable-server promotion.
