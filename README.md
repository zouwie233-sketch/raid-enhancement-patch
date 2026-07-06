# Raid Enhancement Patch

Current version: `0.9.1.3-bossbar-module-boundary-alpha`

0.9.1.3 is a low-risk BossBar module boundary audit build. It preserves current gameplay behavior and only adds diagnostic boundary fields.

# Raid Enhancement Patch

Current staged build:

```text
0.9.1.2-key-audit-polish-alpha
```

This alpha keeps the tested 0.9.0.3 RaidInstanceKey settlement fix, the `[REP]` independent BossBar path, same-wave refill suppression, and dimension-safe cleanup. It adds a short same-dimension `VictoryBarAttachGuard` after raid completion so the vanilla `event.minecraft.raid.victory.full` BossBar cannot visibly rebind players after the managed `[REP]` BossBar has been removed.

This version does not change settlement keys, VillageFavor, rewards, villager gifts, raid wave counts, RaidWaveAuthority, RaidWaveExpansionController, baselineReset, waveChange, the core BossBar progress algorithm, persistent data, or the wave-8 crash guard. `ServerBossEventRaidTitleMixin` remains disabled.
