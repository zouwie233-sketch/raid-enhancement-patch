# Current Project Build Info

Current stage: bounded safe-spawn execution hotfix.

Current delivery: `0.9.1.10-bounded-spawn-queue-alpha`

Direct source baseline: `0.9.1.8-safe-spawn-validation-alpha`.

Behavior comparison baseline: user-tested `0.9.1.8-safe-spawn-validation-alpha` JAR, SHA-256 `5CC761327877FE3042D317DDFD6F50BDA82C65A2FF2786470BFC6558F83FA330`.

Emergency safety anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

0.9.1.10 retains the centralized LevelTickEvent coordinator and immutable runtime views. Patch-owned main, side and Raids Enhanced special spawn plans now execute through a per-raid bounded queue so partial safe-spawn results cannot silently discard the remaining planned attackers.

Wave composition, safe-position rules, persistence format and enabled Mixins remain unchanged. Spawn execution is spread across bounded server ticks and failed slots rotate through planned anchors before being exhausted.

The supplied source remains independently Gradle-buildable with the included Wrapper and GitHub Actions Java 21 workflow. The local preferred runtime is Zulu JDK 21.0.8.
