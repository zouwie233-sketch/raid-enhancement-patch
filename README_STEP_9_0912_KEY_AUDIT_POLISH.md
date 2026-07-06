# README_STEP_9_0912_KEY_AUDIT_POLISH

## Version

`0.9.1.2-key-audit-polish-alpha`

## Goal

Polish the read-only Key Service audit introduced in 0.9.1.1. This version is meant to make the discovered dimension duplication issue easier to inspect without changing live key behavior.

## Explicit non-goals

- Does not normalize duplicated dimension tokens.
- Does not change `settlementKeyMode=raidInstance`.
- Does not fix VillageKey center drift.
- Does not change BossBar, VillageFavor, rewards, waves, or persistence.

## Why this exists

0.9.1.1 found `dimensionDuplication=true` in raidInstanceKey / settlementKey strings. 0.9.1.2 makes that audit clearer with summary and recommendation fields while preserving the current safe gameplay baseline.
