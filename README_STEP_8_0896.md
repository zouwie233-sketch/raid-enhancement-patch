# Raid Enhancement Patch 0.8.9.6 - Village Security Deployment Doctrine

基线：0.8.9.5.2-return-reconcile-hotfix。

本版本只优化村庄安防铁傀儡的战场逻辑，不改 HUD、不改第 8 波桥接、不改第 9～11 波额外波状态机、不改 Raids Enhanced 特殊袭击者生成、不改原版 Raid 胜负状态。

## 新增功能

1. 每波开始后重新防御部署现有安防铁傀儡。
   - 防止安防铁傀儡经过多次战斗后离村庄中心过远。
   - 默认把存活的安防铁傀儡重新部署到村庄中心附近防御环。

2. 新刷安防铁傀儡支持更多出生点位。
   - 默认 8 个出生点。
   - 新增安防铁傀儡会在多个点位间轮换投送，降低扎堆。

3. 安防铁傀儡获得正规军抗性效果。
   - 默认启用 Resistance I。
   - 通过配置可关闭、调整等级和持续时间。

4. 存活安防铁傀儡进入下一波时恢复生命。
   - 默认每波开始将存活安防铁傀儡修复满血。
   - 可配置为关闭或改为固定恢复量。

5. 新增村民防卫同盟文本反馈。
   - 重新部署文本。
   - 存活单位生命恢复文本。
   - 正规军防护协议 / 抗性效果文本。

## 新增配置项

```properties
messages.redeployment=true
messages.combatBuff=true
securityGolems.spawnPointCount=8
securityGolems.redeployExistingAtWaveStart=true
securityGolems.redeployRingRadius=14
securityGolems.repairSurvivorsOnWaveStart=true
securityGolems.survivorRepairToFull=true
securityGolems.survivorRepairHealth=10.0
securityGolems.resistance.enabled=true
securityGolems.resistance.level=1
securityGolems.resistance.durationTicks=1200
```

## 测试建议

1. 让安防铁傀儡追敌离开村庄中心，进入下一波时确认它们会重新回到防御阵位。
2. 调高 `securityGolems.spawnPointCount`，观察新刷铁傀儡是否不再集中于少数点位。
3. 检查安防铁傀儡是否获得抗性效果。
4. 检查存活安防铁傀儡进入下一波时是否恢复生命。
5. 确认普通铁傀儡/普通雇佣军不获得这些正规军效果。
