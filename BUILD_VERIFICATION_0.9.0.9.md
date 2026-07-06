# BUILD_VERIFICATION_0.9.0.9

## Version

`0.9.0.9-victory-bar-attach-guard-alpha`

## Source package checks

- zip root is intended to be the Gradle project root: yes
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
- mixin config: present
- `.github/workflows/build-mod.yml`: present

## Build status

Gradle clean build was not completed in the current sandbox because the environment cannot resolve `services.gradle.org`.

Test JAR creation method:

1. Use the 0.9.0.8 JAR as runtime packaging baseline.
2. Compile changed Java classes with `javac` against the existing mod JAR classpath.
3. Replace the compiled mod classes in the JAR.
4. Update `META-INF/neoforge.mods.toml`.
5. Update `META-INF/MANIFEST.MF`.
6. Repack the JAR.

Compiled classes replaced:

```text
com/noah/raidenhancement/RaidEnhancementPatch.class
com/noah/raidenhancement/raid/RaidIndependentBossbarManager.class
com/noah/raidenhancement/raid/RaidIndependentBossbarManager$VictoryAttachGuard.class
com/noah/raidenhancement/raid/RaidIndependentBossbarManager$ManagedBossbar.class
com/noah/raidenhancement/raid/RaidIndependentBossbarManager$SuppressResult.class
com/noah/raidenhancement/raid/RaidIndependentBossbarManager$CountDiagnostics.class
com/noah/raidenhancement/raid/RaidIndependentBossbarManager$CountResult.class
```

No local Minecraft runtime test was performed in this sandbox.

## Known issues

- This is an alpha test build.
- It must be tested in game before being considered a candidate.
- It does not address VillageFavor center/key drift.
