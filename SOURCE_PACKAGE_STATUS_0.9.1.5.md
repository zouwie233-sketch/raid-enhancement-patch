# Source Package Status 0.9.1.5

Version: `0.9.1.5-village-favor-gateway-audit-alpha`

## Delivery decision

This delivery is source-first.

The current environment can run Java 21 local compilation and static bytecode checks, but it cannot complete the Gradle Wrapper download because `services.gradle.org` cannot be resolved. Therefore this package does **not** claim a verified Gradle clean build and does not present a locally repacked JAR as the formal release artifact.

## What is verified

- The complete Gradle source project is present.
- `VillageFavorGateway` and `VillageFavorGatewayAudit` are present.
- `VictorySettlementController` source routes favor reads and victory writes through `VillageFavorGateway`.
- The gateway delegates to the existing `VillageFavorSystem` with the same arguments and ordering.
- A future `RaidCompletionResult` overload is present but is not selected by the current runtime settlement path.
- Static Java 21 compilation of the new/changed boundary classes passed using the game-tested 0.9.1.4 binary as compile classpath plus compile-time-only stubs.
- Mixin configuration remains unchanged; `ServerBossEventRaidTitleMixin` is not enabled.

## What remains to be verified

Run a formal build in an environment that can resolve Gradle dependencies:

```bash
./gradlew clean build
```

Then test the produced JAR with the supplied `TEST_CHECKLIST_0.9.1.5.txt`.

## Formal artifact status

- Source package: ready for review and external build.
- Formal Gradle-built JAR: not produced in this environment.
- In-game regression status: pending until an externally built JAR is tested.
