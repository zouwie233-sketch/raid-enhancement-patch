# 0.8.9.9.4 Omen I Native Wave / BossBar Cache Hotfix

## 修复目标

0.8.9.9.2 把所有不祥等级都当成存在原版 bonus wave，导致困难 + 不祥 1 被写成 `numGroups=7`，实测只有 7 波而 HUD 显示 8 波。

## 修复内容

- `hasExpectedVanillaBonusWave(omenLevel)` 从 `omenLevel > 0` 改为 `omenLevel > 1`。
- Omen I：8 逻辑波 = `numGroups=8`。
- Omen II+：8 逻辑波 = `numGroups=7 + vanilla bonus wave`。
- `RaidWaveExpansionController` 的 unsafe repair cap 改为按当前 omen level 计算，避免把 Omen I 的安全 `numGroups=8` 降成 7。
- BossBar title cache 从 5 秒延长到 30 分钟，用于减少原版标题回退造成的闪回。

## 未改动

- 波次目标表未改。
- 战备令牌、村民保护、安防铁傀儡、胜利奖励、扫荡、恩情、Raids Enhanced 方块回滚未改。
