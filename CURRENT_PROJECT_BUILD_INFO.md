# Current Project Build Info

Current line: 0.9.1.x architecture governance line

Current delivery: `0.9.1.4-victory-settlement-boundary-audit-alpha`

Baseline anchor: `0.9.1.0-victory-bar-attach-guard-alpha` passed BossBar / settlementKey regression candidate testing.

Passed candidates before this delivery:

- 0.9.1.1 Key Service audit;
- 0.9.1.2 Key audit polish;
- 0.9.1.3 BossBar module boundary;
- 0.9.1.3.1 BossBar audit throttle.

0.9.1.4 adds a diagnostic-only immutable `RaidCompletionResult` projection at the existing victory-settlement boundary. It does not migrate any runtime consumer and requires in-game verification before candidate promotion.
