# raid_enhancement_patch 0.8.9.3-village-security-timeout-penalty

## 版本定位

基线：0.8.9.2-village-security-polish

本版本只补充“清剿超时惩罚”这一块，并把它接入已经稳定的村庄安防系统。它不替代原版 Raid 失败，不清除袭击者，不传送袭击者，不强制结束袭击，不改 HUD，不改第 8 波桥接，不改第 9～11 波额外波状态机。

## 新增功能

### 1. 每波清剿超时惩罚

当动态清剿时限耗尽，并且当前波次仍然有已知存活袭击者时，村民防卫同盟会触发一次超时惩罚。

默认规则：

- 每波最多触发一次；
- 默认对附近村民造成 2 点生命值压力；
- 村民最低保留 2 点生命值，避免超时惩罚直接杀死村民；
- 扣血后同步更新村民 allowedHealth，兼容 0.8.8.9 的禁外部回血机制；
- 玩家离场导致清剿计时暂停时，不触发超时惩罚；
- 必须确认当前波仍有已知存活袭击者，避免桥接/回归/状态空转误判。

### 2. 超时聊天提示

新增“村民防卫同盟”清剿超时文本池。触发时最多两条聊天提示：

- 第几波清剿超时；
- 村民因战斗拖延受到生命压力。

### 3. 战后总结接入超时统计

0.8.9.2 的战后总结现在会统计：

- 防守失败次数；
- 清剿超时次数；
- 防线崩溃受冲击人次；
- 超时受冲击人次；
- 回收临时安防铁傀儡数量；
- 胜利后普通雇佣军返回数量。

### 4. 完胜/惨胜判定更新

- 完胜：没有任何防守失败，也没有任何清剿超时；
- 惨胜：最终胜利，但出现过防线崩溃或清剿超时。

### 5. 新增配置项

新增配置：

```properties
timeoutPenalty.enabled=true
timeoutPenalty.damage=2.0
timeoutPenalty.minVillagerHealth=2.0
timeoutPenalty.oncePerWave=true
timeoutPenalty.skipIfWaveDefenseAlreadyFailed=false
timeoutPenalty.requireKnownLivingRaiders=true
timeoutPenalty.includeInBattleSummary=true
messages.timeoutPenalty=true
debug.timeoutPenalty=false
```

## 保持不变

本版本没有改动：

- HUD；
- LevelTickEvent 事件桥；
- 第 8 波 → 第 9 波桥接；
- 第 9～11 波额外波状态机；
- 离场 / 回归冻结逻辑；
- Raids Enhanced 特殊袭击者生成；
- 原版 Raid 胜负状态；
- 袭击者清理或传送逻辑。
