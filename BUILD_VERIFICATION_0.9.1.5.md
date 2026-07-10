# BUILD VERIFICATION 0.9.1.5

Version: `0.9.1.5-village-favor-gateway-audit-alpha`

## Gradle status

`./gradlew --version` was attempted. The wrapper tried to download Gradle 8.12 from `services.gradle.org` and failed with `java.net.UnknownHostException`.

This delivery is **not claimed as a Gradle clean build**.

## Fallback build method

1. Use the game-tested 0.9.1.4 JAR as the binary base.
2. Compile the new/changed project classes with Java 21 (`javac --release 21`) against that binary base and compile-time-only stubs.
3. Patch only the constant-pool owner reference in `VictorySettlementController.class` from `VillageFavorSystem` to `VillageFavorGateway`; method descriptors remain unchanged.
4. Copy only project classes into the output JAR; no Minecraft, NeoForge or SLF4J stubs are packaged.
5. Update manifest and `neoforge.mods.toml`, then run archive, bytecode linkage, class-version, entry-diff and Mixin-list checks.

## Changed runtime classes

- `com/noah/raidenhancement/RaidEnhancementPatch.class`
- `com/noah/raidenhancement/raid/RaidKeyDiagnostics.class`
- `com/noah/raidenhancement/raid/VictorySettlementController.class` — owner-reference routing only
- `com/noah/raidenhancement/favor/VillageFavorGateway.class` — new
- `com/noah/raidenhancement/favor/VillageFavorGatewayAudit.class` — new

## Behavior protection

- Favor formulas changed: no.
- Favor field mutation changed: no.
- Persistence changed: no.
- Gift or cooldown behavior changed: no.
- Reward order changed: no.
- Settlement duplicate guard changed: no.
- Key format changed: no.
- BossBar or raid-wave behavior changed: no.
- `RaidCompletionResult` runtime migration enabled: no.


## Source-first formal delivery

Per the user-approved fallback rule, the formal artifact from this environment is the complete source package. The locally repacked test JAR is not included as the formal deliverable because a Gradle clean build could not be completed. External builders should run `./gradlew clean build` and then execute the supplied regression checklist.
