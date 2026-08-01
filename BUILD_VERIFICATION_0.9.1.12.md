# Build verification 0.9.1.12-raid-registration-lifecycle-hotfix

## Passed locally with Zulu JDK 21.0.8

- Dependency-free bounded queue and persistence contract: PASS.
- Runtime architecture, single-insertion and lifecycle static contract: PASS
  with 88 Java sources.
- JSON resource parsing: PASS with 32 files.
- `SafeRaidSpawnResolver` remains byte-identical; SHA-256:
  `2DAC1D2C532E68F73E10E04F9D3D334A622E0A49C45EF053D0E531829B5A1076`.
- NeoForge 21.1.234 server lifecycle event classes and signatures were verified
  from the locally installed universal JAR.

## GitHub build authority

The included GitHub Actions workflow installs Java 21, runs the contracts, then
executes `./gradlew clean build --stacktrace --no-daemon` and uploads the JAR.
The built JAR must pass `TEST_CHECKLIST_0.9.1.12.txt` before promotion.

At the user's direction, no local JAR is required or claimed for this delivery;
the GitHub Actions Gradle job is the authoritative full compile.
