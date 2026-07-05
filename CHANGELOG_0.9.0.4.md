# CHANGELOG_0.9.0.4

版本：`0.9.0.4-bossbar-display-layer-hotfix-alpha`

## 开发目标

修复 0.9.0.3 实测后遗留的 BossBar 玩家视觉层问题：

```text
第一波 BossBar 会下降；
第二波 BossBar 视觉上不回充；
第二波之后 BossBar 后续不继续正常下降。
```

0.9.0.3 日志显示服务端内部已经出现：

```text
waveChange=true
baselineReset=true
refillAttempt=true
progressApplied=true
progress=1.0000
```

因此 0.9.0.4 不重写进度算法，而是只修显示同步层。

## 主要改动

### 1. BossBar 视觉刷新时立即同步玩家

`RaidIndependentBossbarManager` 中，`updateBar(...)` 现在返回是否需要视觉刷新。

当出现以下情况时，会立即执行玩家同步：

```text
waveChange
refillAttempt
progressApplied
```

不再完全等待普通 20 tick 玩家同步周期。

### 2. 新增客户端重附着机制

新增：

```text
forceClientReattach(...)
```

当新波次、回充或进度变化发生时，对已绑定到 mod-owned BossBar 的玩家执行：

```text
removePlayer(player)
addPlayer(player)
```

目的是强制客户端重新接收当前 BossBar 的 title / progress / visible 状态。

### 3. 玩家实体对象替换保护

`syncPlayers(...)` 中，如果同一个玩家 UUID 对应的 ServerPlayer 对象发生替换，会先移除旧对象，再绑定新对象。

### 4. 原版 Raid BossBar 清理增强

`hideVanillaBossbar(...)` 中，隐藏原版 Raid BossBar 后额外执行：

```text
removeAllPlayers()
```

用于降低客户端仍显示或重新绑定旧原版条的概率。

## 保留内容

完整保留 0.9.0.3 的 settlementKey 修复方向：

```text
VictorySettlement 使用 RaidInstanceKey；
VillageFavor 继续使用 VillageKey；
旧 center-based settlement history 不再阻挡新袭击结算。
```

## 未修改内容

本版本没有修改：

```text
settlementKey / RaidInstanceKey 逻辑
VillageFavor
村民礼物
奖励表
袭击波次数
RaidWaveAuthority
RaidWaveExpansionController
RaidExtraWaveController 主结构
第八波防崩 Mixin
Raids Enhanced 方块保护 Mixin
```

## 已知问题

本版本尚未实机验证。必须通过用户游戏内测试确认：

```text
第二波视觉回充是否恢复；
第二波后续击杀是否继续下降；
是否出现双 BossBar、闪烁或 BossBar 消失；
0.9.0.3 settlementKey 回归是否仍通过。
```
