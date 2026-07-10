# Current Project Build Info

Current line: 0.9.1.x architecture governance line

Current delivery: `0.9.1.3.1-bossbar-audit-throttle-hotfix-alpha`

Baseline anchor: `0.9.1.0-victory-bar-attach-guard-alpha` passed BossBar / settlementKey regression candidate testing.

0.9.1.3 completed the BossBar module boundary audit candidate, but testing found excessive audit output: approximately 4185 `hide-vanilla` records in about six minutes and full GameProfile payloads in player-binding records. 0.9.1.3.1 is a diagnostic-only hotfix that throttles those records without changing runtime behavior.
