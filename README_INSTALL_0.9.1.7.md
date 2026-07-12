# Build and Install 0.9.1.7

Version: `0.9.1.7-reflection-cache-hotfix-alpha`

## Install

1. Back up the test world.
2. Close Minecraft completely.
3. Remove the previous `raid_enhancement_patch` JAR from `mods`.
4. Copy `raid_enhancement_patch-0.9.1.7-reflection-cache-hotfix-alpha.jar` into `mods`.
5. Keep all existing configuration, VillageFavor and settlement-history files.
6. Start the same 0.9.1.6 test environment and confirm the startup log reports the exact 0.9.1.7 version.
7. Run the functional checklist before using the world for normal play.
8. Run `/spark profiler start --timeout 60` in a comparable problem scene.

## Rebuild from source

Requirements:

- Java 21
- Gradle 8.12 through the included Wrapper
- Network access for the NeoForge Gradle plugin and dependencies

Windows:

```bat
gradlew.bat clean build
```

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean build
```

A formal Gradle artifact should appear under `build/libs/`.
