# Current Project Build Info

Current line: 0.9.1.x architecture governance line

Current delivery: `0.9.1.5-village-favor-gateway-audit-alpha`

Baseline anchor: `0.9.1.0-victory-bar-attach-guard-alpha` passed BossBar / settlementKey regression candidate testing.

Passed candidates before this delivery:

- 0.9.1.1 Key Service audit;
- 0.9.1.2 Key audit polish;
- 0.9.1.3 BossBar module boundary;
- 0.9.1.3.1 BossBar audit throttle;
- 0.9.1.4 VictorySettlement boundary audit.

0.9.1.5 routes the existing settlement-facing VillageFavor read and write calls through `VillageFavorGateway`. The gateway delegates unchanged to `VillageFavorSystem`. A `RaidCompletionResult` overload is available but runtime consumer migration remains disabled pending game testing and later architecture approval.


## 0.9.1.5 source-first status

`0.9.1.5-village-favor-gateway-audit-alpha` is delivered as a complete source package for external Gradle build. Local Java 21 boundary compilation/static checks passed, but no formal Gradle clean build is claimed.
