# Known Issues 0.9.1.4

- `RaidCompletionResult` is diagnostic-only and is not yet the authoritative source consumed by settlement, rewards, favor, or cleanup.
- `dimensionDuplicationAny=true` may still appear for raidInstanceKey/settlementKey. This version intentionally does not normalize real key formats.
- VillageKey/center drift remains deferred to the 0.9.2.x VillageKey stability line.
- The delivery JAR is not a verified Gradle clean build because the sandbox cannot resolve `services.gradle.org`.
