# STEP 9 / 0.9.0.1 - Key Link Diagnostics Alpha

## 1. Stage goal

This is a diagnostic-only source patch for auditing the current key chain before implementing the formal `VillageKey` / `RaidInstanceKey` split.

It adds opt-in logs for:

- raid discovery state key;
- village key candidate;
- raid instance key candidate;
- settlement key;
- favor record key;
- raid session key candidate;
- BossBar key;
- favor storage path source.

## 2. Explicit non-goals

This stage does **not** change:

- raid wave count;
- extra-wave spawning;
- BossBar progress algorithm;
- vanilla BossBar title Mixin state;
- victory/failure decision logic;
- settlement rewards;
- village favor reward values;
- villager profession gift logic;
- gift cooldown behavior;
- persistence location.

## 3. New config

A new diagnostic config is created at runtime:

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

Default master switch:

```properties
enabled=false
```

Enable only while collecting logs:

```properties
enabled=true
log.raidDiscovery=true
log.settlement=true
log.favor=true
log.bossbar=true
log.storagePaths=true
log.intervalTicks=100
log.maxPlayerKeysPerLine=3
```

## 4. Log prefixes

All new logs use this prefix family:

```text
[Raid Enhancement Patch][KeyDiag][raid-discovery]
[Raid Enhancement Patch][KeyDiag][settlement]
[Raid Enhancement Patch][KeyDiag][favor-record]
[Raid Enhancement Patch][KeyDiag][favor-interaction]
[Raid Enhancement Patch][KeyDiag][bossbar]
[Raid Enhancement Patch][KeyDiag][favor-storage]
```

## 5. Test purpose

The main test target is two consecutive successful raids in the same village:

1. first raid: record `state.key`, `villageKey`, `raidInstanceKeyCandidate`, `settlementKey`, `favorRecordKey`;
2. second raid: compare whether the village key stays stable and whether the settlement key is reused incorrectly;
3. confirm whether favor record keys aggregate or split.

## 6. Build status

This handoff is a source patch. It was not Gradle-built in the current sandbox because the environment cannot download Gradle 8.12 and does not contain the required offline Gradle/Maven caches.
