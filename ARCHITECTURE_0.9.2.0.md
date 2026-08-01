# 0.9.2.0 架构说明

本版本完成 `ARCH-1.1` 首批基础设施，采用“先验证生命周期容器，再迁移权威玩法状态”的渐进方案。

## 新增边界

```text
ServerStartingEvent
  -> RaidRuntimeRegistry.start(server)
      -> RaidRuntimeContext
          -> RaidDiagnosticsContext

ServerStoppingEvent
  -> beginStopping
  -> legacy queue checkpoint
  -> context checkpoint

ServerStoppedEvent
  -> legacy runtime clear
  -> registry remove
  -> context close
  -> diagnostic state release
```

`RaidRuntimeRegistry` 是唯一允许保留的进程级服务器查找表。它使用对象身份而不是 `equals` 区分服务器，并在 stopped 事件显式删除条目。

`RaidRuntimeContext` 不暴露通用 Map，也不负责波次、生成、结算或 UI，因此不是新的上帝类。本阶段仅拥有 `RaidDiagnosticsContext`。

## 后续迁移顺序

1. 完成 GitHub 构建和跨存档实机验证。
2. 迁移其他纯缓存和诊断状态。
3. 迁移 BossBar/只读投影状态。
4. 迁移村庄防御、支援等服务状态。
5. 最后迁移 `RaidEncounter` 核心权威状态。
