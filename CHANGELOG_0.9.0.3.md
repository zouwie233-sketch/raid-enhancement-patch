# CHANGELOG 0.9.0.3

版本：`0.9.0.3-settlement-raidinstance-key-alpha`

## 修改范围

本版只修改 VictorySettlement 防重复结算 key。

## 核心变更

- `VictorySettlementController` 的胜利结算防重复 key 改为 RaidInstanceKey。
- 保留旧 center-based settlement history 检查，但旧 history 只记录为 `legacy-history-present-ignored`，不再直接阻挡新袭击结算。
- 当新的 RaidInstanceKey 已经结算过时，才允许以 `duplicate-blocked-by-raid-instance-history` 阻挡重复结算。
- `RaidKeyDiagnostics` 增强 settlement 诊断输出，用于确认 `settlementKeyMode=raidInstance`。

## 保留不变

- VillageFavor 恩情系统保持原路径。
- 村民礼物系统不改。
- 奖励系统不改。
- BossBar 算法不改。
- 袭击波次数系统不改。
- RaidWaveAuthority 不改。
- RaidWaveExpansionController 不改。
- 第八波防崩 Mixin 不改。
- Mixin 配置不改。

## 实测结论

用户在以下环境中完成测试：

```text
Minecraft 1.21.1
NeoForge 21.1.234
Java 21
安装 Raids Enhanced 与 fdlib
```

确认：

```text
settlementKeyMode=raidInstance
@raidInstance settlementKey
legacy-history-present-ignored
accepted-before-rewards
favor-record
```

未发现：

```text
duplicate-blocked-by-history
@raidInstance:fallback
重复发奖励
崩溃
```

## 已知问题

- BossBar 玩家视觉层仍存在不回充 / 后续不继续下降问题。
- BossBar 问题应在 `0.9.0.4-bossbar-display-layer-hotfix-alpha` 中单独修复。
- 第二次同村袭击观察到 center 轻微漂移：`85,-55,189 -> 85,-54,190`。该现象记录归档，不作为回滚 RaidInstanceKey 的理由。
