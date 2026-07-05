# STEP 9 / 0.9.0.2 - Key + BossBar Diagnostics Logfile Alpha

## Purpose

This is a diagnostic-only patch. It exists to make the next test log answer why the independent raid BossBar can decrease during a wave but fails to refill when the next wave starts.

It also keeps the 0.9.0.1 key-chain diagnostics for VillageKey / RaidInstanceKey auditing.

## Scope

This patch:

- replaces unreliable `System.out.println` KeyDiag output with SLF4J logger output so lines should enter `latest.log`;
- additionally writes diagnostic lines to `config/raid_enhancement_patch/key_diagnostics.log`;
- emits a startup diagnostic marker showing whether diagnostics are enabled;
- keeps key-chain lines for raid discovery, settlement, favor record, favor interaction and favor storage path;
- adds BossBar diagnostic fields: wave, previousLastWave, waveChange, baselineReset, alive, baseline, progress, countSource, nativeCount, sessionCount, nearbyCount, refillAttempt, progressApplied, vanillaProgress, decision, snapshotAgeTicks and staleSnapshotCandidate.

## Hard non-goals

This patch must not:

- change BossBar progress behavior;
- change key logic;
- change raid waves;
- change rewards;
- change villager gifts;
- change Mixin configuration;
- migrate persistence data;
- split or rewrite `RaidExtraWaveController`;
- be marked as a stable fix.

## How to enable diagnostics

Edit:

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

Set:

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

Restart the game or server after editing.

## What to collect after testing

Send both files if available:

```text
.minecraft/logs/latest.log
config/raid_enhancement_patch/key_diagnostics.log
```

The key question is whether the second wave shows:

```text
waveChange=true
baselineReset=true
refillAttempt=true
progressApplied=true
```

or whether it shows the old wave/baseline being reused.
