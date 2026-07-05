# CHANGELOG_0.9.0.5

版本：`0.9.0.5-bossbar-visible-authority-audit-alpha`

## 类型

BossBar 可见权威诊断版。

## 背景

0.9.0.4 实测结果显示：

```text
第一波 BossBar 会下降；
第二波开始时玩家视觉仍不回充；
第二波击杀后会继续下降；
日志中服务端内部 progress=1.0000 且 progressApplied=true。
```

因此问题不应继续按进度算法修复，而应审计玩家实际看到的 BossBar 权威对象。

## 修改内容

### RaidIndependentBossbarManager

- 给本 Mod 创建的独立 BossBar 增加可配置临时标题标记 `[REP]`。
- 新增 BossBar visible-authority 诊断日志。
- 记录 independent BossBar：identity、title、progress、visible、playerCount、trackedPlayers。
- 记录 vanilla Raid BossBar：identity、title、progress、visible、playerCount。
- 记录 addPlayer/removePlayer 绑定事件。
- 记录 hideVanillaBossbar 执行前后的原版条状态。
- 记录 waveChange 后下一 tick 的 independent BossBar 状态。

### KeyDiagnosticsConfig

新增诊断配置：

```properties
bossbar.visibleAuthorityAudit.enabled=true
bossbar.visibleAuthorityAudit.intervalTicks=20
bossbar.visibleAuthorityAudit.temporaryRepTitleMarker=true
```

### 版本元数据

更新：

```text
gradle.properties
neoforge.mods.toml
RaidEnhancementPatch.VERSION
```

## 未修改内容

本版未修改：

```text
settlementKey
RaidInstanceKey
VictorySettlementController
VillageFavor
村民礼物
奖励系统
袭击波次数
RaidWaveAuthority
RaidWaveExpansionController
RaidExtraWaveController 主结构
第八波防崩 Mixin
ServerBossEventRaidTitleMixin 启用状态
```

## 重要说明

本版不是 BossBar 最终修复版。完成标准不是“修好回充”，而是回答：

```text
玩家看到的 BossBar 是不是带 [REP] 的 REP 独立 BossBar？
```
