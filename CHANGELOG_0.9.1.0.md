# CHANGELOG 0.9.1.0

Version:

`0.9.1.0-victory-bar-attach-guard-alpha`

## Summary

Small BossBar stabilization candidate based on the 0.9.0.9 victory attach guard line.

## Functional changes

No new gameplay feature was added in this build.

This build intentionally preserves the 0.9.0.9 implementation and promotes it into a 0.9.1.0 small regression candidate after the previous 0.9.0.9 test showed:

- `[REP]` BossBar visible
- wave progress decreases correctly
- next wave refill reaches full once
- completion removes the `[REP]` bar
- the vanilla no-`[REP]` victory bar no longer remains visible
- `VictoryBarAttachGuard` blocks vanilla victory BossBar re-attach attempts
- `skipped-different-dimension` remains active
- `settlementKeyMode=raidInstance` remains active

## Preserved guardrails

This build does not modify:

- `settlementKey`
- `RaidInstanceKey`
- `VictorySettlementController`
- `VillageFavor`
- rewards
- villager gifts
- raid wave counts
- `RaidWaveAuthority`
- `RaidWaveExpansionController`
- `baselineReset`
- `waveChange`
- core BossBar progress math
- persistent data
- wave-8 crash guard mixins
- `ServerBossEventRaidTitleMixin` activation state

## Notes

This local artifact was prepared without a successful Gradle clean build in the sandbox because the Gradle wrapper could not download Gradle from `services.gradle.org` in the offline environment. Use GitHub Actions or a local environment with cached Gradle/NeoForge dependencies for formal clean-build verification.
