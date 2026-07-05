# raid_enhancement_patch 0.8.9.2 安装说明

## 1. 安装环境

推荐环境：

- Minecraft：1.21.1
- NeoForge：21.1.234
- Raids Enhanced：1.0.2
- fdlib：1.0.9

Raids Enhanced 与 fdlib 是可选兼容依赖；如果你的整合包使用 Raids Enhanced，请同时安装 fdlib。

## 2. 安装方法

把文件：

```text
raid_enhancement_patch-0.8.9.2-village-security-polish.jar
```

放入游戏或服务器的：

```text
mods
```

文件夹。

如果旧版本 jar 仍在 mods 文件夹里，请删除旧版本，只保留一个 raid_enhancement_patch jar。

## 3. 配置文件

第一次启动后会生成：

```text
config/raid_enhancement_patch/village_security.properties
```

同时会生成参考默认配置：

```text
config/raid_enhancement_patch/village_security.default.properties
```

修改配置后需要重启游戏或服务器。

## 4. 本版本新增配置

```properties
messages.victoryGrade=true
messages.battleSummary=true

debug.enabled=false
debug.wavePreparation=false
debug.breach=false
debug.completion=false
debug.golemTrim=false
```

说明：

- messages.victoryGrade：控制完胜/惨胜文本。
- messages.battleSummary：控制战后总结文本。
- debug.enabled：debug 总开关。
- debug.wavePreparation：每波安防整备日志。
- debug.breach：防线崩溃/村民扣血日志。
- debug.completion：袭击结束清理日志。
- debug.golemTrim：普通雇佣军清退日志。

正常游玩建议保持 debug 全部关闭。

## 5. 推荐测试项目

建议测试：

1. 安防铁傀儡没有全灭并胜利时，是否出现“完胜”。
2. 至少一波安防铁傀儡全灭但最终胜利时，是否出现“惨胜”。
3. 防守失败次数是否进入战后总结。
4. 非胜利结束时是否出现失败总结，且村民不会获得胜利治疗。
5. debug.enabled=false 时，控制台是否没有大量安防详细日志。
6. debug.enabled=true 并开启子项后，是否能看到对应诊断日志。

## 6. 本版本未改动内容

本版本没有改：

- HUD；
- 第 8 波到第 9 波桥接；
- 第 9 到 11 波额外波状态机；
- 离场/回归冻结逻辑；
- Raids Enhanced 特殊袭击者生成逻辑；
- 原版 Raid 失败状态；
- 袭击者清理或传送逻辑。
