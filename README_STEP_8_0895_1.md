# raid_enhancement_patch 0.8.9.5.1-world-load-hotfix

## 基线

基于 0.8.9.5-performance-audit-and-cache。

## 修复内容

修复进入世界后服务端 tick 崩溃：

- 崩溃首因：`NoSuchMethodError: LevelTickEvent$Post.getLevel(): java.lang.Object`
- 原因：`VillagerProtectionEvents.onLevelTickPost` 在 0.8.9.5 中被编译成了不兼容的 `getLevel():Object` 调用描述符；NeoForge 21.1.234 实际运行签名为 `getLevel():Level`。
- 修复：将村民保护 tick 入口改为与稳定的 `RaidWaveExpansionEvents` 一致的桥接写法，确保字节码调用 `getLevel():Level`，再传入内部 `Object` 参数进行 ServerLevel 判断。

## 未改动内容

本版本不新增玩法，不改动：

- HUD；
- 第 8 波 → 第 9 波桥接；
- 第 9～11 波额外波状态机；
- 离场 / 回归冻结；
- Raids Enhanced 特殊袭击者生成；
- 原版 Raid 胜负状态；
- 袭击者清理或传送；
- 0.8.9.5 的性能配置项和缓存逻辑。

## 测试重点

1. 加载世界不应再在 server tick loop 崩溃。
2. 原 0.8.9.5 的残兵定位、超时惩罚、安防系统、铁傀儡玩家伤害保护应保持不变。
