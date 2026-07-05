# 0.8.9.7.2-component-factory-hotfix 安装说明

修复 0.8.9.7.1 仍然可能在启动阶段调用错误的 `Component.translatable(String)` / `Component.literal(String)` 字节码签名导致崩溃的问题。

安装：
1. 从 mods 文件夹移除 0.8.9.7 与 0.8.9.7.1 旧 JAR。
2. 只放入 `raid_enhancement_patch-0.8.9.7.2-component-factory-hotfix.jar`。
3. 保留原有 Raids Enhanced / fdlib 等依赖。

本版本只修复新物品系统的 Component 工厂兼容，不改 HUD、第 8 波桥接、第 9～11 波额外波、离场回归、原版 Raid 胜负状态、清怪或传送袭击者逻辑。
