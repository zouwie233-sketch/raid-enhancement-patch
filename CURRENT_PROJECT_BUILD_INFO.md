# Current Project Build Info

Current stage: raid registration and server lifecycle hotfix.

Current delivery: `0.9.2.0-architecture-runtime-context-alpha`

Direct source baseline: `0.9.1.8-safe-spawn-validation-alpha`.

Behavior comparison baseline: user-tested `0.9.1.8-safe-spawn-validation-alpha` JAR, SHA-256 `5CC761327877FE3042D317DDFD6F50BDA82C65A2FF2786470BFC6558F83FA330`.

Emergency safety anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

0.9.2.0 retains the centralized LevelTickEvent coordinator, immutable runtime views and all validated 0.9.1.12 raid behavior. ARCH-1.1 adds an identity-keyed `RaidRuntimeRegistry`, one `RaidRuntimeContext` per concrete server, explicit start/stopping/checkpoint/close transitions, and server-scoped diagnostic throttling. Core raid state and persistence formats are intentionally unchanged in this first architecture batch.

Wave composition, safe-position rules and enabled Mixins remain unchanged. The lifecycle persistence format is extended compatibly: older sidecars contain no queue section and restore as an empty queue. Bounded queue limits use a separate clamped server properties file.

The supplied source remains independently Gradle-buildable with the included Wrapper and GitHub Actions Java 21 workflow. The local preferred runtime is Zulu JDK 21.0.8.
