# Step 9 - 0.8.9.9.8 Independent BossBar Dynamic Progress Hotfix

## 背景

0.8.9.9.6 修复了 BossBar 闪回，0.8.9.9.7 尝试恢复每波回充，但 fallback 逻辑过于保守，导致独立 BossBar 在很多情况下持续满格，不能随袭击者死亡而下降。

## 修复

`RaidIndependentBossbarManager` 的进度算法改为动态 per-wave baseline：

- 新波次切换时重置本波 baseline。
- 观察到活袭击者时，以本波观察到的最大活袭击者数量作为分母。
- 当前活袭击者数量作为分子。
- 后续补怪或额外袭击者加入时，baseline 可向上修正。
- 无法取得可靠计数时才使用隐藏原版 BossBar 的非满格值或保留上一动态值。

## 数据来源优先级

1. 原版 Raid 的 `getTotalRaidersAlive()`。
2. 原版 Raid 的 `getAllRaiders()` 或内部 raider 容器反射计数。
3. 本 Mod 的 `RaidSession` 已知袭击者缓存，按 UUID 在当前世界查实体。
4. 低频、局部半径 Raider 扫描兜底。

## 性能控制

- 不扫描全世界。
- 优先读取 Raid 自身集合/计数。
- 局部扫描只在前几种来源失败时触发，并按 BossBar 节流。
- 玩家同步、标题更新和进度更新仍沿用 0.8.9.9.6 的节流策略。

## 不回归约束

本版不改波次生成、不改第八波防崩溃、不改胜利结算、不改奖励和村庄系统。
