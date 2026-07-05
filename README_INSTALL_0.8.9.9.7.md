# Raid Enhancement Patch 0.8.9.9.7

## 安装

删除旧版 `raid_enhancement_patch-0.8.9.9.6-independent-raid-bossbar-performance-hotfix.jar`，只保留本版 JAR。

建议测试前删除：

```text
config/raid_enhancement_patch/raid_session_lifecycle.properties
```

不要删除奖励历史、村庄恩情、战备令牌配置和 settlement_rewards 文件夹。

## 本版目标

修复独立 BossBar 的进度条表现：上一版按 `当前波 / 总波数` 显示，导致每波结束后只补一格。本版改为：

- 活跃波次：尽量镜像隐藏的原版 Raid BossBar 进度。
- 清剿/波间阶段：显示满条。
- 保留 0.8.9.9.5/0.8.9.9.6 已验证的第八波防崩溃与独立 BossBar 不闪回逻辑。
