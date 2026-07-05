# 安装说明：raid_enhancement_patch-0.8.9.8.1-world-tick-accessor-hotfix

## 基线

本版本基于 `0.8.9.8.0-victory-settlement-sweep-favor-alpha` 进行热修。

## 修复内容

修复进入世界即崩溃的问题：

`java.lang.NoSuchMethodError: net.neoforged.neoforge.event.tick.LevelTickEvent$Pre.getLevel()`

原因是部分事件监听器直接调用 `LevelTickEvent.Pre/Post#getLevel()`，在用户实际 NeoForge 21.1.234 运行时中出现返回值描述符不兼容。本热修将以下监听器改为反射获取世界对象：

- `RaidWaveExpansionEvents`
- `VillagerProtectionEvents`

保持 `BattleSupportEvents` 原有反射路径不变。

## 没有改动

- 不改第 9～11 波桥接状态机。
- 不改 Raids Enhanced 最后防线魔像回滚保护。
- 不改战备令牌注册、创造栏、HUD。
- 不改胜利结算、战场扫荡、恩情机制的设计规则，只修启动世界崩溃。

## 安装

1. 删除 mods 文件夹内旧版 `raid_enhancement_patch-*.jar`。
2. 只放入：`raid_enhancement_patch-0.8.9.8.1-world-tick-accessor-hotfix.jar`。
3. 不要多个版本共存。
