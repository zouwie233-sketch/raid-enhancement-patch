# 0.9.1.6 配置审计摘要

版本：`0.9.1.6-config-audit-alpha`

性质：只读源码审计。没有删除、重命名、迁移或改变任何配置值。

## 按配置类统计

| 配置类 | 字段总数 | 文件加载项 | 已确认有效 | 已加载但无消费者/不生效 | 代码常量 |
|---|---:|---:|---:|---:|---:|
| BattleSupportConfig | 75 | 74 | 67 | 8 | 1 |
| KeyDiagnosticsConfig | 12 | 11 | 12 | 0 | 1 |
| RaidEnhancementConfig | 251 | 70 | 231 | 20 | 181 |
| RaidsEnhancedCompatConfig | 16 | 16 | 16 | 0 | 0 |
| VictorySettlementConfig | 30 | 29 | 28 | 2 | 1 |
| VillageFavorConfig | 33 | 32 | 31 | 2 | 1 |

## 重点结论

- `rareGiftChanceMultiplier`：**配置会被读取，但当前没有任何奖励/礼物运行时消费者读取该字段，因此目前不生效。**
- `equalXpPerEligiblePlayer`：**配置会被读取，但当前结算代码未读取该字段，因此目前不生效。**
- 职业礼物总开关、满级村民稀有礼物开关、礼物冷却、每日领取上限和绿宝石硬上限均存在运行时消费者。
- BossBar 诊断开关和审计间隔均存在运行时消费者；当前日志偏大主要是 VERBOSE 风格诊断开启，而不是 Gateway 本身。
- `RaidEnhancementConfig` 中存在大量代码常量，不全是用户可编辑配置。该事实只被记录，本版不配置化。

## 重点配置项

| 配置项 | 字段 | 默认值 | 是否生效 | 读取模块 | 建议 |
|---|---|---|---|---|---|
| log.bossbar | LOG_BOSSBAR | true | 有效 | com/noah/raidenhancement/raid/RaidIndependentBossbarManager.java；com/noah/raidenhancement/raid/RaidKeyDiagnostics.java | 保留 |
| bossbar.visibleAuthorityAudit.intervalTicks | BOSSBAR_VISIBLE_AUDIT_INTERVAL_TICKS | 20 | 有效 | com/noah/raidenhancement/raid/RaidIndependentBossbarManager.java | 保留 |
| equalXpPerEligiblePlayer | EQUAL_XP_PER_ELIGIBLE_PLAYER | true | 已读取但当前不生效 | 无外部读取 | 候选废弃；本版仅标记，不删除；配置被读取，但结算代码没有分支读取该开关 |
| giftCooldownTicks | GIFT_COOLDOWN_TICKS | 24000 | 有效 | com/noah/raidenhancement/favor/VillageFavorState.java | 保留 |
| enableProfessionGift | ENABLE_PROFESSION_GIFT | true | 有效 | com/noah/raidenhancement/favor/GiftTierResolver.java | 保留 |
| enableMasterVillagerRareGift | ENABLE_MASTER_VILLAGER_RARE_GIFT | true | 有效 | com/noah/raidenhancement/favor/GiftTierResolver.java | 保留 |
| giftClaimPeriodTicks | GIFT_CLAIM_PERIOD_TICKS | 24000 | 有效 | com/noah/raidenhancement/favor/VillageFavorState.java | 保留 |
| maxGiftClaimsPerVillagePerDay | MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY | 1 | 有效 | com/noah/raidenhancement/favor/VillageFavorState.java | 保留 |
| maxEmeraldPerGift | MAX_EMERALD_PER_GIFT | 8 | 有效 | com/noah/raidenhancement/favor/FavorReward.java；com/noah/raidenhancement/favor/GiftTierResolver.java | 保留 |
| maxEmeraldBonusByFavorLevel | MAX_EMERALD_BONUS_BY_FAVOR_LEVEL | 4 | 有效 | com/noah/raidenhancement/favor/GiftTierResolver.java | 保留 |
| maxEmeraldBonusByVillagerLevel | MAX_EMERALD_BONUS_BY_VILLAGER_LEVEL | 4 | 有效 | com/noah/raidenhancement/favor/GiftTierResolver.java | 保留 |
| rareGiftChanceMultiplier | RARE_GIFT_CHANCE_MULTIPLIER | 1.0D | 已读取但当前不生效 | 无外部读取 | 候选废弃；本版仅标记，不删除；配置被读取，但当前 GiftTierResolver/FavorReward 未使用该倍率 |

完整逐项表见 `CONFIG_AUDIT_0.9.1.6.csv`。
