# BUILD_VERIFICATION_0.9.1.1

Version: `0.9.1.1-key-service-audit-alpha`

## Build status

The sandbox still cannot perform a full Gradle clean build because Gradle Wrapper requires access to `services.gradle.org` for Gradle 8.12. Therefore this delivery uses the previously available 0.9.1.0 jar as the binary base, compiles the new/changed diagnostic classes with `javac 21` against the 0.9.1.0 jar plus minimal compile-time stubs for external Minecraft/SLF4J symbols, then repacks the jar with updated manifest/resource metadata.

This is not claimed as a local Gradle clean build.

## Compiled and replaced classes

- `com/noah/raidenhancement/raid/RaidKeyService.class`
- `com/noah/raidenhancement/raid/KeyDebugService.class`
- `com/noah/raidenhancement/raid/RaidKeyDiagnostics.class`

## Metadata updated

- `gradle.properties`: `mod_version=0.9.1.1-key-service-audit-alpha`
- `RaidEnhancementPatch.java`: `VERSION=0.9.1.1-key-service-audit-alpha` in source
- `META-INF/neoforge.mods.toml`: `version="0.9.1.1-key-service-audit-alpha"`
- `META-INF/MANIFEST.MF`: `Implementation-Version: 0.9.1.1-key-service-audit-alpha`
- Key BossBar diagnostic version strings patched to `0.9.1.1-key-service-audit-alpha`

## Static checks required after packaging

- Jar integrity check with `zip -T`.
- Confirm manifest version.
- Confirm `neoforge.mods.toml` version.
- Confirm `RaidKeyService.class`, `KeyDebugService.class`, and modified `RaidKeyDiagnostics.class` are present.
- Confirm `ServerBossEventRaidTitleMixin` is still not enabled in `raid_enhancement_patch.mixins.json`.

## Runtime test requirement

This build must be game-tested before being promoted beyond alpha candidate.

## Local Gradle wrapper attempt

`./gradlew --version` was attempted in the sandbox. It failed before build execution because the wrapper could not resolve `services.gradle.org`:

```text
java.net.UnknownHostException: services.gradle.org
```

Use GitHub Actions or a local environment with Gradle 8.12 available for official clean build validation.
