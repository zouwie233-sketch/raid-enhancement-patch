# 本轮开发交付报告

## 1. 项目信息

- 项目名称：Raid Enhancement Patch
- modid：`raid_enhancement_patch`
- 修改前版本：`0.9.1.6-config-audit-alpha`
- 修改后版本：`0.9.1.7-reflection-cache-hotfix-alpha`
- 开发源码基线：完整 0.9.1.6 源码
- 行为对照基线：0.9.1.5
- 回退基线：0.9.1.0

## 2. 本轮目标

消除魔像方块回滚与村民保护兼容代码在服务器 Tick 热路径中的重复反射成员搜索，限制回滚任务的单 Tick 工作量，并阻止方块恢复重新将魔像封入地形。

## 3. 实际完成内容

- 新增共享正/负反射缓存。
- Golem 回滚任务按 ServerLevel 隔离。
- 同一魔像 UUID 的快照去重并刷新窗口。
- 方块检查加入全局预算和轮转公平性。
- 掉落清理区按方块位置合并并设置处理预算。
- 魔像碰撞期间延迟恢复；超时后放弃会重新困住魔像的恢复。
- MobEffect 方法、字段和构造器查找全部缓存。
- 村民保护效果刷新节流。
- 同一游戏 Tick 的生命锁定维护幂等化。
- 两个 LevelTick 事件入口改用共享反射缓存。

## 4. 未完成内容

- 附加袭击者安全生成位置验证未修改。
- 未进行 Minecraft 游戏内测试。
- 未进行 Spark 修复后对照测试。
- 当前环境无法执行 Gradle clean build。

## 5. 修改文件

| 文件 | 路径 | 修改内容 |
|---|---|---|
| RaidEnhancementPatch.java | `src/main/java/com/noah/raidenhancement/` | 版本与热修启动标记 |
| MobEffectCompat.java | `src/main/java/com/noah/raidenhancement/compat/` | 使用共享正/负缓存，增加阈值刷新 |
| BattleSupportEvents.java | `src/main/java/com/noah/raidenhancement/event/` | Tick 事件访问器改用缓存 |
| VillagerProtectionEvents.java | `src/main/java/com/noah/raidenhancement/event/` | Tick 事件访问器改用缓存 |
| GolemBlockRollbackGuard.java | `src/main/java/com/noah/raidenhancement/raid/` | 按维度/魔像队列、预算、碰撞延迟、去重 |
| ProtectedVillagerState.java | `src/main/java/com/noah/raidenhancement/villager/` | 记录同 Tick 健康锁维护时间 |
| VillagerProtectionController.java | `src/main/java/com/noah/raidenhancement/villager/` | 效果刷新节流与健康锁幂等化 |
| gradle.properties | 项目根目录 | 版本更新 |
| neoforge.mods.toml | `src/main/resources/META-INF/` | 版本与说明更新 |
| CURRENT_PROJECT_BUILD_INFO.md | 项目根目录 | 阶段切换为 P0 稳定化 |

## 6. 新增文件

| 文件 | 路径 | 用途 |
|---|---|---|
| CachedReflection.java | `src/main/java/com/noah/raidenhancement/compat/` | 可复用兼容反射缓存边界 |
| CHANGELOG_0.9.1.7.md | 根目录 | 修改记录 |
| KNOWN_ISSUES_0.9.1.7.md | 根目录 | 已知问题 |
| HOTFIX_ARCHITECTURE_0.9.1.7.md | 根目录 | 热修模块边界 |
| TEST_CHECKLIST_0.9.1.7.txt | 根目录 | 用户与 Spark 测试 |
| README_INSTALL_0.9.1.7.md | 根目录 | 安装和重建说明 |
| BUILD_VERIFICATION_0.9.1.7.md | 根目录 | 构建方法和限制 |

## 7. 删除文件

无。

## 8. 配置变化

无配置键、默认值或配置文件格式变化。性能预算为热修内部固定上限，后续可在独立配置治理版本中审查是否外置。

## 9. 数据与持久化变化

无存档格式变化。Golem 回滚任务仍为运行时内存数据，但从全局列表调整为按 ServerLevel 隔离的任务队列。

## 10. 网络同步变化

无。

## 11. Mixin 或 Access Transformer 变化

无。启用的 Mixin JSON 与 0.9.1.6 完全一致。

## 12. 构建结果

构建成功。

说明：Java 21 变更类编译和完整 JAR 打包成功；由于当前环境不能下载 Gradle/NeoForge 依赖，未完成 Gradle `clean build`。JAR 为基于 0.9.1.6 完整二进制基线的增量热修 alpha 工件，不标记为稳定版。

## 13. 输出文件

- JAR：`raid_enhancement_patch-0.9.1.7-reflection-cache-hotfix-alpha.jar`
- 源码包：`raid_enhancement_patch-0.9.1.7-reflection-cache-hotfix-alpha-github-ready-source.zip`
- 其他材料：SHA-256、测试清单、构建核验和架构说明

## 14. 游戏内测试状态

未测试。

## 15. 本轮可能影响的旧功能

- 最终手段魔像方块保护和掉落清理。
- 村民保护效果刷新与健康锁维护。
- BattleSupport/VillagerProtection LevelTick 事件访问。

## 16. 禁止复发问题回归清单

详见 `TEST_CHECKLIST_0.9.1.7.txt`。BossBar、波次、victory 条、跨维度清理、单次结算、奖励和 VillageFavor 都必须回归。

## 17. 当前已知风险

- 需要真实 Raids Enhanced 魔像进行碰撞与恢复测试。
- 需要 Spark 证明热点消失。
- 附加袭击生成在方块中的根因仍未修复。
- 增量打包工件仍需用户环境和后续 Gradle 环境双重验证。

## 18. 用户测试步骤

按 `TEST_CHECKLIST_0.9.1.7.txt` 执行，重点复现原 Spark 场景并运行 `/spark profiler start --timeout 60`。

## 19. 是否建议继续开发

在功能测试和 Spark 复测完成前，不建议进入其他维护或新功能。性能通过后再进入安全生成位置专项。

## 20. 是否建议作为稳定基线

不建议作为稳定基线。
