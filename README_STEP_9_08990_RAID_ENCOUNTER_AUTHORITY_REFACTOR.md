# 0.8.9.9.0：Raid Encounter Authority 第一版结构重构

## 改动重点

1. 新增 `RaidEncounterAuthority`：保存每场袭击的统一运行期只读快照。
2. 新增 `RaidEncounterSnapshot`：把 UI、波次、额外波、清剿计时等事实合并为一个只读数据结构。
3. 新增 `RaidBossbarTitleFormatter`：BossBar 标题只允许一个格式器生成。
4. 新增 `RaidVictoryGate`：第一版纯规则胜利门控，为后续结算统一入口做准备。
5. `RaidWaveExpansionEvents` 拆分 Pre/Post：
   - Pre：只修原版 `numGroups`。
   - Post：处理额外波、UI 快照、胜利结算。
6. `RaidExtraWaveController` 在发布 HUD 快照时同步发布 `RaidEncounterSnapshot`。
7. 加入波次审计日志：输出 difficulty、rawOmen、normalizedOmen、target、nativeLimit、numGroups、groupsSpawned、customExtraWaves。
8. 保留原版 BossBar 位置显示，不恢复旧灰底 HUD，不恢复下方 Actionbar。

## 没有改动

- 没有改战备令牌注册链。
- 没有改村民保护和安防铁傀儡规则。
- 没有改胜利奖励 JSON 格式。
- 没有改 Raids Enhanced 魔像方块回滚和掉落物清理。
- 没有改当前波次表数值。

## 当前仍不是最终形态

这一版是低风险第一阶段：建立权威读模型并减少双 Tick 推进风险。`RaidExtraWaveController` 仍然是大类，后续应继续把桥接、胜利门控、运行期 session key 迁移到真正的 `RaidEncounterManager / RaidRuntimeSession`。
