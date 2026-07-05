# 第九步热修：困难 + 不祥 1 波次桥接修复

## 问题

用户反馈：困难 + 不祥 1 级袭击实际只有 7 波，但 HUD 显示 8 波。

## 判断

这不是单纯 UI 问题。旧逻辑存在两个不一致点：

1. `RaidWaveExpansionPlan.hard(omen=1)` 仍计划 7 波。
2. `estimatedNativeTotalWaves()` 会把理论不祥额外波加入估算，导致 HUD / 状态目标被抬到 8。
3. `ExtraWaveState.effectiveNativeWaves()` 又把这个估算当成原版已经能处理的原生波次，导致 `extraWavesNeeded()` 变成 0。
4. 实机中原版只生成 7 组后结束，于是第 8 波没有被自定义桥接层补出。

## 修复

- 困难 + 不祥 1 的计划目标明确改为 8。
- `nativeOminousBonusWave()` 返回 0，不再把理论不祥额外波直接计入原生目标。
- `effectiveNativeWaves()` 不再信任 `observedNativeTargetWaves` 作为已生成证明，只信任 `maxObservedNativeGroupsSpawned`。
- 如果原版确实生成了第 8 组，则 `groupsSpawned > nativeSafeWaves` 会被承认。
- 如果原版只到第 7 组就结束，则自定义桥接层会补出逻辑第 8 波。

## 保留内容

- 保留三档胜利结算。
- 保留 JSON 奖池。
- 保留战场扫荡。
- 保留村庄恩情第一版。
- 保留 9～11 波额外波状态机。
- 保留 Raids Enhanced 魔像回滚和掉落物清理。
- 保留 0.8.9.8.1 的 LevelTickEvent 反射兼容热修。
