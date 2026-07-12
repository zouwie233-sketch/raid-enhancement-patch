# Known Issues 0.9.1.7

- This alpha has not been tested in Minecraft by the user.
- The supplied JAR is a Java 21 binary-overlay hotfix artifact because the current environment cannot download Gradle 8.12 or NeoForge dependencies. It is not claimed as a Gradle `clean build` artifact.
- The extra-wave spawn system can still select unsafe offset positions and can place raiders inside blocks. This is the next P1 safe-spawn project and is not fixed in 0.9.1.7.
- The rollback mechanism still observes a bounded nearby block snapshot rather than receiving an authoritative list of blocks broken by the golem. The new budgets and collision guard reduce risk, but source attribution remains technical debt.
- Reflection invocation remains in optional compatibility paths; only member discovery is cached. Spark must confirm that reflection discovery no longer appears as a meaningful hotspot.
- VillageKey drift, key normalization, persistence migration and configuration debt remain deferred.
