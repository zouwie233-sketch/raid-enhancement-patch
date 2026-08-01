# 0.9.2.0 Architecture Runtime Context Alpha

## 架构变化

- 新增按具体 `MinecraftServer` 身份隔离的 `RaidRuntimeRegistry`。
- 新增只负责生命周期和依赖组合的 `RaidRuntimeContext`。
- 新增 JDK-only `ServerScopedContextStore`，明确服务器身份、删除和关闭语义。
- 将 KeyDiagnostics 的日志限流表和结算边界 warn-once 标记迁入服务器作用域。
- 在 `ServerStartingEvent` 创建 Context，在 stopping 阶段冻结/checkpoint，在 stopped 阶段移除并关闭。
- GitHub Actions 新增服务器作用域契约测试。

## 明确未改变

- 不修改额外波次数量和袭击者编成。
- 不修改困难 + 不祥 V 最终连续波次逻辑。
- 不修改胜利 Mixin 和完成判定。
- 不修改有界生成队列、重试、恢复或持久化格式。
- 不修改安全出生点的碰撞、流体、危险方块、支撑、世界边界和已加载区块检查。
- 不修改 BossBar、奖励、结算 Key、VillageFavor 或配置默认值。
- 不新增实体扫描和 tick 世界查询。

## 当前阶段限制

核心袭击运行态仍由旧控制器拥有。本版本只验证新的服务器生命周期边界，不提前迁移 `ExtraWaveState`。
