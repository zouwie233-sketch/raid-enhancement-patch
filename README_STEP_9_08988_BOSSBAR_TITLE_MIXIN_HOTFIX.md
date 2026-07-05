# 0.8.9.8.8：BossBar 标题闪回修复

## 修复目标

0.8.9.8.7 已经清理旧自定义 HUD，但顶部原版袭击 BossBar 标题仍会在“袭击”和“袭击 第 X / Y 波”之间闪回。

## 原因

原版 `ServerBossEvent#setName` 仍可能把标题写回普通“袭击”，而补丁通过 tick 后反射再改标题，客户端看到交替更新。

## 修复

新增 `ServerBossEventRaidTitleMixin`，在 `ServerBossEvent#setName` 参数进入方法时直接把属于当前受管理 Raid 的标题替换为：

- `袭击 第 1 / 8 波`
- `袭击 第 9 / 11 波｜最后攻势！`
- `袭击 第 3 / 8 波｜清剿 02:15`

这样原版每次刷新 BossBar 名称时，写入的就是补丁标题，不再 tick 后抢写。

## 保留内容

- 保留 0.8.9.8.5+ 的 RaidWaveAuthority 波次权威层。
- 保留 1～8 原版负责、9～11 自定义额外波负责的结构。
- 不改奖励、扫荡、村庄恩情、战备令牌和 Raids Enhanced 魔像保护。
