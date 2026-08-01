# Delivery report 0.9.1.12-raid-registration-lifecycle-hotfix

## Scope

GitHub-ready source hotfix for duplicate native raid insertion and reliable
same-process integrated-server restart recovery.

## Implemented

- Single world-insertion commit followed by membership-only native raid join.
- Dedicated server lifecycle event adapter with pre-stop checkpointing.
- Post-stop cleanup of controller, session and encounter process-local state.
- Static regression guards for join semantics, listener registration and reset
  coverage.

## Preserved

The bounded retry/persistence queue, safe-position authority, wave composition,
BossBar behavior, settlement keys, rewards, VillageFavor, configuration defaults
and enabled Mixins remain unchanged.

## Source validation

- Zulu Java 21 bounded queue/persistence contract: PASS.
- Architecture, registration and server lifecycle static contract: PASS.
- JSON parsing: PASS (32 resources).
- Safe spawn resolver byte-identity check: PASS.
- The included GitHub Actions workflow remains the authority for the full
  NeoForge Gradle compile and JAR artifact; no local JAR is included.

## Required game validation

Use Hard difficulty with Omen V. Confirm that each patch batch finishes at its
planned total without `UUID of added entity already exists`. During wave 11,
save and quit to title, then re-enter the world and confirm a persisted queue
restore message appears and only remaining slots spawn.
