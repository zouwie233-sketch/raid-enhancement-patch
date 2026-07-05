# 安装说明：0.8.9.9.4-module-stub-cleanup-hotfix

## 适用环境

- Minecraft 1.21.1
- NeoForge 21.1.234
- Java 21

## 安装方式

1. 删除旧版：
   - `raid_enhancement_patch-0.8.9.9.3-omen1-native-wave-bossbar-cache-hotfix.jar`
2. 放入新版：
   - `raid_enhancement_patch-0.8.9.9.4-module-stub-cleanup-hotfix.jar`
3. 建议删除旧运行期状态：
   - `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除：

- `victory_settlement_history.properties`
- `village_favor.properties`
- `battle_support_items.properties`
- `settlement_rewards` 文件夹

## 本版说明

本版修复启动阶段 Java 模块层崩溃。0.8.9.9.3 的 JAR 内误包含编译期 stub class，导致 `raid_enhancement_patch` 与 `minecraft` 同时导出 `net.minecraft.world.level` 等包。
