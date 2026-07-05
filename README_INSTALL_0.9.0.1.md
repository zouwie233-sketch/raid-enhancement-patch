# Raid Enhancement Patch 0.9.0.1 Key Link Diagnostics Alpha

## Status

- Source patch prepared: yes
- JAR built in this sandbox: no
- Game-tested: no
- Stability label: diagnostic alpha only

## Build command for a prepared environment

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

Expected output after a successful external build:

```text
build/libs/raid_enhancement_patch-0.9.0.1-key-link-diagnostics-alpha.jar
```

## Runtime diagnostic enablement

After first launch, edit:

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

Set:

```properties
enabled=true
```

Keep it disabled during normal play to avoid log noise.
