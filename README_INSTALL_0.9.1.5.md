# Build and Install 0.9.1.5

Version: `0.9.1.5-village-favor-gateway-audit-alpha`

## Source-first delivery

This package is delivered as a complete source project because the current build environment cannot resolve `services.gradle.org`. No local fallback JAR should be treated as the formal release artifact.

## Build

Requirements:

- Java 21
- Network access for the Gradle Wrapper and dependencies

Windows:

```bat
gradlew.bat clean build
```

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean build
```

The built JAR should appear under:

```text
build/libs/
```

## Install

1. Close Minecraft.
2. Remove the previous Raid Enhancement Patch JAR from `mods`.
3. Copy the newly Gradle-built 0.9.1.5 JAR into `mods`.
4. Delete only `config/raid_enhancement_patch/key_diagnostics.log` for a clean test log.
5. Keep `.properties`, settlement history and VillageFavor data.
6. Start Minecraft and run the supplied regression checklist.

Do not label the build stable before the complete raid regression passes.
