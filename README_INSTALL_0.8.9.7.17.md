# raid_enhancement_patch 0.8.9.7.17-golem-canentitygrief-gate

基于 0.8.9.7.16。

新增第五层、也是更早的一层保护：拦截 NeoForge `EventHooks.canEntityGrief(level, entity)`。

Raids Enhanced 的最后防线魔像在 `destroyBlocks()` 真正扫描和破坏方块之前，会先调用：

```java
EventHooks.canEntityGrief(level, this)
```

本版本在来源实体为 `GolemOfLastResort` 且配置禁止破坏方块时，直接返回 `false`。
这样原始逻辑会在最前置网关退出，不再进入附近方块扫描，不再调用 `onEntityDestroyBlock`，也不再调用 `Level.destroyBlock`。

默认配置仍为：

```properties
raidsEnhanced.golemOfLastResort.blockBreaking.enabled=false
```

如果旧配置被手动改成 true，请改回 false 或删除：

```text
config/raid_enhancement_patch/raids_enhanced_compat.properties
```

本版本不改魔像生命、伤害、AI、动画、音效、粒子、生成表，也不改战备令牌、雇佣兵、HUD 或额外波桥接。
