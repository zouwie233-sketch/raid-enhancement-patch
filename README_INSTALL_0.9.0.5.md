# README_INSTALL_0.9.0.5

版本：`0.9.0.5-bossbar-visible-authority-audit-alpha`

## 定位

这是 BossBar 可见权威诊断版，不是最终修复版。

本版目标只有一个：确认玩家屏幕上看到的 BossBar 是否为本 Mod 创建的独立 BossBar。

## 安装环境

```text
Minecraft 1.21.1
NeoForge 21.1.234 或更高 21.1.x
Java 21
Raids Enhanced：可安装
fdlib：可安装
```

## 安装方法

1. 删除 `mods/` 中旧版 `raid_enhancement_patch-*.jar`。
2. 放入：

```text
raid_enhancement_patch-0.9.0.5-bossbar-visible-authority-audit-alpha.jar
```

3. 启动游戏，确认 `latest.log` 中出现 `0.9.0.5-bossbar-visible-authority-audit-alpha`。

## KeyDiag 配置

打开：

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

建议：

```properties
enabled=true
log.bossbar=true
bossbar.visibleAuthorityAudit.enabled=true
bossbar.visibleAuthorityAudit.intervalTicks=20
bossbar.visibleAuthorityAudit.temporaryRepTitleMarker=true
```

如果已有旧配置文件，新增字段不会自动写入旧文件，但代码默认值为 true。也可以参考自动生成的：

```text
config/raid_enhancement_patch/key_diagnostics.default.properties
```

## 测试目标

触发困难 + 不祥 1 级袭击后，观察屏幕顶部 BossBar：

```text
如果玩家看到的 BossBar 标题带 [REP]，说明玩家看到的是本 Mod 的独立 BossBar。
如果玩家看到的 BossBar 不带 [REP]，说明玩家看到的很可能是原版 / Raids Enhanced / 其他残留 BossBar。
```

同时保存：

```text
latest.log
config/raid_enhancement_patch/key_diagnostics.log
```

重点搜索：

```text
BossBarAuthorityAudit
visibleAuthorityQuestion=does-player-see-[REP]
independentTitle=
independentProgress=
independentPlayerCount=
vanillaTitle=
vanillaProgress=
vanillaPlayerCount=
hide-vanilla
post-wave-next-tick
```

## 已知问题

0.9.0.4 实测失败：第二波服务端 progress=1.0000，但玩家视觉仍不回充。
0.9.0.5 不承诺修好 BossBar，只用于确认可见 BossBar 权威对象。

## 禁止误用

不要把本版标记为稳定版。
不要用本版结论替代 0.9.0.3 settlementKey 回归测试。
