# Build verification — 0.9.1.8-safe-spawn-validation-alpha

- Source baseline: `0.9.1.7-reflection-cache-hotfix-alpha`
- Java target: 21 / class major version 65
- Gradle clean build: not available in the current offline container because the Gradle 8.12 distribution and NeoForge dependencies cannot be downloaded.
- Test JAR construction: changed Java sources compiled with Java 21 against the verified 0.9.1.7 binary baseline plus compile-only Minecraft/NeoForge signature stubs; only project classes and project metadata are inserted into the JAR.
- Compile-only stubs are not packaged.
- Version consistency checked in `gradle.properties`, `RaidEnhancementPatch.VERSION`, `neoforge.mods.toml` and JAR manifest.
- Mixin configuration unchanged from 0.9.1.7.
- Safe-spawn smoke tests passed:
  - open requested position accepted;
  - blocked requested point moved to a nearby safe point;
  - fully blocked scenario terminated through the bounded budget path.

This is a test build, not a stable release. A connected local/CI Gradle clean build remains recommended before a release candidate.
