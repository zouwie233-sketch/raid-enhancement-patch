# Current Project Build Info

## Version

0.9.0.2-key-bossbar-diagnostics-logfile-alpha

## Base

0.9.0.1-key-link-diagnostics-alpha

## Environment

- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Java: 21
- Gradle plugin: net.neoforged.moddev 2.0.107
- Gradle Wrapper: 8.12

## Build status in this handoff

This package is a source patch only. It has not been Gradle-built in the current sandbox because Gradle 8.12 and dependency caches are unavailable offline.

A lightweight stub-based Java syntax check was run for the modified diagnostics classes, but that is not a replacement for a real Gradle/NeoForge build.

## Recommended build command

```bash
./gradlew clean build --stacktrace --no-daemon
```

## Expected output after external build

```text
build/libs/raid_enhancement_patch-0.9.0.2-key-bossbar-diagnostics-logfile-alpha.jar
```

## Important note

No prebuilt Mod JAR is included in this source patch package. Build it through GitHub Actions or another Gradle-capable environment.
