# 0.8.9.4 安装说明

## 安装环境

- Minecraft 1.21.1
- NeoForge 21.1.234
- 可选兼容：Raids Enhanced 1.0.2
- 可选兼容：fdlib 1.0.9

## 安装方法

将文件放入客户端或服务器的 `mods` 文件夹：

```text
raid_enhancement_patch-0.8.9.4-village-golem-player-protection.jar
```

如果已经安装旧版本，请删除旧版本 JAR，避免同时加载多个版本。

## 配置文件

启动后会生成：

```text
config/raid_enhancement_patch/village_security.properties
```

新增配置：

```properties
strictRules.allowPlayerDamageToVillageGolemsDuringRaid=false
```

默认 `false`：袭击期间村庄范围内所有铁傀儡都不能被玩家攻击。袭击结束后，普通铁傀儡恢复正常。

修改配置后需要重启游戏或服务器。
