# README_INSTALL_0.9.0.9

## Version

`0.9.0.9-victory-bar-attach-guard-alpha`

## Purpose

This alpha only targets the post-victory vanilla Raid BossBar rebind issue observed after 0.9.0.8.
It keeps the `[REP]` independent BossBar path, dimension-safe cleanup, same-wave refill suppression, and the 0.9.0.3 RaidInstanceKey settlement fix.

## Install

1. Remove every older `raid_enhancement_patch-*.jar` from `mods/`.
2. Install only:

```text
raid_enhancement_patch-0.9.0.9-victory-bar-attach-guard-alpha.jar
```

3. Keep the environment:

```text
Minecraft 1.21.1
NeoForge 21.1.234
Java 21
Raids Enhanced installed if used by the pack
fdlib installed if required by Raids Enhanced
```

4. Enable diagnostics in:

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

Recommended values:

```properties
enabled=true
log.bossbar=true
bossbar.visibleAuthorityAudit.enabled=true
bossbar.visibleAuthorityAudit.temporaryRepTitleMarker=true
```

## Test focus

Check:

1. The game loads as 0.9.0.9.
2. The `[REP]` BossBar appears during raids.
3. Wave 1 decreases after kills.
4. Wave 2 refills once and then decreases after kills.
5. Cross-dimension cleanup remains `skipped-different-dimension`.
6. The `[REP]` bar disappears after raid completion.
7. The no-`[REP]` vanilla victory BossBar does not remain after completion.
8. Logs contain `VictoryBarAttachGuard` and, if vanilla tries to rebind, `victory-bar-attach-blocked`.
9. `settlementKeyMode=raidInstance` still appears.
10. No `duplicate-blocked-by-history`.
11. No `@raidInstance:fallback`.
12. No repeated rewards.
13. No crash.

## Known status

This is an alpha test build, not a stable release.
