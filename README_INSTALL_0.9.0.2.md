# Raid Enhancement Patch 0.9.0.2 Key + BossBar Diagnostics Logfile Alpha

## Status

Source patch only in this handoff.

```text
Built JAR: not produced in this sandbox
Game test: not tested
Purpose: diagnostics only
```

## Build

Use a Gradle-capable environment, for example GitHub Actions, and run:

```bash
./gradlew clean build --stacktrace --no-daemon
```

Expected JAR:

```text
build/libs/raid_enhancement_patch-0.9.0.2-key-bossbar-diagnostics-logfile-alpha.jar
```

## Testing focus

After installing the built JAR:

1. Start the game/server once.
2. Enable `config/raid_enhancement_patch/key_diagnostics.properties` by setting `enabled=true`.
3. Restart the game/server.
4. Trigger one raid and advance from wave 1 into wave 2.
5. Observe whether the BossBar decreases during wave 1 and whether wave 2 refills.
6. Send `latest.log` and `config/raid_enhancement_patch/key_diagnostics.log`.

## Important

This version does not fix BossBar refill. It only makes the next log explain where 0.9.0.3 should fix it.
