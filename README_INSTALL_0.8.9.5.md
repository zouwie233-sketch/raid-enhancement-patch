# 0.8.9.5 安装说明

## 环境

- Minecraft：1.21.1
- NeoForge：21.1.234
- 可选兼容：Raids Enhanced 1.0.2
- 可选兼容：fdlib 1.0.9

## 安装

把以下文件放入客户端或服务器的 `mods` 文件夹：

```text
raid_enhancement_patch-0.8.9.5.1-world-load-hotfix.jar
```

如果旧版本仍在 `mods` 文件夹中，请先移除旧版本，只保留一个 raid_enhancement_patch JAR。

## 配置文件

第一次启动后会生成：

```text
config/raid_enhancement_patch/village_security.properties
```

0.8.9.5 新增的性能相关配置包括：

```properties
performanceOptimization.raiderCacheScanIntervalTicks=20
performanceOptimization.stragglerGlowIntervalTicks=40
performanceOptimization.raiderAuditIntervalTicks=200
performanceOptimization.specialRaiderScanIntervalTicks=100
performanceOptimization.timeoutPenaltyCheckIntervalTicks=20
performanceOptimization.activeSessionFastCheck=true
performanceOptimization.cleanupEndedSessionsAggressively=true
debug.performance=false
```

修改配置后需要重启游戏或服务器。

## 推荐默认值

建议保留默认值测试。默认值偏保守，优先保证已经稳定的袭击桥接、离场回归、残兵定位、超时审计和村庄安防系统不被破坏。

## 排查性能日志

如果需要查看性能清理日志，使用：

```properties
debug.enabled=true
debug.performance=true
```

平时建议保持关闭，避免控制台刷屏。
