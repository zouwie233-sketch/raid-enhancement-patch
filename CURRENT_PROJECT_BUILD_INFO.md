# CURRENT_PROJECT_BUILD_INFO

Current line: 0.9.1.0 BossBar stabilization candidate

Current version:

`0.9.1.0-victory-bar-attach-guard-alpha`

Environment:

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Mod ID: `raid_enhancement_patch`

Status:

- Based on the 0.9.0.9 victory attach guard line.
- No new gameplay feature was added.
- The 0.9.0.3 `settlementKeyMode=raidInstance` fix must remain preserved.
- The 0.9.0.5 `[REP]` independent BossBar path must remain preserved.
- The 0.9.0.7 dimension-safe cleanup guard must remain preserved.
- The 0.9.0.9 `VictoryBarAttachGuard` must remain preserved.
- Do not reactivate `ServerBossEventRaidTitleMixin`.

Build note:

The included local JAR was not produced by a successful sandbox Gradle clean build because Gradle could not be downloaded from `services.gradle.org`. Use GitHub Actions for formal clean-build verification.
