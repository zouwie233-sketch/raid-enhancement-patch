# raid_enhancement_patch 0.8.9.3 安装说明

## 环境

- Minecraft：1.21.1
- NeoForge：21.1.234
- 可选兼容：Raids Enhanced 1.0.2
- 可选兼容：fdlib 1.0.9

## 安装

将以下文件放入游戏或服务器的 `mods` 文件夹：

```text
raid_enhancement_patch-0.8.9.3-village-security-timeout-penalty.jar
```

如果已经安装旧版本，请先移除旧版 JAR，避免同一 modid 重复加载。

## 配置文件

第一次启动后会生成：

```text
config/raid_enhancement_patch/village_security.properties
config/raid_enhancement_patch/village_security.default.properties
```

修改配置后需要重启游戏或服务器。

## 超时惩罚默认值

```properties
timeoutPenalty.enabled=true
timeoutPenalty.damage=2.0
timeoutPenalty.minVillagerHealth=2.0
timeoutPenalty.oncePerWave=true
timeoutPenalty.skipIfWaveDefenseAlreadyFailed=false
timeoutPenalty.requireKnownLivingRaiders=true
timeoutPenalty.includeInBattleSummary=true
messages.timeoutPenalty=true
debug.timeoutPenalty=false
```

默认含义：每波清剿超时后，如果仍有已知存活袭击者，附近村民受到 2 点生命压力；同一波默认只触发一次；不会直接替代原版 Raid 失败。

## 推荐测试

1. 正常完成袭击，确认完胜/惨胜文本正常；
2. 故意拖到清剿超时，确认村民扣 2 点生命；
3. 确认超时惩罚不会把村民直接扣死；
4. 确认玩家离场进入清剿计时暂停后，不触发超时惩罚；
5. 确认战后总结出现清剿超时次数和超时受冲击人次；
6. 确认 HUD、额外波、离场/回归、Raids Enhanced 特殊袭击者仍然正常。
