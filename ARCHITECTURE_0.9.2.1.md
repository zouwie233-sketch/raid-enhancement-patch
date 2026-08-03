# 0.9.2.1 跨存档生命周期持久化隔离

## 问题证据

0.9.2.0 的 A → B → A 实机测试证明 `RaidRuntimeContext` 可以正常启动和关闭，但存档 B 仍读取了存档 A 写入版本目录 `config` 的袭击生命周期快照。旧键只包含维度与袭击中心，无法声明快照所属存档。

## 新边界

```text
MinecraftServer
  -> RaidRuntimeContext
       -> RaidDiagnosticsContext
       -> RaidLifecycleSnapshotRepository
            -> Overworld DimensionDataStorage
                 -> RaidLifecycleSavedData
```

- `RaidLifecycleSnapshotRepository` 是控制器访问持久化的唯一网关。
- Repository 延迟挂载到当前服务器主世界的 `DimensionDataStorage`。
- 每个存档独立生成 `data/raid_enhancement_patch_raid_session_lifecycle.dat`。
- `RaidExtraWaveController` 保留当前快照映射和业务校验，但不再解析路径、创建目录或直接写磁盘文件。
- `RaidRuntimeContext.close()` 关闭 Repository，并释放其 `MinecraftServer` 强引用。

## 数据安全

- SavedData 外层格式版本为 `1`，未来版本会被安全拒绝。
- 现有 Properties 负载继续复用，避免改变已验证的队列字段语义。
- 二进制负载上限为 4 MiB。
- 旧全局 `raid_session_lifecycle.properties` 不自动导入、不删除，也不参与运行时恢复。

## 行为不变量

本版本不修改袭击波数、生成组成、安全位置检查、实体注册、BossBar、结算、奖励、VillageFavor 或 Mixin 启用集合。
