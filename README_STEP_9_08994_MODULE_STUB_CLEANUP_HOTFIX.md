# 第九步 0.8.9.9.4 工作报告：Module Stub Cleanup Hotfix

## 版本

`0.8.9.9.4-module-stub-cleanup-hotfix`

## 用户反馈

上一版游戏启动直接崩溃。

## 崩溃原因

日志显示：

`java.lang.module.ResolutionException: Modules raid_enhancement_patch and minecraft export package net.minecraft.world.level to module c2me_opts_math`

原因是 0.8.9.9.3 JAR 误打包了编译期 stub class，例如：

- `net/minecraft/world/level/Level.class`
- `net/minecraft/world/entity/raid/Raid.class`
- `net/neoforged/fml/common/Mod.class`
- `org/spongepowered/asm/mixin/Mixin.class`

这些 class 不应该被放进最终 Mod JAR。

## 修复方式

从 JAR 中删除所有误打包的：

- `net/minecraft/**`
- `net/neoforged/**`
- `org/spongepowered/asm/**`

## 保留内容

保留 0.8.9.9.3 的功能修复：

- 困难 + 不祥 1 的 8 波修正
- 不祥 2+ 的 vanilla bonus wave numGroups 处理
- BossBar 标题缓存缓解
- Raid Encounter Authority 第一层结构
- 波次审计日志

## 未改动

未改动：

- 波次目标表
- 战备令牌
- 雇佣兵
- 村民保护
- 安防铁傀儡
- 胜利奖励 JSON
- 战场扫荡
- 村庄恩情
- Raids Enhanced 魔像方块回滚与掉落物清理
