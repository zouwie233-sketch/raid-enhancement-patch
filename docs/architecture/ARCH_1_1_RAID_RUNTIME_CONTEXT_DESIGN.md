# ARCH-1.1：RaidRuntimeContext 详细设计

状态：首批实施完成，等待 GitHub 构建与实机验证  
范围：只建立服务器作用域和关闭协议；首批不得改变波次、编成、奖励或持久化格式。

## 1. 要解决的问题

当前多个模块以进程级静态集合保存运行态。集成服务器退出到主菜单后 JVM 仍然存在，旧世界状态可能继续保留；不同世界的 `gameTime` 也不能互相比较。

`RaidRuntimeContext` 的目的不是成为新的上帝对象，而是提供生命周期和依赖容器。具体状态仍由各专属组件拥有。

## 2. 目标结构

```text
RaidRuntimeRegistry
  `-- MinecraftServer -> RaidRuntimeContext
        |-- EncounterRuntimeStore
        |-- RaidSessionStore
        |-- VillageDefenseRuntimeStore
        |-- VillagerProtectionIndex
        |-- BattleSupportRuntimeStore
        |-- MercenaryIndex
        |-- PresentationRegistry
        |-- WorkBudgetRegistry
        `-- RaidDiagnosticsContext
```

### 边界说明

- `RaidRuntimeRegistry` 只负责按 `MinecraftServer` 查找、创建和移除上下文。
- `RaidRuntimeContext` 只负责组合依赖和执行关闭顺序。
- 各 Store/Index 负责自身数据，不允许 Context 暴露通用可变 Map。
- 领域服务依赖窄接口，不直接访问 Registry。
- 是否能使用 NeoForge 原生附件承载服务器上下文，需要在实施时依据当前 NeoForge 版本 API 编译验证；验证前不假定 API 名称。若不可用，采用显式服务器生命周期 Registry，并在 `ServerStoppedEvent` 强制移除。

## 3. 生命周期

| 事件 | 动作 | 失败策略 |
|---|---|---|
| Server about-to-start/starting | 创建唯一 Context，注册只读基础依赖 | 创建失败则禁用 Mod 运行模块并明确报错，不留下半初始化 Context |
| Level load | 创建或登记维度级视图/预算 | 单维度失败不得污染其他维度 |
| 正常 tick | 由事件入口取得 Context，调用应用服务 | 每个阶段独立错误边界 |
| Server stopping | 冻结新命令，完成必要 checkpoint | 写入失败保留脏标记并输出可定位错误 |
| Server stopped | 清空纯缓存、解除世界和实体引用、从 Registry 移除 | 清理应幂等，可安全调用两次 |

关闭顺序：

1. 拒绝新的袭击写命令。
2. 持久化已标记为脏的世界事实。
3. 解除 BossBar 和展示订阅。
4. 清空实体索引、诊断缓存和预算。
5. 关闭各 Store。
6. 从 Registry 删除服务器键。

## 4. API 草案

```java
public interface RaidRuntimeAccess {
    RaidRuntimeContext require(MinecraftServer server);
    Optional<RaidRuntimeContext> find(MinecraftServer server);
}

public final class RaidRuntimeContext implements AutoCloseable {
    public EncounterRuntimeStore encounters();
    public RaidSessionStore sessions();
    public RaidDiagnosticsContext diagnostics();
    public boolean isClosing();
    public void checkpoint();
    @Override public void close();
}
```

这些 API 只是设计形状，不承诺最终类名。实施前必须根据现有调用点验证最小参数迁移范围。

## 5. 线程模型

- 所有玩法写操作必须发生在 Minecraft 服务端线程。
- Context 不以 `synchronized` 掩盖错误线程调用；开发构建应能检查服务端线程不变量。
- 配置解析可以在重载准备阶段构建不可变对象，最终替换在规定线程完成。
- 后台任务不得直接访问世界实体或修改聚合。

## 6. 首批迁移范围

首批实现范围：

1. 已新增 Registry、Context、关闭协议和生命周期契约测试。
2. 已迁移没有玩法写权限的 KeyDiagnostics 限流和结算边界 warn-once 状态。
3. 已增加静态运行边界检查和 GitHub JDK-only 契约测试。
4. 已保持现有 `RaidExtraWaveController`、生成队列、胜利门控和持久化格式不变。

首批明确不允许：

- 迁移 `ExtraWaveState`。
- 修改最终波次时间和刷新条件。
- 修改 Mixin 胜利拦截。
- 修改安全生成逻辑。
- 修改奖励、好感或配置默认值。
- 删除任何旧状态表。

完成首批验证后，第二批再逐个迁移只读投影、Session、村庄防御和核心 Encounter 状态。

## 7. 验收标准

1. 同一 JVM 中连续启动两个存档，诊断状态和新 Context 身份完全隔离。
2. `ServerStoppedEvent` 后 Registry 中没有服务器条目。
3. Context 关闭两次不抛异常。
4. 无强引用保留旧 `MinecraftServer`、`ServerLevel` 或实体。
5. 无活动袭击时不增加实体扫描或额外世界查询。
6. 当前困难 + 不祥 V、最终波次退出重进测试结果不变。
7. GitHub 构建和测试通过后，只交付源码 ZIP，不要求本地 JAR。

## 8. 回滚策略

首批不修改持久化格式和核心状态所有者。若生命周期 Context 出现问题，可以删除新事件注册和 Context 接入点，旧控制器仍保持原行为，不需要迁移或回滚世界数据。
