# Current Project Build Info

Current stage: behavior-preserving runtime architecture foundation.

Current delivery: `0.9.1.9-runtime-boundary-alpha`

Direct source baseline: `0.9.1.8-safe-spawn-validation-alpha`.

Behavior comparison baseline: user-tested `0.9.1.8-safe-spawn-validation-alpha` JAR, SHA-256 `5CC761327877FE3042D317DDFD6F50BDA82C65A2FF2786470BFC6558F83FA330`.

Emergency safety anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

0.9.1.9 centralizes all LevelTickEvent handling in RaidTickCoordinator while retaining the previous event-registration execution order. BossBar and battle-support modules now consume immutable runtime views instead of reflecting into private controller maps.

No gameplay, configuration, persistence or enabled-Mixin change is intended in this stage. The next architecture stage must not begin until this boundary release passes the 0.9.1.8 regression checklist.

The supplied source remains independently Gradle-buildable with the included Wrapper and GitHub Actions Java 21 workflow. This local environment only has Java 11, so it cannot produce the Java 21 JAR locally.
