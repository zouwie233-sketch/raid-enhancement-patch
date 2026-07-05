# README_INSTALL_0.9.0.4

版本：`0.9.0.4-bossbar-display-layer-hotfix-alpha`

## 定位

本版本是 BossBar 显示层专项热修 alpha。

它基于已经实测通过候选的 `0.9.0.3-settlement-raidinstance-key-alpha`，保留 RaidInstanceKey settlementKey 修复逻辑，只处理玩家视觉 BossBar 不回充 / 后续不继续下降的问题。

## 安装环境

建议环境：

```text
Minecraft 1.21.1
NeoForge 21.1.234 或同分支更高版本
Java 21
Raids Enhanced：可安装
fdlib：可安装
```

## 安装方式

1. 关闭游戏。
2. 进入当前整合包或实例的 `mods/` 文件夹。
3. 删除旧版 `raid_enhancement_patch-*.jar`。
4. 放入：

```text
raid_enhancement_patch-0.9.0.4-bossbar-display-layer-hotfix-alpha.jar
```

5. 启动游戏。
6. 确认 `latest.log` 中出现：

```text
0.9.0.4-bossbar-display-layer-hotfix-alpha
```

## 本版改动范围

本版本只围绕独立 BossBar 显示层做最小热修：

```text
1. BossBar 进度回充 / 进度变化时，强制对已绑定玩家执行一次 removePlayer + addPlayer 客户端重附着；
2. 每次视觉刷新需要时，立即同步附近玩家，而不是完全依赖 20 tick 周期同步；
3. 如果同一玩家实体对象发生替换，会移除旧对象并绑定新对象；
4. 隐藏原版 Raid BossBar 后，额外 removeAllPlayers，降低玩家仍绑定旧条的概率。
```

## 明确没有修改

本版本没有修改：

```text
settlementKey 逻辑
RaidInstanceKey 逻辑
VillageFavor 恩情系统
村民礼物系统
奖励系统
袭击波次数系统
RaidWaveAuthority
RaidWaveExpansionController
RaidExtraWaveController 大结构
第八波防崩 Mixin
RaidVictorySuppressMixin
职业礼物池
持久化迁移
```

## 已知风险

这是显示层热修 alpha，尚未经过用户游戏内实测。

重点观察：

```text
1. 第二波开始时玩家视觉 BossBar 是否真正回充；
2. 第二波回充后，击杀袭击者时 BossBar 是否继续下降；
3. 是否出现 BossBar 闪烁；
4. 是否出现两个袭击 BossBar 同时显示；
5. 是否出现袭击条完全消失；
6. settlementKey / favor-record 回归是否正常。
```

## 建议开启诊断

如果需要继续诊断，请确认：

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

中：

```properties
enabled=true
log.bossbar=true
log.settlement=true
log.favor=true
```

测试后保留：

```text
latest.log
config/raid_enhancement_patch/key_diagnostics.log
```
