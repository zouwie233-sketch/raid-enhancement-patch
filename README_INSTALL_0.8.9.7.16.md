# Raid Enhancement Patch 0.8.9.7.16 - golem-block-break-hard-guard

基线：0.8.9.7.15-golem-block-break-guard

## 修复目标

0.8.9.7.15 实测仍无法完全阻止 Raids Enhanced 的“最后防线魔像”受击后破坏真实方块。本版改为多层硬防护。

## 改动

- 保留原先对 `GolemOfLastResort.destroyBlocks()` 的精准拦截。
- 新增受击后计时器清除：`hurt(...)` 返回后立即清空 `destroyBlocksTick`。
- 新增 NeoForge 事件钩子拦截：当 `EventHooks.onEntityDestroyBlock(...)` 的实体是最后防线魔像时，返回 false。
- 新增最终硬防线：当 `Level.destroyBlock(pos, drop, entity)` 的 source entity 是最后防线魔像时，直接返回 false。

## 默认配置

配置文件：

`config/raid_enhancement_patch/raids_enhanced_compat.properties`

关键项：

```properties
raidsEnhanced.golemOfLastResort.blockBreaking.enabled=false
raidsEnhanced.golemOfLastResort.blockBreaking.resetPendingTimerWhenBlocked=true
raidsEnhanced.compat.debugLogs.enabled=false
```

`false` 表示禁止最后防线魔像真实破坏方块。

## 未改动内容

- 不削弱最后防线魔像生命、伤害、AI、攻击动画、音效、粒子。
- 不改 Raids Enhanced 生成表。
- 不改 HUD。
- 不改战备令牌、洞察/集敌令牌、圣盾、雇佣兵系统。
- 不改第 9 → 10 → 11 波桥接。

## 安装

移除旧版：

`raid_enhancement_patch-0.8.9.7.15-golem-block-break-guard.jar`

只放入新版：

`raid_enhancement_patch-0.8.9.7.16-golem-block-break-hard-guard.jar`

不要多个版本同时放入 mods 文件夹。
