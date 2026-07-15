# Build verification 0.9.1.9-runtime-boundary-alpha

## Passed locally

- Runtime architecture contract script.
- Version consistency across Gradle properties, entrypoint and NeoForge metadata.
- Exactly one `LevelTickEvent` owner.
- Legacy post-tick execution order contract.
- No reflection into `RaidExtraWaveController.STATES`.
- No reflection into `VillageSecurityController.SESSIONS`.
- Mixin source/config exact match.
- 84 top-level Java source files.

## Local build limitation

`gradlew.bat build --no-daemon` was attempted. It stopped before Gradle or Java source compilation because the local Java runtime is class version 55 (Java 11), while the Gradle wrapper class and project require class version 65 (Java 21).

This is not a source compilation result. The included GitHub Actions workflow installs Temurin Java 21, runs the architecture contract and then runs `clean build`.

## Still required

- Successful GitHub Actions Gradle build.
- JAR content and class-major audit.
- Minecraft startup with and without Raids Enhanced.
- 0.9.1.8 gameplay regression checklist.
- Multiplayer/Spark comparison before choosing a new stable baseline.
