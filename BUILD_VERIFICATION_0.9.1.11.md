# Build verification 0.9.1.11-raid-spawn-persistence-beta

## Passed locally with Zulu JDK 21.0.8

- Dependency-free queue and Properties codec compile with `--release 21`.
- 62-slot bounded retry/idempotency contract passes.
- Snapshot round-trip restores only remaining slots and retry rotation state.
- Restored batch reservations reject duplicate enqueue.
- Corrupt partial batch snapshots are rejected as a whole.
- Spawn config and stable raid-key helper compile with Java 21.
- Runtime architecture/static safety contract passes with 87 Java sources.
- 32 JSON resources parse successfully.
- `SafeRaidSpawnResolver` SHA-256 remains
  `2DAC1D2C532E68F73E10E04F9D3D334A622E0A49C45EF053D0E531829B5A1076`.

## Full build authority

The GitHub Actions workflow installs Java 21, runs the dependency-free contracts
and static verifier, then executes `./gradlew clean build --stacktrace --no-daemon`.
The produced JAR must pass `TEST_CHECKLIST_0.9.1.11.txt` before stable promotion.

