# Build and Install 0.9.1.6

Version: `0.9.1.6-config-audit-alpha`

## Artifact types

- The GitHub-ready source ZIP is the formal deliverable from this environment.
- A locally repacked snapshot JAR may also be supplied for testing, but it is not a Gradle clean-build release artifact.

## Build from source

Requirements:

- Java 21
- Network access for Gradle Wrapper and NeoForge dependencies

Windows:

```bat
gradlew.bat clean build
```

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean build
```

The Gradle-built JAR should appear under `build/libs/`.

## Install and test

1. Close Minecraft completely.
2. Remove the previous Raid Enhancement Patch JAR from `mods`.
3. Copy the 0.9.1.6 Gradle-built JAR or explicitly labeled snapshot JAR into `mods`.
4. Delete only `config/raid_enhancement_patch/key_diagnostics.log` for a clean test segment.
5. Keep all `.properties`, settlement history, VillageFavor data and world data.
6. Start Minecraft, complete one raid and follow `TEST_CHECKLIST_0.9.1.6.txt`.

This version is an audit alpha and must not be labeled stable before game regression passes.
