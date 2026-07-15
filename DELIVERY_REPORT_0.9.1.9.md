# Delivery report 0.9.1.9-runtime-boundary-alpha

## Scope

This release is the first behavior-preserving architecture phase based on
0.9.1.8-safe-spawn-validation-alpha. It does not intentionally change raid
rules, rewards, configuration values, persistence formats or enabled mixins.

## Structural changes

- Added `RaidTickCoordinator` as the single owner of `LevelTickEvent`.
- Preserved the effective 0.9.1.8 controller execution order and legacy error
  boundaries.
- Added read-only, type-safe runtime views for raid and village-security state.
- Removed bossbar reflection into `RaidExtraWaveController.STATES`.
- Removed battle-support reflection into `VillageSecurityController.SESSIONS`.
- Removed one unregistered tick bridge and one unconfigured dead mixin pair.
- Added an architecture contract verifier and made it a required GitHub Actions
  step before the Gradle build.

## Verification results

- Architecture contract: PASS.
- Version consistency: PASS.
- Java source inventory: 84 before, 84 after.
- `LevelTickEvent` owners: exactly 1.
- Private-map reflection targeted by this phase: 0 remaining occurrences.
- Enabled mixin configuration and source inventory: exact match.
- Configuration package hash comparison with 0.9.1.8: no changes.
- Resource hash comparison with 0.9.1.8: only
  `META-INF/neoforge.mods.toml` changed for release metadata.
- JSON parsing: 32 files checked, 0 parse failures.

## Build status

The local machine has Java 11, while this NeoForge project and its Gradle
wrapper require Java 21. The local `gradlew.bat build --no-daemon` attempt
therefore stopped before project source compilation with a class-version
mismatch (55 versus 65). This is an environment limitation, not a successful
or failed Java source compilation result.

The included GitHub Actions workflow installs Temurin Java 21, runs the
architecture verifier and then executes `./gradlew clean build`.

## Required acceptance before stable promotion

1. GitHub Actions completes successfully and uploads the JAR artifact.
2. Audit the built JAR for expected metadata, class inventory and Java 21 class
   major version.
3. Run `TEST_CHECKLIST_0.9.1.9.txt` in Minecraft, including multiplayer cases.
4. Compare server timings/Spark results against 0.9.1.8.

Until those checks pass, retain the `runtime-boundary-alpha` suffix and do not
replace the known-good 0.9.1.8 server build.
