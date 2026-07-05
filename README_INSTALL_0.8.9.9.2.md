# 0.8.9.9.2 安装说明

删除旧版 0.8.9.9.1，只保留：

`raid_enhancement_patch-0.8.9.9.2-vanilla-bonus-wave-numgroups-hotfix.jar`

建议测试前删除：

`config/raid_enhancement_patch/raid_session_lifecycle.properties`

本版修复：原版 Raid.numGroups 与不祥奖励波叠加后导致第 8 波索引 8 越界崩溃。逻辑上仍按 8 波原版段处理，但写入原版 numGroups 字段时只写常规波数量，预留原版不祥奖励波。
