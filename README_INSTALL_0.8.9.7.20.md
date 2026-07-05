# Raid Enhancement Patch 0.8.9.7.20 - Golem Drop Cleanup Query Hotfix

基于 0.8.9.7.19。

本版本继续保留 0.8.9.7.18 已验证有效的“最后防线魔像方块回滚保护”，并修复/强化 0.8.9.7.19 中掉落物未被清理的问题。

## 修复重点

- 强化 ItemEntity 查询：
  - `getEntitiesOfClass(Class, AABB)`
  - `getEntitiesOfClass(Class, AABB, Predicate)`
  - `getEntities(Entity, AABB, Predicate)` 泛用回退
- 反射方法查找现在会搜索：
  - public inherited methods
  - interface/default methods
  - declared superclass methods
- 方块恢复后立即执行一次掉落物清理，同时保留短时清理区继续兜底。
- 默认清理半径从 1.25 提升到 2.5。
- 默认清理窗口从 20 tick 提升到 60 tick。
- 默认最大物品年龄从 40 tick 提升到 120 tick。
- 默认每 tick 最大清理物品数从 128 提升到 256。

## 仍然保持的原则

- 不全世界扫描掉落物。
- 不每 tick 扫描所有实体。
- 只有“最后防线魔像受击后方块被恢复”的短窗口内才启用清理。
- 清理区域仍然绑定在被恢复的方块位置附近，避免误删普通战斗掉落。

## 建议配置

如果旧配置已经存在，建议删除：

`config/raid_enhancement_patch/raids_enhanced_compat.properties`

让新版自动生成完整默认配置。

关键默认值：

```properties
raidsEnhanced.golemOfLastResort.blockBreaking.enabled=false
raidsEnhanced.golemOfLastResort.rollbackGuard.enabled=true
raidsEnhanced.golemOfLastResort.dropCleanup.enabled=true
raidsEnhanced.golemOfLastResort.dropCleanup.windowTicks=60
raidsEnhanced.golemOfLastResort.dropCleanup.radius=2.5
raidsEnhanced.golemOfLastResort.dropCleanup.maxItemAgeTicks=120
raidsEnhanced.golemOfLastResort.dropCleanup.maxItemsPerTick=256
```

## 安装

移除旧版：

`raid_enhancement_patch-0.8.9.7.19-golem-block-drop-cleanup.jar`

只放入新版：

`raid_enhancement_patch-0.8.9.7.20-golem-drop-cleanup-query-hotfix.jar`

不要多个版本同时放入 mods 文件夹。
