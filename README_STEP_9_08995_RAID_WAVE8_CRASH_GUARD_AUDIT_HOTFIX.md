# 0.8.9.9.5 - Raid Wave 8 Crash Guard Audit Hotfix

## 根因

0.8.9.9.4 中 `RaidWaveIndexClampMixin` 已经成功加载，但崩溃报告仍显示：

`Raid#spawnGroup -> Raid#getDefaultNumSpawns(index=8)`

这说明原来的 `@ModifyVariable(method = "getDefaultNumSpawns", at = HEAD)` 兜底没有实际改写从 `spawnGroup` 传入的参数。

## 修复

在 `RaidWaveIndexClampMixin` 中新增更强的调用点钳制：

`@ModifyArg(method="spawnGroup", target="Raid#getDefaultNumSpawns(RaiderType,int,boolean)", index=1)`

这样在 `spawnGroup` 调用 `getDefaultNumSpawns` 前，直接把 wave 参数钳制到 `0..7`，用于阻止第 8 波数组越界。

## 未改动

- 没有改波次目标表
- 没有改奖励、恩情、战备令牌
- 没有改村民保护、安防铁傀儡
- 没有改 Raids Enhanced 魔像回滚和掉落物清理

## 后续

如果本版仍然在第 8 波崩溃，下一步应停止继续把原版 `numGroups` 推到 8，改为“原版最多 7 波 + 模组自定义补第 8 波”的保守桥接方案。
