# 本轮开发交付报告

## 1. 项目信息

- 项目名称：Raid Enhancement Patch
- modid：`raid_enhancement_patch`
- 修改前版本：`0.9.1.7-reflection-cache-hotfix-alpha`
- 修改后版本：`0.9.1.8-safe-spawn-validation-alpha`
- 开发源码基线：0.9.1.7 完整源码包
- 回退基线：0.9.1.7 性能热修版本；紧急旧锚点仍为 0.9.1.0

## 2. 本轮目标

修正补丁自行生成的袭击者可能进入墙体、地面、建筑、液体或其他危险空间的问题，并在困难、高不祥、多刷新点和复杂地形下限制安全位置搜索成本，避免安全检查自身制造服务器主线程尖峰。

## 3. 实际完成内容

1. 新增独立的 `SafeRaidSpawnResolver` 安全刷新边界。
2. 每个实体在加入世界前使用自身真实碰撞箱验证最终坐标。
3. 验证已加载区块、世界边界、建筑高度、碰撞空间、液体、常见危险方块和脚下支撑。
4. 原坐标不安全时，在有限半径内按确定性顺序搜索邻近位置，并结合地表高度与有限垂直探测。
5. 不加载新区块、不清除地形、不破坏建筑来强行制造刷新空间。
6. 加入每实体 48 次与每世界每 Tick 2048 次的双重硬预算。
7. 普通附加波次、原生波次侧翼增援及 Raids Enhanced 地面特殊袭击者统一经过地面安全验证。
8. Raids Enhanced 飞艇经过独立空中碰撞空间验证。
9. 安全验证启用时，禁止命令召唤在原始不安全坐标绕过验证。
10. 原版 Raid `findRandomSpawnPos` 与关键生成反射调用改用 `CachedReflection` 缓存。
11. 飞艇仅在成功加入世界后才注册为 Raid 波次成员，避免幽灵成员。
12. 无安全位置或预算耗尽只输出一次低频诊断，不逐实体刷屏。

## 4. 未完成内容

- 尚未进行真实 Minecraft 游戏内测试。
- 尚未进行用户 Spark 对照测试。
- 尚未执行联网环境下的 Gradle `clean build`。
- 没有新增跨 Tick 待生成队列；极端情况下安全预算耗尽的可选增援可能被跳过，核心自定义波次在完全未生成时仍使用既有重试机制。
- 没有改变原版或 Raids Enhanced 自身控制的原生生成逻辑，只处理本 Mod 主动添加的实体。

## 5. 修改文件

| 文件 | 路径 | 修改内容 |
|---|---|---|
| `RaidExtraWaveController.java` | `src/main/java/com/noah/raidenhancement/raid/` | 将所有补丁自有生成路径接入安全解析器；缓存生成反射；防止命令绕过；修正飞艇注册顺序 |
| `RaidEnhancementConfig.java` | `src/main/java/com/noah/raidenhancement/config/` | 新增安全搜索半径、垂直探测及双重预算常量 |
| `RaidEnhancementPatch.java` | `src/main/java/com/noah/raidenhancement/` | 更新版本号与启动说明 |
| `gradle.properties` | 项目根目录 | 更新版本号 |
| `neoforge.mods.toml` | `src/main/resources/META-INF/` | 更新版本号 |

## 6. 新增文件

| 文件 | 路径 | 用途 |
|---|---|---|
| `SafeRaidSpawnResolver.java` | `src/main/java/com/noah/raidenhancement/raid/` | 集中处理实体碰撞箱安全验证、有界搜索和 Tick 预算 |
| `CHANGELOG_0.9.1.8.md` | 项目根目录 | 版本变更说明 |
| `BUILD_VERIFICATION_0.9.1.8.md` | 项目根目录 | 构建边界与静态验证记录 |

## 7. 删除文件

无

## 8. 配置变化

新增代码常量：

```text
EXTRA_WAVE_SAFE_SPAWN_VALIDATION_ENABLED = true
EXTRA_WAVE_SAFE_SPAWN_SEARCH_RADIUS = 12
EXTRA_WAVE_SAFE_SPAWN_VERTICAL_PROBE = 2
EXTRA_WAVE_SAFE_SPAWN_MAX_CHECKS_PER_ENTITY = 48
EXTRA_WAVE_SAFE_SPAWN_MAX_CHECKS_PER_TICK = 2048
EXTRA_WAVE_SAFE_AIR_CLEARANCE = 20
```

这些仍为源码常量，尚未改为用户配置文件。没有修改旧配置文件格式。

## 9. 数据与持久化变化

无。安全刷新预算和缓存均为运行期内存状态，不写入存档，不改变既有持久化键。

## 10. 网络同步变化

无。

## 11. Mixin 或 Access Transformer 变化

无。Mixin 配置与 0.9.1.7 字节一致。

## 12. 构建结果

构建成功

补充：当前离线环境无法下载 Gradle 8.12 与 NeoForge 依赖，因此本 JAR 是 Java 21 增量构建产物，以已验证的 0.9.1.7 JAR 为二进制基线，只替换本项目修改类和版本元数据。未打包任何 Minecraft/NeoForge 编译桩。

## 13. 输出文件

- JAR：`raid_enhancement_patch-0.9.1.8-safe-spawn-validation-alpha.jar`
- 源码包：`raid_enhancement_patch-0.9.1.8-safe-spawn-validation-alpha-github-ready-source.zip`
- 测试清单：`TEST_CHECKLIST_0.9.1.8.txt`
- 构建审计：`BUILD_AUDIT_0.9.1.8_FULL_CHECK.txt`
- SHA-256：`SHA256SUMS_0.9.1.8.txt`

## 14. 游戏内测试状态

未测试

## 15. 本轮可能影响的旧功能

- 补丁自行添加的普通袭击者、侧翼增援和特殊袭击者的实际最终坐标。
- 极端无安全空间时，部分可选增援可能不生成。
- 自定义波次完全未产生实体时仍会按既有失败重试逻辑重试。
- 飞艇加入 Raid 成员列表的时机调整为成功加入世界之后。

## 16. 禁止复发问题回归清单

- BossBar 不下降、不会回充或闪回。
- 原版 victory BossBar 再出现。
- 困难加不祥一级波次数量错误。
- 第八波或最后一波越界崩溃。
- 自定义波次重复生成。
- 奖励、VillageFavor 或 settlement 重复。
- 跨维度 cleanup 误杀。
- 0.9.1.7 的反射性能热点复发。
- 魔像破坏—恢复循环复发。
- dedicated server 加载客户端类。

## 17. 当前已知风险

1. 真实 Mod 实体的碰撞箱和特殊 AI 仍需游戏内验证。
2. 非常封闭的地下环境可能找不到足够安全位置，导致可选增援减少。
3. 安全解析器在实体生成瞬间执行，仍需 Spark 验证峰值是否可接受。
4. 当前没有把安全参数外置到配置文件，后续配置治理时应统一处理。
5. Gradle clean build 仍需用户本地或 GitHub Actions 完成。

## 18. 用户测试步骤

以 `TEST_CHECKLIST_0.9.1.8.txt` 为准。优先测试：

1. 开阔平原村庄。
2. 房屋密集村庄。
3. 山坡、悬崖和树木密集区。
4. 困难加高不祥、多刷新点。
5. 最终手段魔像、劫掠钻机、飞艇等大型特殊实体。
6. 60 秒 Spark：`/spark profiler start --timeout 60`。

## 19. 是否建议继续开发

先进行用户实测。安全刷新与 Spark 结果通过后，再继续下一项技术债。

## 20. 是否建议作为稳定基线

不建议作为稳定基线
