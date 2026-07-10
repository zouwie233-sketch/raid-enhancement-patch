# Known Issues 0.9.1.5

- `VillageFavorGateway` is a delegating boundary; `VillageFavorSystem` remains the real read/write authority.
- The `RaidCompletionResult` overload is not yet used by the runtime settlement consumer.
- VillageKey/center drift remains deferred to the 0.9.2.x VillageKey stability line.
- `dimensionDuplicationAny=true` may still appear for raidInstanceKey/settlementKey; real key normalization remains deferred.
- Persistence is still properties-based and is not migrated in this version.
- A formal Gradle-built JAR was not produced in this environment because the sandbox cannot resolve `services.gradle.org`; this delivery is source-first.
