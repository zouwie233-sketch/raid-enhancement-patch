# Raid Enhancement Patch

Current package: `0.9.0.7-bossbar-dimension-safe-cleanup-alpha`.

This is a Minecraft 1.21.1 NeoForge 21.1.234+ patch mod for raid enhancement testing.

## Current focus

0.9.0.7 fixes the 0.9.0.6 BossBar cleanup regression where the End dimension tick could incorrectly clean an Overworld `[REP]` BossBar.

The version keeps:

- 0.9.0.3 RaidInstanceKey settlement fix;
- 0.9.0.5 `[REP]` independent BossBar visible-authority path;
- BossBarCleanupAudit diagnostics;
- disabled `ServerBossEventRaidTitleMixin`.

It does not change settlement, reward, VillageFavor, raid-wave, persistence, or wave-8 guard logic.

See:

- `README_INSTALL_0.9.0.7.md`
- `CHANGELOG_0.9.0.7.md`
- `BUILD_VERIFICATION_0.9.0.7.md`
