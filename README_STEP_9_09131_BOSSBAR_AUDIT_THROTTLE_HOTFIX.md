# Step 9 — 0.9.1.3.1 BossBar Audit Throttle Hotfix

## Objective

Reduce BossBar diagnostic log amplification found in 0.9.1.3 without changing any runtime BossBar behavior.

## Evidence from 0.9.1.3

- Approximately 4185 `hide-vanilla` audit lines in about six minutes.
- Full boundary declarations repeated on recurring events.
- Player-binding audit lines included complete GameProfile texture and signature data.

## Hotfix boundary

Allowed: audit emission frequency, audit payload size, compact/full diagnostic field selection.

Forbidden: progress math, refill behavior, player synchronization behavior, vanilla suppression execution, cleanup behavior, VictoryBarAttachGuard behavior, keys, settlement, VillageFavor, rewards, waves, persistence, Mixins.

## Throttle policy

- first audit: log;
- vanilla visible reappearance: log;
- suppression failure: log;
- state changes: at most once per 20 ticks;
- unchanged state: summary at most once per 200 ticks;
- skipped records: counted in `suppressedRepeatCount`.

## Player payload policy

Record player name and UUID only. Do not serialize GameProfile properties, textures or signatures.
