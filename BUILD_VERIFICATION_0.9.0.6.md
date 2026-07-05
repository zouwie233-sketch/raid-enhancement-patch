# BUILD VERIFICATION 0.9.0.6

版本：`0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha`

## 源码包结构自检

```text
1. zip 根目录是否就是 Gradle 工程根目录：是
2. build.gradle 是否存在：是
3. settings.gradle 是否存在：是
4. gradle.properties 是否存在：是
5. gradlew / gradlew.bat 是否存在：是
6. gradle-wrapper.jar 是否存在：是
7. src/main/java 是否存在：是
8. src/main/resources 是否存在：是
9. neoforge.mods.toml 是否存在：是
10. mixin 配置是否存在：是，raid_enhancement_patch.mixins.json
11. .github/workflows/build-mod.yml 是否存在：是
```

## 本地构建状态

当前沙盒尝试执行：

```text
./gradlew --version
```

失败阶段：Gradle Wrapper 下载 Gradle 发行版阶段。

错误摘要：

```text
java.net.UnknownHostException: services.gradle.org
```

因此：

```text
本包未完成本地 ./gradlew clean build。
```

## 测试用 JAR 生成方式

由于当前沙盒无法访问 Gradle 服务，测试用 JAR 使用以下方式生成：

```text
1. 以 0.9.0.5 测试 JAR 为基线；
2. 使用 javac 编译 0.9.0.6 修改过的类；
3. 替换 JAR 内对应 class；
4. 更新 META-INF/neoforge.mods.toml；
5. 更新 META-INF/MANIFEST.MF；
6. 重新打包为 0.9.0.6 测试 JAR。
```

编译替换的类：

```text
com/noah/raidenhancement/raid/RaidIndependentBossbarManager.class
com/noah/raidenhancement/config/KeyDiagnosticsConfig.class
com/noah/raidenhancement/RaidEnhancementPatch.class
```

## 生成文件

```text
raid_enhancement_patch-0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha.jar
raid_enhancement_patch-0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha-github-ready-source.zip
```

## 静态核验

```text
modid：raid_enhancement_patch
版本号：0.9.0.6-bossbar-end-cleanup-and-refill-polish-alpha
Minecraft：1.21.1
NeoForge：21.1.234+
Java：21
ServerBossEventRaidTitleMixin：类存在，但 mixins.json 未启用
BossBarCleanupAudit：已打入 RaidIndependentBossbarManager.class
same-wave-refill-suppressed：已打入 RaidIndependentBossbarManager.class
```

## 是否已进游戏实测

```text
否。当前版本尚未游戏内实测。
```

## 当前已知问题

```text
1. 这是 alpha 测试版，不是稳定版；
2. 本地 Gradle clean build 未完成，建议使用 GitHub Actions 正式构建；
3. [REP] 标题标记仍为诊断用途，后续稳定版应考虑移除或配置关闭；
4. 需要实测确认袭击完成后是否不再出现无 [REP] 原版条残留。
```
