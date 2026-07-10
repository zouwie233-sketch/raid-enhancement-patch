# Step 9 — 0.9.1.4 Victory Settlement Boundary Audit

## Goal

Represent the data already present at the accepted victory-settlement boundary as one immutable diagnostic snapshot without moving any runtime responsibility.

## New boundary object

`RaidCompletionResult` contains:

- raidInstanceKey;
- villageKey;
- dimensionId;
- center X/Y/Z;
- victory;
- eligible player UUIDs;
- omen level;
- total waves;
- completed game time.

## Audit phases

- `before-history-check`: identity/outcome snapshot before duplicate-history evaluation; participant resolution is still pending.
- `accepted-before-rewards`: accepted settlement snapshot after eligible players and effective omen/waves are resolved, before reward dispatch.

## Non-authoritative rule

0.9.1.4 does not migrate any consumer to `RaidCompletionResult`. The following continue to use the tested existing path:

- duplicate guard;
- reward dispatch;
- Hero of the Village application;
- VillageFavor write;
- battlefield sweep scheduling;
- BossBar cleanup and VictoryBarAttachGuard.

## Future use

A later, separately approved boundary version may migrate one consumer at a time after this audit confirms that the projected fields are stable and complete.
