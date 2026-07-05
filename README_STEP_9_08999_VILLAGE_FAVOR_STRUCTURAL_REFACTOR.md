# 0.8.9.9.9 村庄恩情系统结构性重构说明

本版本基于 `0.8.9.9.8-independent-bossbar-dynamic-progress-hotfix`，不修改袭击波次、状态条、第八波防崩溃、胜利结算奖励、村民保护、安防铁傀儡和 Raids Enhanced 方块回滚。

## 目标

把旧的“胜利结算类内部恩情点数”改成服务端权威、村庄区域级、玩家 UUID 独立的长期恩情关系系统。

## 新增模块

- `VillageFavorConfig`：集中管理恩情系统配置。
- `VillageFavorState`：服务端持久化权威状态，保存玩家 × 被拯救村庄关系。
- `VillageFavorRecord`：单条玩家与村庄关系记录。
- `VillageFavorSystem`：恩情系统门面，连接胜利捕捉、交互、奖励和展示。
- `VillageFavorEvents`：只监听玩家与村民交互，不做 tick 扫描。
- `FavorReward`：负责恩情礼物计算和发放。
- `FavorDisplay`：负责聊天、粒子、音效反馈，不参与真实判定。

## 数据模型

每条记录包含：

- `dimension`
- `centerX / centerY / centerZ`
- `radius`
- `player UUID`
- `favorLevel`
- `victoryCount`
- `lastGiftTime`
- `lastGreetingTime`
- `totalClaimedGiftCount`
- `dataVersion`

## 触发流

袭击胜利结算：

```text
VictorySettlementController
→ VillageFavorSystem.recordRaidVictory
→ VillageFavorState.recordVictory
→ FavorDisplay.sendVictoryRecorded
```

玩家与村民互动：

```text
VillageFavorEvents
→ VillageFavorSystem.onVillagerInteracted
→ VillageFavorState.findForInteraction
→ FavorReward.tryGiveGift
→ FavorDisplay.sendGreeting / sound / particles
```

## 性能边界

- 不每 tick 扫描村民。
- 不每 tick 扫描玩家。
- 只在袭击胜利和玩家与村民互动时处理恩情逻辑。
- 礼物冷却按“玩家 × 村庄”统一保存。
- 调试日志默认关闭。

## 禁止回归区域

本版本没有修改：

- 袭击波次表。
- `numGroups` 修复逻辑。
- 第八波防崩溃 Mixin。
- 独立 BossBar 动态状态条。
- 胜利结算奖励 JSON 主流程。
- 村民保护和安防铁傀儡。
- Raids Enhanced 魔像方块回滚和掉落清理。
