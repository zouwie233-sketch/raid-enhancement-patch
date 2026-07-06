# README_INSTALL_0.9.0.8

## Version

`raid_enhancement_patch-0.9.0.8-bossbar-victory-vanilla-final-suppress-alpha`

## Target environment

- Minecraft `1.21.1`
- NeoForge `21.1.234` or later in the 21.1.x line
- Java `21`
- Raids Enhanced: optional but expected in the current test pack
- fdlib: optional but expected in the current test pack

## Install

1. Back up the test world.
2. Remove every older `raid_enhancement_patch-*.jar` from the `mods` folder.
3. Put only this file into `mods`:

   `raid_enhancement_patch-0.9.0.8-bossbar-victory-vanilla-final-suppress-alpha.jar`

4. Start the game and confirm `latest.log` shows `0.9.0.8-bossbar-victory-vanilla-final-suppress-alpha`.
5. Keep KeyDiag enabled for testing:

   `config/raid_enhancement_patch/key_diagnostics.properties`

   Recommended settings:

   ```properties
   enabled=true
   log.bossbar=true
   log.settlement=true
   log.favor=true
   bossbar.visibleAuthorityAudit.enabled=true
   bossbar.visibleAuthorityAudit.temporaryRepTitleMarker=true
   ```

## Scope

0.9.0.8 only addresses the post-completion vanilla victory BossBar reappearing or rebinding players after the `[REP]` independent BossBar has been cleaned up.

It keeps the 0.9.0.7 dimension-safe cleanup guard. If a non-owning dimension tick sees another dimension's managed BossBar, it must log `skipped-different-dimension` instead of cleaning it.

## Do not treat as stable

This is an alpha test build. It must not be labeled stable until the user confirms:

- `[REP]` BossBar visible during active raid;
- first wave decreases;
- second wave refills once and decreases after kills;
- no End/Nether tick clears an Overworld BossBar;
- raid completion removes `[REP]`;
- no vanilla no-`[REP]` victory BossBar remains visible after completion;
- `settlementKeyMode=raidInstance` remains normal;
- no `duplicate-blocked-by-history`;
- no `@raidInstance:fallback`;
- no duplicate reward;
- no crash.
