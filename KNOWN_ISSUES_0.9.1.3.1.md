# Known Issues 0.9.1.3.1

- Full Gradle clean build is not verified in the sandbox because `services.gradle.org` is unreachable.
- Real key format still contains the previously audited dimension duplication in RaidInstanceKey / settlementKey; this version intentionally does not change it.
- VillageKey / village-center drift remains deferred to the 0.9.2.x VillageFavor stability line.
- In-game log-volume reduction is pending user regression testing.
