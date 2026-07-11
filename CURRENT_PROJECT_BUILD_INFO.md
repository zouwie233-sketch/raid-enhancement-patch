# Current Project Build Info

Current line: 0.9.1.x architecture governance line

Current delivery: `0.9.1.6-config-audit-alpha`

Baseline anchor: `0.9.1.0-victory-bar-attach-guard-alpha`.

Passed candidates before this delivery:

- 0.9.1.1 Key Service audit;
- 0.9.1.2 Key audit polish;
- 0.9.1.3 BossBar module boundary;
- 0.9.1.3.1 BossBar audit throttle;
- 0.9.1.4 VictorySettlement boundary audit;
- 0.9.1.5 VillageFavorGateway audit (user Gradle build and game regression passed candidate).

0.9.1.6 performs a read-only configuration inventory. It adds no configuration behavior changes. The full audit contains 417 rows, 232 runtime-loaded entries and 9 loaded entries without confirmed runtime consumers.

Formal handoff from this environment is source-first. A local snapshot JAR may be supplied for testing but is not a Gradle clean-build release artifact.
