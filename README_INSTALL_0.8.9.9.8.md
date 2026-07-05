# Raid Enhancement Patch 0.8.9.9.8 安装说明

版本：`0.8.9.9.8-independent-bossbar-dynamic-progress-hotfix`

## 安装

1. 删除旧版：
   `raid_enhancement_patch-0.8.9.9.7-independent-bossbar-progress-refill-hotfix.jar`
2. 放入新版：
   `raid_enhancement_patch-0.8.9.9.8-independent-bossbar-dynamic-progress-hotfix.jar`
3. 建议测试前删除：
   `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除：

- `victory_settlement_history.properties`
- `village_favor.properties`
- `battle_support_items.properties`
- `settlement_rewards/`

## 本版目标

修复独立 BossBar 在 0.8.9.9.7 中“会补满但不会下降”的问题。

本版不再每 tick 强制满格，而是按当前波次有效存活袭击者数量动态计算进度：

`当前有效存活袭击者 / 本波观察到的最大有效袭击者数量`

## 不改动区域

- 波次目标表
- 第八波防崩溃 Mixin
- `numGroups` 修复逻辑
- 刷怪逻辑
- 胜利结算
- 村庄恩情
- 战场扫荡
- 战备令牌
- 村民保护
- 安防铁傀儡
- Raids Enhanced 魔像保护
