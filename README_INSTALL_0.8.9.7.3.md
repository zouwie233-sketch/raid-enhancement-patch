# Raid Enhancement Patch 0.8.9.7.3 Registry Key Hotfix

基于 0.8.9.7.2。

修复内容：

- 修复 `ModItems` 在用户 NeoForge 21.1.234 运行环境中因 `Registries.ITEM` / `Registries.CREATIVE_MODE_TAB` 字段描述符不匹配导致的 `NoSuchFieldError`。
- 保留 0.8.9.7.2 的 Component 工厂兼容修复。
- 未改动 HUD、第 8 波桥接、第 9～11 波状态机、离场 / 回归、Raids Enhanced 特殊袭击者、原版 Raid 胜负、清怪/传送怪逻辑。

安装：

只保留 `raid_enhancement_patch-0.8.9.7.3-registry-key-hotfix.jar`，不要与 0.8.9.7 / 0.8.9.7.1 / 0.8.9.7.2 同时放入 mods。

注意：

本次错误报告中还存在 Distant Horizons 3.1.2-b 的 SelfUpdater 初始化 NPE，这是独立问题，不属于本模组。
