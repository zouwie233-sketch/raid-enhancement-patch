# 0.9.2.1 Cross-Save Lifecycle Isolation Alpha

## 修复

- 修复袭击生命周期快照和待生成队列使用版本级共享文件导致的跨存档污染风险。
- 存档 A 的恢复元数据不再被存档 B 载入内存。
- 每次服务器停止后重置生命周期持久化的提示和失败限流状态。

## 架构

- 新增 `RaidLifecycleSnapshotRepository`，作为存档持久化唯一网关。
- 新增版本化 `RaidLifecycleSavedData`，固定挂载到当前服务器主世界。
- 新增有界 `RaidLifecyclePropertiesCodec`，保留现有字段语义并限制最大负载。
- `RaidRuntimeContext` 开始拥有并关闭生命周期 Repository。
- `RaidExtraWaveController` 移除全局路径、临时文件和直接磁盘写入职责。

## 兼容策略

- 旧全局 sidecar 保留在磁盘但永远不自动导入，以免无法证明它属于哪个存档。
- 首次安装本版本前，应先完成仍依赖旧 sidecar 恢复的进行中袭击。
- 其余玩法、配置默认值和安全生成检查保持不变。
