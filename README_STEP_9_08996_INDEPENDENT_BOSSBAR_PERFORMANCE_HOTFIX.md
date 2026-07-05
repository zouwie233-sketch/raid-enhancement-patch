# Step 9 - 0.8.9.9.6 Independent BossBar Performance Hotfix

本版只处理 UI 闪回，不改变波次表、刷怪、胜利、奖励、村民保护、战备令牌、Raids Enhanced 魔像回滚与掉落物清理。

## 修改内容

1. 新增 `RaidIndependentBossbarManager`：
   - 为每场受管理袭击创建模组独立 `ServerBossEvent`。
   - 读取 `RaidEncounterAuthority` 的快照，不自行计算波次。
   - 隐藏原版 Raid BossBar，避免原版标题和模组标题反复抢写。
   - 只有独立 BossBar 创建成功后才隐藏原版 BossBar，失败时保留原版显示。

2. 停用旧标题覆盖路径：
   - 从 `raid_enhancement_patch.mixins.json` 移除 `ServerBossEventRaidTitleMixin`。
   - 旧类保留在 jar 中但不再作为 Mixin 加载。

3. 性能处理：
   - 玩家加入/移除同步每 20 tick 执行一次。
   - 标题只有变化时才 `setName`。
   - 进度变化超过 0.005 才 `setProgress`。
   - 反射方法查找使用缓存。
   - 不扫描全部玩家以外的实体，不做额外怪物查询。

## 保留基线

- 保留 0.8.9.9.5 的第八波崩溃防护。
- 保留当前波次表：简单 3/3/4/4/5，普通 5/6/6/7/8，困难 8/8/9/10/11。
- 不触碰战备令牌、村民保护、胜利结算、村庄恩情、战场扫荡和 Raids Enhanced 回滚保护。
