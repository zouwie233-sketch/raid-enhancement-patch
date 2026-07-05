# raid_enhancement_patch 0.8.9.9.5 安装说明

版本：0.8.9.9.5-raid-wave8-crash-guard-audit-hotfix

## 安装

1. 删除旧版 `raid_enhancement_patch-0.8.9.9.4-module-stub-cleanup-hotfix.jar`。
2. 只放入 `raid_enhancement_patch-0.8.9.9.5-raid-wave8-crash-guard-audit-hotfix.jar`。
3. 测试前建议删除：
   `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除奖励历史、村庄恩情、战备令牌配置和 `settlement_rewards` 文件夹。

## 本版目标

本版是稳定性热修，不加新玩法。重点修复原版 Raid 第 8 波进入 `Raid#getDefaultNumSpawns(index=8)` 时发生的数组越界崩溃。
