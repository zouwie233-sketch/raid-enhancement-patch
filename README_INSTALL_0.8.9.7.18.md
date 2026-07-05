# Raid Enhancement Patch 0.8.9.7.18 - golem-block-rollback-guard

基于 0.8.9.7.17 继续加固 Raids Enhanced 最后防线魔像方块破坏保护。

## 新增
- 在最后防线魔像受到伤害时，只对它身边小范围非空气方块做短期快照。
- 接下来默认 30 tick 内，每 tick 只恢复这份小快照中被改动/破坏的方块。
- 这不是全世界扫描，也不是全实体扫描，只在该魔像受伤后短窗口工作。

## 默认配置
config/raid_enhancement_patch/raids_enhanced_compat.properties

- raidsEnhanced.golemOfLastResort.blockBreaking.enabled=false
- raidsEnhanced.golemOfLastResort.rollbackGuard.enabled=true
- raidsEnhanced.golemOfLastResort.rollbackGuard.windowTicks=30
- raidsEnhanced.golemOfLastResort.rollbackGuard.horizontalRadius=3
- raidsEnhanced.golemOfLastResort.rollbackGuard.downRadius=1
- raidsEnhanced.golemOfLastResort.rollbackGuard.upRadius=3
- raidsEnhanced.golemOfLastResort.rollbackGuard.maxBlocksPerSnapshot=256

## 未改动
HUD、战备令牌、雇佣兵、第 9→10→11 波桥接、Raids Enhanced 生成表、原版 Raid 胜负逻辑均未做无关改动。
