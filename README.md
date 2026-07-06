# Raid Enhancement Patch

Current package: `0.9.0.8-bossbar-victory-vanilla-final-suppress-alpha`.

This is a Minecraft 1.21.1 NeoForge 21.1.234+ patch mod for raid enhancement testing.

## Current focus

0.9.0.8 keeps the tested 0.9.0.7 dimension-safe BossBar cleanup and adds a narrower final suppress window for the vanilla victory BossBar after raid completion.

The version keeps:

- 0.9.0.3 RaidInstanceKey settlement fix;
- 0.9.0.5 `[REP]` independent BossBar visible-authority path;
- 0.9.0.7 cross-dimension cleanup guard, where non-owning dimensions log `skipped-different-dimension` instead of clearing another dimension's bar;
- BossBarCleanupAudit diagnostics;
- disabled `ServerBossEventRaidTitleMixin`.

It only targets `event.minecraft.raid.victory.full` style vanilla victory BossBar rebinds during the same-dimension completion cleanup window. It does not change settlement, reward, VillageFavor, raid-wave, baselineReset, waveChange, progress math, persistence, or wave-8 guard logic.

See:

- `README_INSTALL_0.9.0.8.md`
- `CHANGELOG_0.9.0.8.md`
- `BUILD_VERIFICATION_0.9.0.8.md`
