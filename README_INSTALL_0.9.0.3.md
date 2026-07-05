# Raid Enhancement Patch 0.9.0.3 安装与测试说明

版本：`0.9.0.3-settlement-raidinstance-key-alpha`

## 版本定位

本版是 **settlementKey 热修通过候选版**，不是长期稳定版。

本版唯一核心目标：

```text
VictorySettlement 防重复结算使用 RaidInstanceKey，避免同村第二次袭击被旧 center-based settlement history 阻挡。
```

本版已在用户环境中完成游戏内测试，环境如下：

```text
Minecraft 1.21.1
NeoForge 21.1.234
Java 21
安装 Raids Enhanced 与 fdlib
```

## 安装方法

1. 备份存档。
2. 删除 `mods/` 中所有旧版 `raid_enhancement_patch` JAR。
3. 放入：

```text
raid_enhancement_patch-0.9.0.3-settlement-raidinstance-key-alpha.jar
```

4. 启动游戏。
5. 确认 `latest.log` 中出现：

```text
0.9.0.3-settlement-raidinstance-key-alpha
```

## 诊断日志建议

启动一次后检查：

```text
config/raid_enhancement_patch/key_diagnostics.properties
```

建议测试时开启：

```properties
enabled=true
log.raidDiscovery=true
log.settlement=true
log.favor=true
log.bossbar=true
log.storagePaths=true
log.intervalTicks=100
log.maxPlayerKeysPerLine=3
```

日志文件：

```text
config/raid_enhancement_patch/key_diagnostics.log
```

## 已确认通过的核心现象

日志已确认出现：

```text
settlementKeyMode=raidInstance
@raidInstance settlementKey
legacy-history-present-ignored
accepted-before-rewards
favor-record
```

并且 0.9.0.3 阶段未出现：

```text
duplicate-blocked-by-history
@raidInstance:fallback
重复发奖励
崩溃
```

## 已知问题

BossBar 视觉问题仍未修复：

```text
第一波 BossBar 会下降；
第二波 BossBar 视觉上不回充；
后续击杀后 BossBar 不继续正常下降。
```

该问题不属于 0.9.0.3 的修复范围，应进入后续 `0.9.0.4-bossbar-display-layer-hotfix-alpha` 专项处理。

## 本版禁止误判

不要因为第二次袭击中心点轻微漂移而回滚本版 settlementKey 修复。

已观察到一次中心点轻微变化：

```text
85,-55,189 -> 85,-54,190
```

当前判断：

```text
RaidInstanceKey 用于单次袭击结算，是正确方向。
VillageKey 用于村庄恩情长期归属，是另一个问题。
```

村庄区域识别稳定性不在 0.9.0.3 范围内，也不应混入 0.9.0.4 BossBar 修复版。
