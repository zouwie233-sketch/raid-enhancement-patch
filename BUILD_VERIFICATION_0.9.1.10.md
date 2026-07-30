# Build verification 0.9.1.10-bounded-spawn-queue-alpha

## Passed locally with Zulu JDK 21.0.8

- Dependency-free queue contract compiles with `--release 21`.
- Generated queue bytecode reports class major version 65.
- 62-slot retry/idempotency contract passes.
- Per-queue 8-slot cap contract passes.
- Shared per-level 16-slot cap contract passes.
- Anchor rotation and all-failed reservation release contracts pass.
- Runtime architecture/static safety contract passes.
- 86 top-level Java sources found.
- 32 JSON resources parse successfully.
- `SafeRaidSpawnResolver` SHA-256 remains
  `2DAC1D2C532E68F73E10E04F9D3D334A622E0A49C45EF053D0E531829B5A1076`.

## Full Gradle status

Zulu JDK 21 successfully starts the repaired Windows wrapper. The wrapper then
tries to download Gradle 8.12, but `services.gradle.org` times out in the current
network environment. The build therefore stops before NeoForge source
compilation. GitHub Actions runs the same static and Java queue contracts before
`./gradlew clean build`.

## Still required

- Successful GitHub Actions Gradle build and JAR audit.
- Minecraft tests in `TEST_CHECKLIST_0.9.1.10.txt`.
- Spark comparison against 0.9.1.8/0.9.1.9.
- Restart-during-drain decision before stable promotion.
