# 安装说明：0.8.9.8.6-bossbar-wave-ui-hotfix

1. 删除 mods 文件夹内旧版本 `raid_enhancement_patch-*.jar`。
2. 只保留：`raid_enhancement_patch-0.8.9.8.6-bossbar-wave-ui-hotfix.jar`。
3. 不要多个版本共存。
4. 建议删除旧运行期文件：`config/raid_enhancement_patch/raid_session_lifecycle.properties`，避免旧波次快照影响测试。

本版重点：
- 移除 0.8.9.8.5 的下方 Actionbar 兜底波次显示。
- 默认关闭波次清剿时限的聊天刷屏提示。
- 将波次 UI 写入原版顶部袭击 BossBar 标题：`袭击 第 X / Y 波`。
- 保留 0.8.9.8.5 的 RaidWaveAuthority 波次权威层。
