# Install 0.9.1.3.1

Version: `0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha`

1. Close Minecraft completely.
2. Remove the previous Raid Enhancement Patch JAR from the instance `mods` folder.
3. Copy `raid_enhancement_patch-0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha.jar` into the same `mods` folder.
4. Keep Minecraft 1.21.1, NeoForge 21.1.234, Java 21, Raids Enhanced 1.0.2 and fdlib 1.0.9 unchanged for regression testing.
5. Delete only `config/raid_enhancement_patch/key_diagnostics.log` before the test so log-volume comparison is clean.
6. Do not delete `.properties` configuration files.

After testing, provide `latest.log` and `key_diagnostics.log`.
