# Raid Enhancement Patch 0.8.9.7.11 - Control Tokens & Cooldown Isolation

基线：0.8.9.7.10-leveltick-event-compat-hotfix。

本版本新增 4 个战场控制类战备令牌，并修复战备令牌使用后创造栏物品也显示冷却遮罩的问题。

## 新增令牌

- 初级洞察战备令牌 `basic_insight_token`
  - 配方：8 绿宝石 + 1 蜘蛛眼
  - 效果：接下来 3 波新刷出的袭击者获得原版 Glowing 发光效果。

- 高级洞察战备令牌 `advanced_insight_token`
  - 配方：8 绿宝石 + 1 末影之眼
  - 效果：后续全部波次新刷出的袭击者获得原版 Glowing 发光效果。

- 初级集敌战备令牌 `basic_rally_enemy_token`
  - 配方：8 绿宝石 + 1 回响碎片
  - 效果：将当前袭击中最多 5 名袭击者牵引至玩家附近。

- 高级集敌战备令牌 `advanced_rally_enemy_token`
  - 配方：8 绿宝石 + 1 下界之星
  - 效果：将当前袭击中最多 10 名袭击者牵引至玩家附近。
  - 默认不消耗，作为下界之星成本对应的可重复使用战术令牌。

## 冷却显示隔离

本版本不再使用原版 Player ItemCooldowns 给战备令牌加冷却，而是使用模组内部冷却表。
这样可以继续保留玩法冷却，同时避免创造模式物品栏中的同类物品显示“正在冷却”的遮罩。

## 配置文件

`config/raid_enhancement_patch/battle_support_items.properties`

新增配置：

```properties
insight.enabled=true
insight.basicWaveCount=3
insight.glowDurationTicks=6000
insight.cooldownTicks=1200

rally.enabled=true
rally.basicCount=5
rally.advancedCount=10
rally.basicCooldownTicks=1800
rally.advancedCooldownTicks=2400
rally.basicConsumesItem=true
rally.advancedConsumesItem=false
rally.skipRaidersAlreadyWithin=8
rally.teleportMinDistance=6
rally.teleportMaxDistance=12
rally.blacklistEntityIdContains=blimp,ender_dragon,wither,warden

internalTokenCooldowns.enabled=true
```

## 未改动范围

未改 HUD、第 8 波桥接、第 9～11 波状态机、Raids Enhanced 特殊袭击者表、原版 Raid 胜负状态、雇佣兵颜色系统、雇佣兵持久化系统、村民保护药水图标隐藏逻辑。
