# CHANGELOG 0.9.0.6

版本：`0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha`

## 目标

在 0.9.0.5 已确认玩家看到的是 `[REP]` 独立 BossBar 后，本版只做 BossBar 收尾清理与回充时机打磨。

## 新增/调整

- 保留 `[REP]` 临时标题标记。
- 新增 `BossBarCleanupAudit` 日志。
- 当 RaidEncounterAuthority snapshot 消失或袭击完成后：
  - 立即清理 REP 独立 BossBar 的玩家绑定；
  - 将 REP 独立 BossBar 设为不可见；
  - 在短暂 completion cleanup window 内继续 suppress 原版 Raid BossBar；
  - 记录 independent 与 vanilla BossBar 的 visible、progress、playerCount。
- 对同一波内的 upward refill 做显示抑制：
  - 非 `waveChange=true` 时，如果 alive 增加导致 progress 上升，不再把这个上升直接显示出来；
  - 只允许 `waveChange=true` 时形成明显回满；
  - 诊断 decision 会标记 `same-wave-refill-suppressed`。

## 保留

- 保留 0.9.0.3 的 `settlementKeyMode=raidInstance` 逻辑。
- 保留 0.9.0.5 的 BossBar visible authority 诊断能力。
- `ServerBossEventRaidTitleMixin` 仍未在 mixins.json 中启用。

## 未修改

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
持久化数据迁移
```

## 已知状态

- 本版为 alpha 测试版，不标记稳定。
- 当前沙盒无法访问 `services.gradle.org`，未完成 Gradle clean build。
- 已提供测试用 JAR 与 GitHub-ready 源码包，建议在 GitHub Actions 上执行正式构建。
