# Step 9 - 0.9.0.0 Village Favor V2 Profession Gifts Alpha

本版基于 0.8.9.9.9 的村庄恩情结构化 V1，新增 V2 奖励扩展层。

## 已完成

- 增加 `raidMeritScore`、`highestOmenLevelWon`、周期礼物领取次数等持久化字段。
- 恩情等级改为由袭击胜利贡献分阈值计算，村民互动不会提高恩情等级。
- 玩家与村民互动时读取当前村民职业和职业交易等级。
- 新增职业组礼物池，职业缺失时安全回退 generic。
- 新增 giftTier，由恩情等级与村民职业等级共同决定，并受上限限制。
- 新增绿宝石奖励上限：单次礼物最多 `maxEmeraldPerGift`。
- 同一玩家 × 同一村庄共享礼物冷却和每日领取次数。
- 默认奖励保守，不默认给钻石、下界合金、高级魔法材料。

## 未改动

- 未修改袭击波次表。
- 未修改 numGroups 和第八波防崩溃 Mixin。
- 未修改独立 BossBar 状态条逻辑。
- 未修改胜利判定。
- 未修改村民交易价格。
- 未修改村民 AI。
- 未修改 Raids Enhanced 魔像回滚与掉落物清理。

## 新模块

- `VillagerProfessionResolver`
- `GiftTierResolver`
- `GiftLootResolver`
- `FavorGiftContext`

## 关键边界

职业礼物系统只是村庄恩情系统的奖励扩展层。它只在玩家与村民互动时触发，不做每 tick 扫描，不接管客户端判断，不接管袭击逻辑。
