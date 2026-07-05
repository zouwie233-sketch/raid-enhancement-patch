# Raid Enhancement Patch 0.8.9.4 - Village Golem Player Protection

基线版本：0.8.9.3-village-security-timeout-penalty

## 本次目标

本版本只补充一个低风险保护规则：袭击期间，村庄安防系统半径内的所有铁傀儡都不能被玩家伤害；袭击结束后，普通铁傀儡恢复正常可被玩家攻击。

## 新增配置

配置文件：

```text
config/raid_enhancement_patch/village_security.properties
```

新增项目：

```properties
strictRules.allowPlayerDamageToVillageGolemsDuringRaid=false
```

默认值为 `false`，含义是：

- 袭击期间，村庄范围内普通铁傀儡、命名铁傀儡、安防铁傀儡都不能被玩家伤害；
- 只拦截玩家直接攻击和玩家弹射物造成的伤害；
- 不拦截袭击者对铁傀儡造成的伤害；
- 袭击结束后，普通铁傀儡恢复正常行为；
- 若改为 `true`，玩家在袭击期间也可以伤害村庄铁傀儡。

## 没有改动

本版本没有改 HUD、额外波桥接、第 9～11 波状态机、离场回归冻结、Raids Enhanced 特殊袭击者生成、原版 Raid 胜负状态、袭击者清理或袭击者传送。

## 推荐测试

1. 袭击期间攻击村庄内普通铁傀儡，确认不掉血。
2. 袭击期间用弓/弩攻击村庄内普通铁傀儡，确认不掉血。
3. 袭击者攻击铁傀儡，确认仍然能造成伤害。
4. 袭击胜利后补回的普通铁傀儡，确认可以被玩家正常攻击。
5. 将 `strictRules.allowPlayerDamageToVillageGolemsDuringRaid=true` 后重启，确认袭击期间玩家可以攻击村庄铁傀儡。
