# Regression Test Result 0.9.1.4

Status: **game-tested candidate passed**.

Verified in one complete raid:

- correct 0.9.1.4 version loaded;
- `[REP]` BossBar visible, decreasing and refilling correctly;
- completed `[REP]` cleanup and vanilla victory suppression passed;
- cross-dimension cleanup remained `skipped-different-dimension`;
- `settlementKeyMode=raidInstance` remained active;
- no fallback, duplicate settlement, duplicate reward or Mod crash;
- exactly two `victory-settlement-boundary` snapshots were produced;
- accepted snapshot resolved one eligible player;
- `consumersStillUseLegacyArguments=true` and runtime authority remained unchanged.

Archive note: this is an alpha/candidate result, not a full multiplayer, restart-persistence or Gradle-clean-build certification.
