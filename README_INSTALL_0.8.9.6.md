# 安装说明 - 0.8.9.6

1. 删除旧版 `raid_enhancement_patch-0.8.9.5.2-return-reconcile-hotfix.jar`。
2. 放入新版 `raid_enhancement_patch-0.8.9.6-village-security-deployment-doctrine.jar`。
3. 配置文件仍位于：

```text
config/raid_enhancement_patch/village_security.properties
```

如果你的旧配置文件已经存在，新配置项不会自动插入到旧文件里，但代码会使用默认值。需要手动加入时，可复制以下内容：

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

修改配置后需要重启游戏或服务器。
