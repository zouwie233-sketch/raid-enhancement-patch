# raid_enhancement_patch 0.8.9.7.19-golem-block-drop-cleanup

基于 0.8.9.7.18。

## 新增

在最后防线魔像方块回滚保护基础上，新增“恢复点短时掉落物清理”：

- 只在最后防线魔像受伤后的小窗口工作；
- 只在实际恢复过方块的位置创建小范围清理区；
- 只清理清理区附近的新鲜 ItemEntity；
- 记录魔像受伤瞬间周围已有掉落物，避免误删原本就在地上的物品；
- 不全局扫描掉落物，不扫描所有实体。

## 默认配置

配置文件：`config/raid_enhancement_patch/raids_enhanced_compat.properties`

```properties
raidsEnhanced.golemOfLastResort.blockBreaking.enabled=false
raidsEnhanced.golemOfLastResort.rollbackGuard.enabled=true
raidsEnhanced.golemOfLastResort.rollbackGuard.windowTicks=30
raidsEnhanced.golemOfLastResort.rollbackGuard.horizontalRadius=3
raidsEnhanced.golemOfLastResort.rollbackGuard.downRadius=1
raidsEnhanced.golemOfLastResort.rollbackGuard.upRadius=3
raidsEnhanced.golemOfLastResort.rollbackGuard.maxBlocksPerSnapshot=256
raidsEnhanced.golemOfLastResort.dropCleanup.enabled=true
raidsEnhanced.golemOfLastResort.dropCleanup.windowTicks=20
raidsEnhanced.golemOfLastResort.dropCleanup.radius=1.25
raidsEnhanced.golemOfLastResort.dropCleanup.maxItemAgeTicks=40
raidsEnhanced.golemOfLastResort.dropCleanup.maxZones=512
raidsEnhanced.golemOfLastResort.dropCleanup.maxItemsPerTick=128
raidsEnhanced.golemOfLastResort.dropCleanup.baselineExtraRadius=2
```

## 安装

移除旧版 JAR，只保留本版本 JAR。

如果旧配置缺少新增项，可以删除 `raids_enhanced_compat.properties` 让新版自动重建。
