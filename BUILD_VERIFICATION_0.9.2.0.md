# 0.9.2.0 构建验证

验证日期：2026-08-01  
Java：Zulu 21.0.8  
NeoForge 目标：21.1.234 / Minecraft 1.21.1

## 已通过

- `ServerScopedContextStoreContractTest`：PASS。
- 原 `RaidSpawnWorkQueueContractTest`：PASS。
- `tools/verify_runtime_boundary.py`：PASS，92 个 Java 源文件。
- Gradle 8.12 Wrapper 能由指定 Zulu 21 正常启动。
- 版本字符串、Mixin 源码/配置集合、安全生成边界、生成队列边界和生命周期接入静态检查通过。

## 本地完整编译状态

`gradlew compileJava` 已尝试，但 NeoForge 官方 Maven 连接在下载
`net.neoforged:neoform-runtime:1.0.40` 时被重置，因此未进入 Java 编译任务。

这不是源码编译错误。GitHub Actions 已保留完整 `clean build`，并新增本版本的 Context 契约测试；GitHub 构建结果仍是交付验收门槛。

## 尚待完成

- GitHub Actions 完整构建。
- 同一 JVM 连续进入两个存档的隔离测试。
- 困难 + 不祥 V 最终连续波次回归。
- 最终波次退出主菜单并重进的队列恢复回归。
