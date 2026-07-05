# Raid Enhancement Patch 0.8.9.5.2 - Return/Reconcile Hotfix

基线：0.8.9.5.1-world-load-hotfix。

本次只修复两类边界问题：

1. 玩家离开袭击战场后返回时，安防铁傀儡可能因实体/区块尚未重新加载而被误判为全灭，导致本波防守失败。
2. 大型村庄中部分村民可能位于原安防半径边缘之外，导致防线崩溃或清剿超时扣血未覆盖到这些村民。

新增/调整：

- `performanceOptimization.securityGolemReturnGraceTicks=200`
  - 玩家离场/返回或安防铁傀儡 UUID 暂时无法解析时，默认给予 200 tick 的重载宽限。
  - 宽限期间不会立刻判定安防铁傀儡全灭。

- `performanceOptimization.securityGolemMissingPruneGraceTicks=1200`
  - 安防铁傀儡 UUID 暂时找不到时不会马上从追踪列表移除。
  - 默认 1200 tick 后仍无法找回才清理追踪记录，避免长期内存残留。

- `strictRules.villagerPenaltyRadius=128`
  - 防线崩溃与清剿超时的村民扣血使用更大的惩罚半径。
  - 如果村庄非常大，可以在配置文件中继续调高。

本次没有改 HUD、第 8 波桥接、第 9～11 波额外波状态机、Raids Enhanced 特殊袭击者生成、原版 Raid 胜负状态、清怪或传送逻辑。
