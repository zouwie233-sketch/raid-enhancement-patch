# Raid Enhancement Patch 0.9.0.6 安装说明

版本：`0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha`

## 版本定位

这是 BossBar 收尾清理与回充时机打磨 alpha 版，不是稳定版。

本版基于 0.9.0.5 的诊断结论：玩家看到的确实是 RaidIndependentBossbarManager 创建的 `[REP]` 独立 BossBar。0.9.0.6 只处理两个问题：

1. 袭击 completed / stopped / victory 阶段的 BossBar 收尾清理；
2. waveChange 附近的视觉回充打磨，避免同一波内 alive 增加造成明显中间值回充。

## 环境要求

```text
Minecraft：1.21.1
NeoForge：21.1.234 或更高 21.1.x
Java：21
Raids Enhanced：可选，建议保持测试环境一致
fdlib：可选，若 Raids Enhanced 需要则保留
```

## 安装方法

1. 关闭游戏。
2. 进入当前整合包或实例的 `mods` 文件夹。
3. 删除所有旧版 `raid_enhancement_patch-*.jar`。
4. 放入：

```text
raid_enhancement_patch-0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha.jar
```

5. 启动游戏。
6. 确认 `latest.log` 中出现 `0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha`。

## KeyDiag 设置

建议测试时开启：

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

关键项：

```properties
enabled=true
log.bossbar=true
bossbar.visibleAuthorityAudit.enabled=true
bossbar.visibleAuthorityAudit.intervalTicks=20
bossbar.visibleAuthorityAudit.temporaryRepTitleMarker=true
```

## 重点测试

1. 第一波 `[REP]` BossBar 击杀后是否下降；
2. 第二波 `[REP]` BossBar 是否视觉回满；
3. 第二波回满后击杀是否继续下降；
4. 换波前是否不再出现明显中间值二段跳升；
5. 袭击完成后 `[REP]` 条是否正常消失；
6. 袭击完成后是否不再出现无 `[REP]` 原版条残留；
7. `key_diagnostics.log` 是否出现 `BossBarCleanupAudit`；
8. `settlementKeyMode=raidInstance` 是否仍正常；
9. 是否没有 `duplicate-blocked-by-history`；
10. 是否没有 `@raidInstance:fallback`；
11. 是否没有重复奖励；
12. 是否无崩溃。

## 已知限制

- 本版保留 `[REP]` 临时标题标记，用于确认玩家看到的是本 Mod 独立 BossBar。
- 本版不是最终 UI 稳定版。
- 本版不改 settlementKey、VillageFavor、奖励、村民礼物、袭击波次数、RaidWaveAuthority、RaidWaveExpansionController、第八波防崩 Mixin 或持久化数据。
