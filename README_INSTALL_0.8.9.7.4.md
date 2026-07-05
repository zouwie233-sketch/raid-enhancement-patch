# 安装说明：0.8.9.7.4-creative-tab-hotfix

本版本基于 0.8.9.7.3，修复打开创造栏时的崩溃：

- 将村庄战备创造栏的 displayItems lambda 改为具名 DisplayItemsGenerator 实现。
- 显式实现 `CreativeModeTab.DisplayItemsGenerator.accept(CreativeModeTab.ItemDisplayParameters, CreativeModeTab.Output)`。
- 避免混合映射环境把第一个参数擦除为 Object 后，在 NeoForge 21.1.234 运行时触发 AbstractMethodError。

安装时只保留：

`raid_enhancement_patch-0.8.9.7.4-creative-tab-hotfix.jar`

请移除旧版：

- `raid_enhancement_patch-0.8.9.7-security-support-items.jar`
- `raid_enhancement_patch-0.8.9.7.1-component-compat-hotfix.jar`
- `raid_enhancement_patch-0.8.9.7.2-component-factory-hotfix.jar`
- `raid_enhancement_patch-0.8.9.7.3-registry-key-hotfix.jar`

本版本未改动 HUD、第 8 波桥接、第 9～11 波额外波状态机、离场/回归逻辑、Raids Enhanced 特殊袭击者逻辑、原版 Raid 胜负状态、清怪/传送怪逻辑和村民药水图标隐藏逻辑。
