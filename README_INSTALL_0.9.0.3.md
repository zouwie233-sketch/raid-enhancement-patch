# Raid Enhancement Patch 0.9.0.3 安装与测试说明

版本：`0.9.0.3-settlement-raidinstance-key-alpha`

## 安装

1. 删除旧版 `raid_enhancement_patch` JAR。
2. 放入本版构建出的 JAR。
3. 启动一次游戏，确认配置存在。
4. 建议开启：

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

配置文件：

`config/raid_enhancement_patch/key_diagnostics.properties`

## 本版目标

只修 VictorySettlement 防重复结算 key：

- 防重复结算使用 RaidInstanceKey。
- VillageFavor 仍使用 VillageKey。
- 同村第二次袭击不能被旧 history 阻挡。

## 本版不处理

- 不修 BossBar 视觉回充。
- 不改袭击波次。
- 不改奖励。
- 不改村民礼物。
- 不改 Mixin。
- 不迁移全部持久化数据。

## 必测

1. 同一村庄连续完成两次袭击。
2. 第二次袭击是否仍结算。
3. 第二次袭击是否写入 favor-record。
4. 日志中 settlementKey 是否为 `settlementKeyMode=raidInstance`。
5. 旧 center-key history 是否只出现 `legacy-history-present-ignored`，不再阻挡结算。
