# Regression Test Result 0.9.1.5

Status: **game regression passed candidate**.

Confirmed from the user-built Gradle JAR and one complete raid test:

- correct 0.9.1.5 version loaded;
- [REP] BossBar lifecycle and progress remained normal;
- cross-dimension cleanup remained safe;
- settlementKeyMode remained raidInstance;
- no fallback or duplicate settlement;
- 0.9.1.4 completion snapshots remained present;
- VillageFavorGateway read/write legacy delegation occurred once in the expected order;
- eligible player count was correct for the single-player test;
- victoryCount and raidMeritScore increased once, not twice;
- RaidCompletionResult consumer migration remained disabled;
- no crash.
