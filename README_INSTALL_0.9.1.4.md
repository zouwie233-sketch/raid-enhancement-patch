# Install 0.9.1.4

Version: `0.9.1.4-victory-settlement-boundary-audit-alpha`

1. Close Minecraft completely.
2. Remove the previous Raid Enhancement Patch JAR from the instance `mods` folder.
3. Copy `raid_enhancement_patch-0.9.1.4-victory-settlement-boundary-audit-alpha.jar` into the same folder.
4. Keep Minecraft 1.21.1, NeoForge 21.1.234, Java 21 and the existing modpack unchanged for regression testing.
5. Delete only `config/raid_enhancement_patch/key_diagnostics.log` before testing.
6. Do not delete `.properties` configuration files or settlement/favor data.

Complete one normal raid victory. After testing, provide:

- `latest.log`
- `config/raid_enhancement_patch/key_diagnostics.log`
