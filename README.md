# Raid Enhancement Patch

Current staged build:

```text
0.9.1.0-victory-bar-attach-guard-alpha
```

This alpha keeps the tested 0.9.0.3 RaidInstanceKey settlement fix, the `[REP]` independent BossBar path, same-wave refill suppression, and dimension-safe cleanup. It adds a short same-dimension `VictoryBarAttachGuard` after raid completion so the vanilla `event.minecraft.raid.victory.full` BossBar cannot visibly rebind players after the managed `[REP]` BossBar has been removed.

This version does not change settlement keys, VillageFavor, rewards, villager gifts, raid wave counts, RaidWaveAuthority, RaidWaveExpansionController, baselineReset, waveChange, the core BossBar progress algorithm, persistent data, or the wave-8 crash guard. `ServerBossEventRaidTitleMixin` remains disabled.
