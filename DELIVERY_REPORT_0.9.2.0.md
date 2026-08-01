# 0.9.2.0 源码交付报告

## 交付性质

这是架构路线图 `ARCH-1.1` 的首批源码，不是玩法更新。

## 核心结果

- 建立每个具体 Minecraft 服务器独立的运行上下文。
- 建立显式 start、stopping、checkpoint、close 生命周期。
- 首次迁移只涉及诊断限流和 warn-once 状态。
- 核心袭击功能和持久化格式保持 0.9.1.12 行为。

## 验证结果

- 两项 JDK 契约测试通过。
- 静态运行边界验证通过。
- 本地 Gradle 完整编译受 NeoForge Maven 连接重置阻塞，需以 GitHub Actions 构建结果确认。

## 交付要求

上传 GitHub-ready 源码 ZIP，执行仓库自带 `.github/workflows/build-mod.yml`。成功后按 `TEST_CHECKLIST_0.9.2.0.txt` 进行实机回归。
