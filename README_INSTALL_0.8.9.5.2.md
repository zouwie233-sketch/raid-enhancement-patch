# 安装说明 - 0.8.9.5.2

1. 删除旧版 `raid_enhancement_patch-0.8.9.5.1-world-load-hotfix.jar`。
2. 放入 `raid_enhancement_patch-0.8.9.5.2-return-reconcile-hotfix.jar`。
3. 配置文件可以保留。若已有旧配置文件，请手动补充以下配置项，或删除旧配置让模组重新生成：

```properties
performanceOptimization.securityGolemReturnGraceTicks=200
performanceOptimization.securityGolemMissingPruneGraceTicks=1200
strictRules.villagerPenaltyRadius=128
```

如果你的村庄规模很大，部分村民仍未受到防线崩溃/超时压力影响，可以把：

```properties
strictRules.villagerPenaltyRadius=160
```

或更高，但不建议超过 256，除非你确实在测试大型村庄。
