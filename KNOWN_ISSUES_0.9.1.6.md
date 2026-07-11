# Known Issues 0.9.1.6

- This version identifies configuration debt but intentionally does not fix it.
- `rareGiftChanceMultiplier` remains loaded-but-ineffective.
- `equalXpPerEligiblePlayer` remains loaded-but-ineffective.
- Seven BattleSupport fields are loaded but have no confirmed external runtime consumer.
- A unified `OFF / NORMAL / VERBOSE` diagnostics mode is not implemented; existing per-flag diagnostics remain unchanged.
- Key normalization, VillageKey drift and persistence migration remain deferred.
- The snapshot JAR, when supplied, is a Java 21 local repackage based on the tested 0.9.1.5 binary, not a formal Gradle clean build.
