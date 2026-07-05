# Raid Enhancement Patch 0.8.9.5 - Performance Audit and Cache

基线版本：0.8.9.4-village-golem-player-protection

## 版本目标

本版本只做性能审计、扫描降频、缓存与状态清理优化，不新增袭击玩法，不改变原版 Raid 胜负状态，不清理袭击者，不传送袭击者，不改 HUD，不改第 8 波到第 9 波桥接，不改第 9～11 波额外波状态机。

## 新增性能配置项

配置文件：`config/raid_enhancement_patch/village_security.properties`

新增配置：

```properties
performanceOptimization.raiderCacheScanIntervalTicks=20
performanceOptimization.stragglerGlowIntervalTicks=40
performanceOptimization.raiderAuditIntervalTicks=200
performanceOptimization.specialRaiderScanIntervalTicks=100
performanceOptimization.timeoutPenaltyCheckIntervalTicks=20
performanceOptimization.activeSessionFastCheck=true
performanceOptimization.cleanupEndedSessionsAggressively=true
debug.performance=false
```

## 优化内容

1. 已知袭击者缓存扫描间隔改为可配置，默认 20 tick。
2. 残兵发光定位检查低频化，默认 40 tick，并与特殊袭击者扫描间隔共同限流。
3. 超时审计间隔改为可配置，默认 200 tick。
4. 超时惩罚检查低频化，默认 20 tick。
5. Raids Enhanced 特殊袭击者识别结果按 UUID 缓存，避免在同一实体上重复做类型字符串判断。
6. 玩家伤害铁傀儡保护加入 active session 快速判断；无安防 session 时直接返回。
7. 可选积极清理已关闭 RaidSession，降低长期存档运行时状态残留。
8. 新增 `debug.performance`，默认关闭，只有同时开启 `debug.enabled=true` 才输出性能清理日志。

## 没有改动的内容

- 没有改 HUD。
- 没有改第 8 波到第 9 波桥接。
- 没有改第 9～11 波额外波状态机。
- 没有改离场 / 回归冻结逻辑。
- 没有改 Raids Enhanced 特殊袭击者生成逻辑。
- 没有改原版 Raid 失败状态。
- 没有清除袭击者。
- 没有传送袭击者。
- 没有新增惩罚或战斗数值。

## 建议测试

1. 正常袭击流程是否仍能显示村民防卫同盟文本。
2. 残兵数量较少时，是否仍能触发“残兵已被锁定”。
3. 清剿超时后，超时惩罚是否仍按每波一次触发。
4. 袭击结束后，下一场袭击是否不继承上一场的运行状态。
5. 控制台默认不应输出 performance debug 日志。
