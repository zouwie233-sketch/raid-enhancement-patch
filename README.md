# Raid Enhancement Patch

当前开发版本：`0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha`

## 版本定位

0.9.0.6 是 BossBar 收尾清理与回充时机打磨 alpha 版，不是稳定版。

本版基于 0.9.0.5 的实测诊断结论：玩家看到的确实是 `RaidIndependentBossbarManager` 创建的 `[REP]` 独立 BossBar。0.9.0.6 只处理：

1. 袭击 completed / stopped / victory 阶段的 BossBar 收尾清理；
2. waveChange 附近的视觉回充打磨，避免同一波 alive 增加造成明显中间值回充。

## 当前版本文档

- 安装与测试：`README_INSTALL_0.9.0.6.md`
- 变更记录：`CHANGELOG_0.9.0.6.md`
- 构建自检：`BUILD_VERIFICATION_0.9.0.6.md`

## 重要边界

本版保留 0.9.0.3 的 `settlementKeyMode=raidInstance` 修复逻辑。

本版没有修改：

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

`ServerBossEventRaidTitleMixin` 类仍在源码中，但未在 `raid_enhancement_patch.mixins.json` 中启用。

## 构建

推荐在 GitHub Actions 中执行 `gradlew build`。当前沙盒无法解析 `services.gradle.org`，所以未完成本地 Gradle clean build；详情见 `BUILD_VERIFICATION_0.9.0.6.md`。
