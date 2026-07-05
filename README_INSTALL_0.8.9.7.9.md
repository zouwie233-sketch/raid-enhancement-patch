# Raid Enhancement Patch 0.8.9.7.9 - shield-token-hotfix

## 基线

基于 `0.8.9.7.8-wave-chain-bridge-hotfix`。

## 修复内容

本版本只修复并增强圣盾战备令牌：

- 修复圣盾令牌只尝试写入原版 absorption，部分环境/检测方式下表现为“没有作用”的问题。
- 新增圣盾令牌内部伤害吸收池：安防铁傀儡受到伤害时，会先消耗圣盾临时生命，再承受真实伤害。
- 保留对原版 absorption 的兼容写入，作为可视化/原版机制备份。
- 初级圣盾战备令牌从 20 点临时生命增强为 30 点。
- 高级圣盾战备令牌从 50 点临时生命增强为 100 点。
- 旧配置中如果仍是旧默认值 20/50，会在运行时自动迁移为 30/100。
- 自定义非默认配置值会被保留。

## 配置项

配置文件：

```text
config/raid_enhancement_patch/battle_support_items.properties
```

相关配置：

```properties
shield.enabled=true
shield.durationTicks=6000
shield.cooldownTicks=1200
shield.basicTempHealth=30.0
shield.advancedTempHealth=100.0
```

如果你想让配置文件文本也显示新默认值，可以删除旧的 `battle_support_items.properties` 后重启生成；不删除也能运行时迁移旧默认值。

## 未改动内容

本版本没有改动：

- HUD
- 第 8 波桥接
- 第 9～11 波额外波状态机
- 离场 / 回归逻辑
- Raids Enhanced 特殊袭击者逻辑
- 原版 Raid 胜负状态
- 普通战备令牌效果
- 雇佣兵颜色系统
- 雇佣兵持久化系统
- 村民保护药水图标隐藏逻辑

## 安装

移除旧版：

```text
raid_enhancement_patch-0.8.9.7.8-wave-chain-bridge-hotfix.jar
```

只放入新版：

```text
raid_enhancement_patch-0.8.9.7.9-shield-token-hotfix.jar
```
