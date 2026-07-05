# Raid Enhancement Patch 0.9.0.0 安装说明

版本：`0.9.0.0-village-favor-v2-profession-gifts-alpha`

## 安装

1. 删除旧版 JAR：
   `raid_enhancement_patch-0.8.9.9.9-village-favor-structural-refactor-alpha.jar`
2. 放入新版 JAR：
   `raid_enhancement_patch-0.9.0.0-village-favor-v2-profession-gifts-alpha.jar`
3. 建议继续删除运行期袭击状态：
   `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除：

- `victory_settlement_history.properties`
- `village_favor_relations.properties`
- `settlement_rewards/`
- `battle_support_items.properties`

## 新增配置

首次启动会生成或补全：

- `config/raid_enhancement_patch/village_favor_system.properties`
- `config/raid_enhancement_patch/settlement_rewards/village_favor/`

新版新增职业化礼物池，按职业组与礼物等级拆分，例如：

- `village_favor/generic/basic.json`
- `village_favor/farmer/master.json`
- `village_favor/librarian/great.json`

## 测试重点

1. 游戏能否正常启动。
2. 原袭击 UI、状态条、第八波防崩溃是否不回退。
3. 袭击胜利后恩情是否写入。
4. 与不同职业村民互动时，是否发放对应职业主题礼物。
5. 同一玩家同一村庄是否受冷却和每日领取次数限制。
6. 退出重进后，冷却和恩情等级是否保留。
