# Step 9 - 0.8.9.9.7 Independent BossBar Progress Refill Hotfix

## 用户实测反馈

- 困难不祥 1 到第八波不崩溃。
- 袭击信息不闪回。
- 新问题：每波结束后，独立 BossBar 没有像原版一样补满，而是只补充一格。

## 根因

0.8.9.9.6 的独立 BossBar 进度使用：

```text
currentWave / totalWaves
```

这适合作“总进度条”，但不符合原版 Raid BossBar。原版 Raid BossBar 是每一波的状态条，会在波间/下一波准备时重新满条。

## 修复

`RaidIndependentBossbarManager` 的进度策略改为：

1. 清剿/波间阶段：直接显示满条。
2. 活跃波次：优先读取隐藏原版 Raid `ServerBossEvent` 的 progress。
3. 如果读取失败：安全 fallback 为满条，避免回到“一格总进度”表现。

## 未改动

- 波次表
- 第八波防崩溃 Mixin
- numGroups 修复
- 刷怪逻辑
- 胜利结算
- 战备令牌
- 村民保护
- 安防铁傀儡
- Raids Enhanced 魔像方块回滚和掉落物清理
