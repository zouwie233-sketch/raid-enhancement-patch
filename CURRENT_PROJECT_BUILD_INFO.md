# Current Project Build Info

Current stage: P0 runtime stabilization and foundational defect repair.

Current delivery: `0.9.1.7-reflection-cache-hotfix-alpha`

Direct source baseline: `0.9.1.6-config-audit-alpha`.

Behavior comparison baseline: `0.9.1.5-village-favor-gateway-audit-alpha`.

Emergency safety anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

0.9.1.6 had not begun its planned regression test, but the Spark problem scene was run with 0.9.1.6 installed. The 0.9.1.7 hotfix therefore branches directly from the complete 0.9.1.6 source.

0.9.1.7 addresses repeated reflection discovery, unbounded rollback work amplification, duplicate golem snapshots, collision-unsafe restoration and redundant villager protection maintenance. It does not fix the separate unsafe extra-wave spawn-position design defect.

The supplied source remains independently Gradle-buildable with the included Wrapper when network dependencies are available. The local JAR is a Java 21 binary-overlay alpha artifact because this environment cannot download Gradle/NeoForge dependencies.
