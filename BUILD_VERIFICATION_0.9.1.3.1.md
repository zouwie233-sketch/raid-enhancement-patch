# BUILD VERIFICATION 0.9.1.3.1

Version: `0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha`

## Build status

A full Gradle build was attempted with `./gradlew compileJava --no-daemon`, but the sandbox could not resolve `services.gradle.org` and returned `java.net.UnknownHostException`.

This delivery is **not claimed as a Gradle clean build**.

The test JAR was produced by:

1. using the tested 0.9.1.3 JAR as binary base;
2. compiling the changed classes with Java 21 (`javac --release 21`);
3. replacing only the changed project classes;
4. updating `neoforge.mods.toml` and the JAR manifest;
5. repacking and running ZIP/JAR integrity checks.

## Compiled and replaced classes

- `com/noah/raidenhancement/RaidEnhancementPatch.class`
- `com/noah/raidenhancement/raid/BossBarAuditLogger.class`
- `com/noah/raidenhancement/raid/RaidIndependentBossbarManager.class`
- associated `RaidIndependentBossbarManager` nested classes

## Static verification

- Java class major version: 65 (Java 21).
- JAR archive integrity: passed.
- `ServerBossEventRaidTitleMixin` remains absent from the enabled Mixin list.
- `bossBarAuditThrottleStage=0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha` is present.
- `bossBarRuntimeBehaviorChanged=false` is present.
- `playerAuditPayload=name-and-uuid-only` is present.
- `suppressedRepeatCount` and the 200-tick summary policy are present.

## Log-volume replay estimate

Using the 0.9.1.3 test log as input, the new throttle decision logic would reduce `hide-vanilla` audit records from 4185 to approximately 210 (about 95% fewer records). This is a replay estimate, not a substitute for in-game testing.
