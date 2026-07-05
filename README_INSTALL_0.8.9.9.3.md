# 0.8.9.9.4 安装说明

## 文件

`raid_enhancement_patch-0.8.9.9.4-module-stub-cleanup-hotfix.jar`

## 安装

删除旧版：

`raid_enhancement_patch-0.8.9.9.2-vanilla-bonus-wave-numgroups-hotfix.jar`

只放入新版：

`raid_enhancement_patch-0.8.9.9.4-module-stub-cleanup-hotfix.jar`

建议删除旧运行期状态：

`config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除胜利结算历史、村庄恩情、奖励 JSON、战备令牌配置。

## 重点测试

1. 困难 + 不祥 1：应为 8 波，不再只有 7 波。
2. 困难 + 不祥 2：应为 8 波，不崩溃，不进入 9～11。
3. BossBar 标题闪回频率应进一步降低。
