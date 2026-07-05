# Raid Enhancement Patch 0.8.9.7.6 - Mercenary Performance Persistence

基线：0.8.9.7.5-item-use-compat-hotfix。

本版本只检修和增强雇佣兵战斗令牌 / 雇佣兵铁傀儡系统，不改 HUD、第 8 波桥接、第 9～11 波额外波状态机、离场 / 回归核心逻辑、Raids Enhanced 特殊袭击者生成、原版 Raid 胜负状态、清怪/传送袭击者逻辑、村民保护药水显示逻辑。

## 新增与修复

- 雇佣兵默认存在时间改为 30 分钟：36000 tick。
- 雇佣兵默认可在袭击之外召唤：mercenary.requireActiveRaid=false。
- 每名玩家默认最多同时拥有 4 个雇佣兵铁傀儡。
- 雇佣兵药水效果改为召唤时一次性给予，不再每秒刷新。
- 增加低频缺失效果修复：默认每 600 tick 检查一次，只有效果丢失时才补。
- 雇佣兵增加原版发光标记，并尝试加入 GOLD 颜色队伍，表现为金橙色轮廓。
- 雇佣兵记录写入实体 scoreboard tags：owner、expireGameTime、dimension。
- 世界退出 / 重进后，可通过实体持久化标签恢复剩余存在时间。
- PVP 感知玩家伤害保护：主人不能攻击自己的雇佣兵；PVP 关闭时所有玩家不能攻击雇佣兵；PVP 开启时其他玩家可以攻击别人的雇佣兵。

## 重要配置

配置文件：config/raid_enhancement_patch/battle_support_items.properties

如果你之前已经生成过旧配置，代码会对旧默认值做运行时迁移：

- 旧默认 mercenary.requireActiveRaid=true 会迁移为 false；
- 旧默认 mercenary.durationTicks=12000 会迁移为 36000；
- 旧默认 mercenary.maxActivePerPlayer=2 会迁移为 4。

如果你手动改过这些值，代码会尽量保留你的自定义设置。

新增配置项可参考：

- config/raid_enhancement_patch/battle_support_items.default.properties
- 或 JAR 内 config_templates/battle_support_items.default.properties

## 安装

移除旧版：

- raid_enhancement_patch-0.8.9.7-security-support-items.jar
- raid_enhancement_patch-0.8.9.7.1-component-compat-hotfix.jar
- raid_enhancement_patch-0.8.9.7.2-component-factory-hotfix.jar
- raid_enhancement_patch-0.8.9.7.3-registry-key-hotfix.jar
- raid_enhancement_patch-0.8.9.7.4-creative-tab-hotfix.jar
- raid_enhancement_patch-0.8.9.7.5-item-use-compat-hotfix.jar

只放入：

- raid_enhancement_patch-0.8.9.7.6-mercenary-performance-persistence.jar

## 建议测试顺序

1. 进入世界，打开创造栏，确认 10 个令牌显示正常。
2. 在非袭击状态下使用雇佣兵战斗令牌，确认可以召唤。
3. 查看雇佣兵效果时间，应从约 29:59 正常倒计时，不应一直卡住。
4. 确认雇佣兵有金橙色/黄色发光轮廓。
5. 退出世界再进入，确认剩余时间不会重置为 30 分钟。
6. 尝试召唤两次，应达到 4 个雇佣兵上限。
7. PVP 关闭时，其他玩家不能伤害雇佣兵；PVP 开启时，其他玩家可伤害，主人仍不能伤害自己的雇佣兵。
