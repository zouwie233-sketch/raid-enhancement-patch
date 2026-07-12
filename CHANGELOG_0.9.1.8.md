# Raid Enhancement Patch 0.9.1.8-safe-spawn-validation-alpha

## Purpose

Correct patch-owned raid spawn positions after field testing showed added raiders could be inserted into terrain or buildings. This version keeps the 0.9.1.7 performance hotfix and adds a bounded safety gate before entities enter the world.

## Changes

- Added `SafeRaidSpawnResolver` as the single safety boundary for patch-owned raid spawns.
- Validates each concrete entity with its real bounding box, rather than trusting only the shared anchor.
- Checks loaded chunks, world border, build height, collision space, fluids, common hazard blocks and ground support.
- Searches a deterministic nearby area and probes terrain height without loading new chunks or clearing blocks.
- Adds per-entity and per-server-tick search budgets to cap spawn-time work under high-pressure waves.
- Applies ground validation to vanilla extra-wave raiders, native-wave side reinforcements and Raids Enhanced ground specials.
- Applies air-space validation to the Raids Enhanced blimp path.
- Prevents command summon from bypassing safety validation at the original unsafe coordinates.
- Caches the native Raid `findRandomSpawnPos` compatibility method and spawn-path reflection members.
- Adds a one-time diagnostic for no-safe-position and budget-exhaustion outcomes.
- Only registers a blimp in the native Raid after it was successfully inserted into the world.

## Unchanged

- Wave counts and wave composition
- BossBar and HUD progress
- Victory suppression and settlement
- RaidInstanceKey, settlementKey and VillageKey
- Rewards and VillageFavor
- Villager protection behavior
- Golem rollback behavior from 0.9.1.7
- Mixin enablement
- Persistence formats

## Status

Alpha test build. Requires game testing in open terrain, dense villages, mountains, underground structures and high-omen multi-point raids.
