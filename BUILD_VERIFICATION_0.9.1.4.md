# BUILD VERIFICATION 0.9.1.4

Version: `0.9.1.4-victory-settlement-boundary-audit-alpha`

## Gradle status

`./gradlew compileJava --no-daemon` was attempted. The wrapper could not resolve `services.gradle.org` and stopped with `java.net.UnknownHostException` before compilation.

This delivery is **not claimed as a Gradle clean build**.

## Fallback build method

1. Use the tested 0.9.1.3.1 JAR as the binary base.
2. Compile only the new/changed project classes with Java 21 (`javac --release 21`) against the binary base and compile-time-only stubs.
3. Copy only `com/noah/raidenhancement/**` classes into the output JAR; no stub classes are included.
4. Update `META-INF/neoforge.mods.toml` and `META-INF/MANIFEST.MF`.
5. Repack and run archive, class-version, Mixin-list and entry-diff checks.

## Changed runtime classes

- `com/noah/raidenhancement/RaidEnhancementPatch.class`
- `com/noah/raidenhancement/raid/RaidKeyDiagnostics.class`
- `com/noah/raidenhancement/raid/RaidCompletionResult.class`
- `com/noah/raidenhancement/raid/VictorySettlementBoundaryAudit.class`

## Behavior protection

- Runtime settlement order changed: no.
- Duplicate guard changed: no.
- Reward dispatch changed: no.
- VillageFavor write changed: no.
- Cleanup changed: no.
- Real key format changed: no.
- BossBar or raid-wave behavior changed: no.
