# 0.9.2.1 构建验证

验证日期：2026-08-03  
Java：Zulu 21.0.8  
目标：Minecraft 1.21.1 / NeoForge 21.1.234

## 已通过

- `tools/verify_runtime_boundary.py`：PASS，95 个主源码文件。
- `RaidSpawnWorkQueueContractTest`：PASS。
- `ServerScopedContextStoreContractTest`：PASS。
- `RaidLifecyclePropertiesCodecContractTest`：PASS。
- 版本字符串、Mixin 源码/配置集合、单一 LevelTickEvent 入口、生成队列边界、安全生成检查和服务器生命周期接入静态验证通过。
- 新增断言确保控制器不再引用全局生命周期路径，Repository 必须挂载当前服务器 Overworld SavedData。

## 本地完整编译状态

已使用指定 Zulu 21 启动 Gradle Wrapper。Wrapper 下载 Gradle 8.12 时发生网络读取超时，因此未进入 `compileJava`，没有产生本地 JAR，也没有得到 Java 编译错误。

## GitHub 验收门槛

仓库工作流会依次执行三组 JDK 契约、静态架构验证和 `./gradlew clean build --stacktrace --no-daemon`。只有全部成功才上传 JAR。
