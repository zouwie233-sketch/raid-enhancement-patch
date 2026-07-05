# raid_enhancement_patch 0.8.9.7.15-golem-block-break-guard

基于 0.8.9.7.14。

## 本次改动

新增对 Raids Enhanced 1.0.2 / Minecraft 1.21.1 中“最后防线魔像”的兼容保护：

- 精准 Mixin 拦截 `GolemOfLastResort.destroyBlocks()`。
- 默认禁止最后防线魔像受击后真实破坏世界方块。
- 不进行全局实体扫描。
- 不监听每 tick 所有实体。
- 不删除最后防线魔像。
- 不削弱最后防线魔像的攻击、动画、声音、粒子和碎石视觉。
- 不修改 Raids Enhanced 的生成表、袭击波次、胜负逻辑。

## 配置文件

首次运行后生成：

```text
config/raid_enhancement_patch/raids_enhanced_compat.properties
```

默认：

```properties
raidsEnhanced.golemOfLastResort.blockBreaking.enabled=false
raidsEnhanced.golemOfLastResort.blockBreaking.resetPendingTimerWhenBlocked=true
raidsEnhanced.compat.debugLogs.enabled=false
```

如需恢复 Raids Enhanced 原版破坏方块行为，改为：

```properties
raidsEnhanced.golemOfLastResort.blockBreaking.enabled=true
```

## 安装

移除旧版 `raid_enhancement_patch` JAR，只保留：

```text
raid_enhancement_patch-0.8.9.7.15-golem-block-break-guard.jar
```

不要多个版本同时放入 mods 文件夹。
