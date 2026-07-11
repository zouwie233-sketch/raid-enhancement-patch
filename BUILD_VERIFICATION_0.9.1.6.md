# BUILD VERIFICATION 0.9.1.6

Version: `0.9.1.6-config-audit-alpha`

## Gradle status in this environment

`./gradlew --offline clean build` was attempted. The wrapper still attempted to download Gradle 8.12 and failed because `services.gradle.org` could not be resolved (`UnknownHostException`).

This environment therefore does **not** claim a Gradle clean build.

## Snapshot build method

1. Use the tested 0.9.1.5 JAR as the binary baseline.
2. Compile only the new/changed project classes with Java 21 (`javac --release 21`) against the baseline and compile-time-only external stubs.
3. Package only project classes; exclude every Minecraft, NeoForge and SLF4J stub.
4. Replace version metadata and `neoforge.mods.toml`.
5. Run archive, class-version, entry-diff, Mixin-list and string-marker checks.

Changed/new runtime classes:

- `com/noah/raidenhancement/RaidEnhancementPatch.class`
- `com/noah/raidenhancement/config/ConfigAuditService.class`
- `com/noah/raidenhancement/raid/RaidKeyDiagnostics.class`

## Behavior protection

- Config values/defaults changed: no.
- Config keys/files deleted or renamed: no.
- Config migration performed: no.
- Runtime consumers changed: no.
- Favor/reward/gift/cooldown/persistence changed: no.
- Key/BossBar/wave/Mixin behavior changed: no.

The GitHub-ready source ZIP remains the formal handoff. The snapshot JAR is test-only until rebuilt with Gradle and game-tested.
