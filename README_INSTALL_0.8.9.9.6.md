# Raid Enhancement Patch 0.8.9.9.6 安装说明

版本：`0.8.9.9.6-independent-raid-bossbar-performance-hotfix`

## 安装

1. 删除旧版：
   `raid_enhancement_patch-0.8.9.9.5-raid-wave8-crash-guard-audit-hotfix.jar`
2. 放入新版：
   `raid_enhancement_patch-0.8.9.9.6-independent-raid-bossbar-performance-hotfix.jar`
3. 建议删除旧运行期状态：
   `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除：

- `victory_settlement_history.properties`
- `village_favor.properties`
- `battle_support_items.properties`
- `settlement_rewards/`

## 本版重点

- 保留 0.8.9.9.5 已验证的第八波防崩溃修复。
- 停用原版袭击 BossBar 标题覆盖 Mixin。
- 新增独立 BossBar 显示器，避免继续和原版标题刷新抢控制权。
- 只有在模组独立 BossBar 创建成功后，才隐藏原版袭击 BossBar。
- 玩家同步每 20 tick 一次；标题/进度只在变化时更新，降低性能开销。

## 测试重点

1. 游戏能正常启动。
2. 困难 + 不祥 1 到第八波不崩溃。
3. 顶部袭击信息不再在“袭击”和“袭击 第 X / Y 波”之间闪回。
