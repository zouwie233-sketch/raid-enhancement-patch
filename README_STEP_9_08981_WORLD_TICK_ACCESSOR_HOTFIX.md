# 第九步热修：0.8.9.8.1 world tick accessor hotfix

## 问题

用户反馈安装 `0.8.9.8.0-victory-settlement-sweep-favor-alpha` 后，启动世界崩溃。

崩溃核心：

```text
java.lang.NoSuchMethodError: 'java.lang.Object net.neoforged.neoforge.event.tick.LevelTickEvent$Pre.getLevel()'
 at com.noah.raidenhancement.event.RaidWaveExpansionEvents.onLevelTickPre(...)
```

## 原因

构建时直接调用了 `LevelTickEvent.Pre/Post#getLevel()`，生成的字节码描述符与用户 NeoForge 21.1.234 实际运行时不一致。项目之前已经踩过 `LevelTickEvent.Post#getLevel()` 签名坑，交接文件也提醒后续应尽量用兼容/反射路径。

## 修复

将以下事件监听器的 `event.getLevel()` 改为 `callNoArg(event, "getLevel")` 反射调用：

- `RaidWaveExpansionEvents.onLevelTickPre`
- `RaidWaveExpansionEvents.onLevelTickPost`
- `VillagerProtectionEvents.onLevelTickPost`

这样字节码不再绑定 `getLevel()` 的返回值描述符，避免 NeoForge 运行时签名差异导致世界 tick 崩溃。

## 验证

使用 `javap -c` 检查热修后字节码，确认不再存在直接 `invokevirtual LevelTickEvent$Pre.getLevel:()Ljava/lang/Object;` 调用，改为：

```text
invokestatic callNoArg:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
```

## 交付

- `raid_enhancement_patch-0.8.9.8.1-world-tick-accessor-hotfix.jar`
- `raid_enhancement_patch-0.8.9.8.1-world-tick-accessor-hotfix-source.zip`
