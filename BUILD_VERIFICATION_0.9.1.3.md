# BUILD_VERIFICATION_0.9.1.3

Version: `0.9.1.3-bossbar-module-boundary-alpha`

## Build status

The sandbox still cannot perform a full Gradle clean build because Gradle Wrapper requires access to `services.gradle.org`. This delivery therefore uses the existing 0.9.1.2 jar as the binary base, compiles the changed BossBar audit classes with `javac 21`, patches version strings in existing classes/resources, and repacks the jar.

This is not claimed as a local Gradle clean build.

## Compiled and replaced classes

- `com/noah/raidenhancement/raid/BossBarAuditLogger.class`
- `com/noah/raidenhancement/raid/RaidIndependentBossbarManager.class`

## Metadata updated

- `gradle.properties`: `mod_version=0.9.1.3-bossbar-module-boundary-alpha`
- `RaidEnhancementPatch.java`: `VERSION=0.9.1.3-bossbar-module-boundary-alpha` in source
- `META-INF/neoforge.mods.toml`: `version="0.9.1.3-bossbar-module-boundary-alpha"`
- `META-INF/MANIFEST.MF`: `Implementation-Version: 0.9.1.3-bossbar-module-boundary-alpha`

## Runtime test requirement

This build must be game-tested before being promoted beyond alpha candidate. Test focus is behavior unchanged and BossBar boundary audit field visibility.
