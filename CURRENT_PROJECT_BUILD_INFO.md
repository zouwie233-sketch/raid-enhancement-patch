# Current Project Build Info

Current stage: ARCH-1 server lifecycle and cross-save persistence isolation.

Current delivery: `0.9.2.1-cross-save-lifecycle-isolation-alpha`

Direct source baseline: `0.9.1.8-safe-spawn-validation-alpha`.

Behavior comparison baseline: user-tested `0.9.1.8-safe-spawn-validation-alpha` JAR, SHA-256 `5CC761327877FE3042D317DDFD6F50BDA82C65A2FF2786470BFC6558F83FA330`.

Emergency safety anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

0.9.2.1 retains the centralized LevelTickEvent coordinator, immutable runtime views and all validated 0.9.1.12 raid behavior. It keeps the identity-keyed `RaidRuntimeRegistry` and moves lifecycle/queue recovery metadata behind a `RaidLifecycleSnapshotRepository` owned by the concrete server's `RaidRuntimeContext`.

Wave composition, safe-position rules and enabled Mixins remain unchanged. Recovery metadata is now stored in the current save's Overworld `SavedData`; the old version-global properties sidecar is intentionally ignored and is not automatically migrated. Bounded queue limits still use the separate clamped server properties configuration.

The supplied source remains independently Gradle-buildable with the included Wrapper and GitHub Actions Java 21 workflow. The local preferred runtime is Zulu JDK 21.0.8.
