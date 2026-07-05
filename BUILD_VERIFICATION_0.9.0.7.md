# BUILD_VERIFICATION_0.9.0.7

## Artifact

`raid_enhancement_patch-0.9.0.7-bossbar-dimension-safe-cleanup-alpha`

## Source package structure

The github-ready source zip is intended to have the Gradle project at the zip root.

Expected root entries:

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `gradle/`
- `src/`
- `.github/workflows/build-mod.yml`
- `README.md`
- `README_INSTALL_0.9.0.7.md`
- `CHANGELOG_0.9.0.7.md`
- `BUILD_VERIFICATION_0.9.0.7.md`

## Static checks

- `build.gradle`: present
- `settings.gradle`: present
- `gradle.properties`: present
- `gradlew`: present
- `gradlew.bat`: present
- `gradle/wrapper/gradle-wrapper.jar`: present
- `gradle/wrapper/gradle-wrapper.properties`: present
- `src/main/java`: present
- `src/main/resources`: present
- `src/main/resources/META-INF/neoforge.mods.toml`: present
- `src/main/resources/raid_enhancement_patch.mixins.json`: present
- `.github/workflows/build-mod.yml`: present

## Local Gradle build

Attempted command:

```bash
./gradlew --version
```

Result:

```text
Failed in this sandbox because services.gradle.org could not be resolved.
```

Therefore, a full local `./gradlew clean build` was not completed in this environment.

## Partial compile / JAR generation

Because Gradle distribution download was unavailable, the test JAR was produced by:

1. using the user-provided 0.9.0.5 built JAR as the runtime baseline;
2. compiling the changed 0.9.0.7 Java classes with `javac --release 21`;
3. replacing the corresponding class files in the JAR;
4. updating `META-INF/neoforge.mods.toml`;
5. updating `META-INF/MANIFEST.MF`;
6. repackaging the test JAR.

Compiled replacement classes:

- `com/noah/raidenhancement/RaidEnhancementPatch.class`
- `com/noah/raidenhancement/config/KeyDiagnosticsConfig.class`
- `com/noah/raidenhancement/raid/RaidIndependentBossbarManager.class`
- `RaidIndependentBossbarManager` inner classes

## Modified source files

- `src/main/java/com/noah/raidenhancement/raid/RaidIndependentBossbarManager.java`
- `src/main/java/com/noah/raidenhancement/config/KeyDiagnosticsConfig.java`
- `src/main/java/com/noah/raidenhancement/RaidEnhancementPatch.java`
- `gradle.properties`
- `src/main/resources/META-INF/neoforge.mods.toml`
- version documentation files

## Explicitly not modified

- `VictorySettlementController`
- settlementKey / RaidInstanceKey logic
- `VillageFavor`
- reward logic
- villager gift logic
- raid wave count logic
- `RaidWaveAuthority`
- `RaidWaveExpansionController`
- wave-8 crash guard mixins
- persistent data migration
- `ServerBossEventRaidTitleMixin` enablement

## Test status

Not yet in-game tested.

## Known issue status

0.9.0.6 caused cross-dimension cleanup misfire. 0.9.0.7 is a targeted fix for that issue and must be tested with logs confirming `skipped-different-dimension` instead of cross-dimension `inactive-detected`.
