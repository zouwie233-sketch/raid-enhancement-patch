# Raid Enhancement Patch

Current version: `0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha`

0.9.1.3.1 is a diagnostic-only BossBar audit throttle hotfix. It preserves the tested 0.9.1.3 BossBar runtime behavior and module boundaries while reducing repeated `hide-vanilla` audit output, emitting full boundary declarations only on creation, and limiting player-binding identity data to name and UUID.

It does not change BossBar progress math, wave refill behavior, cleanup execution, vanilla suppression execution, VictoryBarAttachGuard behavior, settlement keys, VillageFavor, rewards, raid waves, persistence, or Mixin enablement.

## Current architecture-governance delivery

`0.9.1.5-village-favor-gateway-audit-alpha` introduces `VillageFavorGateway` as the settlement-facing VillageFavor read/write boundary. Existing behavior remains delegated to `VillageFavorSystem`; the `RaidCompletionResult` overload is present for future migration but is not a runtime consumer in this version.
