# Current Project Build Info

Current stage: raid registration and server lifecycle hotfix.

Current delivery: `0.9.1.12-raid-registration-lifecycle-hotfix`

Direct source baseline: `0.9.1.8-safe-spawn-validation-alpha`.

Behavior comparison baseline: user-tested `0.9.1.8-safe-spawn-validation-alpha` JAR, SHA-256 `5CC761327877FE3042D317DDFD6F50BDA82C65A2FF2786470BFC6558F83FA330`.

Emergency safety anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

0.9.1.12 retains the centralized LevelTickEvent coordinator and immutable runtime views. Ground raiders now use one authoritative world insertion followed by membership-only native raid registration. Active bounded plans are checkpointed at server stopping, and process-local raid/session/read-model state is discarded after stop so same-process integrated-server restarts exercise the persisted recovery path.

Wave composition, safe-position rules and enabled Mixins remain unchanged. The lifecycle persistence format is extended compatibly: older sidecars contain no queue section and restore as an empty queue. Bounded queue limits use a separate clamped server properties file.

The supplied source remains independently Gradle-buildable with the included Wrapper and GitHub Actions Java 21 workflow. The local preferred runtime is Zulu JDK 21.0.8.
