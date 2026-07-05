# BUILD_VERIFICATION_0.9.0.4

版本：`0.9.0.4-bossbar-display-layer-hotfix-alpha`

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
10. mixin 配置是否存在：是，src/main/resources/raid_enhancement_patch.mixins.json
11. .github/workflows/build-mod.yml 是否存在：是
12. 本地是否执行过 ./gradlew build：尝试过，但当前沙盒无法解析 services.gradle.org，Gradle Wrapper 无法下载 Gradle 8.12 分发包
13. build 是否通过：当前沙盒未能执行 Gradle clean build，不得标记为本地 clean build 通过
14. 生成的 jar 名称：raid_enhancement_patch-0.9.0.4-bossbar-display-layer-hotfix-alpha.jar
15. 是否已进游戏实测：否，尚需用户实机测试
16. 当前已知问题：0.9.0.4 为 BossBar 显示层热修 alpha，需验证第二波视觉回充与后续下降是否恢复
```

## 构建说明

当前沙盒执行：

```text
./gradlew --version
```

失败阶段：

```text
Gradle Wrapper 下载 Gradle 8.12 分发包阶段
```

报错摘要：

```text
java.net.UnknownHostException: services.gradle.org
```

判断：

```text
这是当前沙盒网络解析限制，不是项目源码编译错误。
GitHub Actions 在正常联网环境下应能通过 Gradle Wrapper 下载 Gradle 并执行 ./gradlew clean build。
```

## 当前 JAR 生成方式说明

由于当前沙盒无法联网下载 Gradle，本次提供的 JAR 不是通过完整 Gradle clean build 生成。

本次 JAR 采用以下可审计方式生成：

```text
1. 以已实测通过候选的 0.9.0.3 JAR 为基线；
2. 使用 javac --release 21，以 0.9.0.3 JAR 作为 classpath，单独编译本版唯一修改的 RaidIndependentBossbarManager.java；
3. 替换 JAR 内 RaidIndependentBossbarManager 及其内部类；
4. 更新 RaidEnhancementPatch.class 中版本字符串；
5. 更新 META-INF/MANIFEST.MF；
6. 更新 META-INF/neoforge.mods.toml；
7. 重新打包生成 0.9.0.4 JAR。
```

这意味着：

```text
JAR 已包含 0.9.0.4 版本号与 BossBar 显示层修改；
但它不是 Gradle clean build 产物；
如需正式归档稳定构建，仍建议将 github-ready-source.zip 上传 GitHub Actions 执行 clean build。
```

## JAR 静态核验

已静态确认：

```text
META-INF/MANIFEST.MF Implementation-Version = 0.9.0.4-bossbar-display-layer-hotfix-alpha
META-INF/neoforge.mods.toml version = 0.9.0.4-bossbar-display-layer-hotfix-alpha
RaidEnhancementPatch.VERSION = 0.9.0.4-bossbar-display-layer-hotfix-alpha
RaidIndependentBossbarManager.class major version = 65，Java 21
RaidIndependentBossbarManager 包含 forceClientReattach
```

## 0.9.0.4 修改边界核验

确认只修改：

```text
src/main/java/com/noah/raidenhancement/raid/RaidIndependentBossbarManager.java
src/main/java/com/noah/raidenhancement/RaidEnhancementPatch.java 的版本与启动说明
src/main/resources/META-INF/neoforge.mods.toml 的版本与说明
版本文档
```

未修改：

```text
VictorySettlementController.java
VillageFavorSystem.java
VillageFavorEvents.java
RaidWaveAuthority.java
RaidWaveExpansionController.java
RaidExtraWaveController.java
RaidVictorySuppressMixin.java
RaidWaveIndexClampMixin.java
RaidsEnhancedFinalWaveGuardMixin.java
奖励 JSON / 礼物池 / 持久化文件结构
```
