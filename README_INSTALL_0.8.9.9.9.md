# Raid Enhancement Patch 0.8.9.9.9 安装说明

版本：`0.8.9.9.9-village-favor-structural-refactor-alpha`

## 安装

1. 删除旧版：
   - `raid_enhancement_patch-0.8.9.9.8-independent-bossbar-dynamic-progress-hotfix.jar`
2. 放入新版：
   - `raid_enhancement_patch-0.8.9.9.9-village-favor-structural-refactor-alpha.jar`
3. 建议继续删除旧运行期袭击状态：
   - `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除：

- `victory_settlement_history.properties`
- `settlement_rewards/`
- `battle_support_items.properties`

## 新增配置

首次启动会生成：

- `config/raid_enhancement_patch/village_favor_system.properties`
- `config/raid_enhancement_patch/settlement_rewards/village_favor_gift.json`

## 测试重点

1. 旧袭击 UI 和状态条不回归：不闪回、会下降、下一波会回充。
2. 困难 + 不祥 1 第八波不崩溃。
3. 胜利后与被拯救村庄区域内村民互动，会触发感谢提示。
4. 礼物有冷却，不能通过多个村民无限刷。
5. 功能关闭后不再触发感谢和礼物。
