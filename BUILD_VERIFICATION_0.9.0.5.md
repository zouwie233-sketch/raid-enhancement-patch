# BUILD_VERIFICATION_0.9.0.5

版本：`0.9.0.5-bossbar-visible-authority-audit-alpha`

## 源码包结构自检

| 项目 | 状态 |
|---|---|
| zip 根目录是否就是 Gradle 工程根目录 | 是 |
| build.gradle 是否存在 | 是 |
| settings.gradle 是否存在 | 是 |
| gradle.properties 是否存在 | 是 |
| gradlew / gradlew.bat 是否存在 | 是 |
| gradle-wrapper.jar 是否存在 | 是 |
| src/main/java 是否存在 | 是 |
| src/main/resources 是否存在 | 是 |
| neoforge.mods.toml 是否存在 | 是 |
| mixin 配置是否存在 | 是 |
| .github/workflows/build-mod.yml 是否存在 | 是 |

## 修改范围审计

相对 0.9.0.4，只修改以下核心文件：

```text
gradle.properties
src/main/resources/META-INF/neoforge.mods.toml
src/main/java/com/noah/raidenhancement/RaidEnhancementPatch.java
src/main/java/com/noah/raidenhancement/config/KeyDiagnosticsConfig.java
src/main/java/com/noah/raidenhancement/raid/RaidIndependentBossbarManager.java
README_INSTALL_0.9.0.5.md
CHANGELOG_0.9.0.5.md
BUILD_VERIFICATION_0.9.0.5.md
```

未修改 settlementKey / RaidInstanceKey / VillageFavor / 奖励 / 波次 / RaidWaveAuthority / 第八波防崩 Mixin。

## 构建说明

当前沙盒无法解析：

```text
services.gradle.org
```

因此未能完成：

```text
./gradlew clean build
```

失败阶段：Gradle Wrapper 下载 Gradle 8.12 分发包阶段。

错误摘要：

```text
java.net.UnknownHostException: services.gradle.org
```

## 临时 JAR 生成方式

为了提供可测试 JAR，本次采用受控临时构建方式：

```text
1. 以用户提供的 0.9.0.4 源码构建 JAR 为基线；
2. 使用 javac --release 21 编译 0.9.0.5 修改过的类；
3. 仅替换 JAR 中对应 class；
4. 更新 neoforge.mods.toml 与 MANIFEST.MF；
5. 重新打包为 0.9.0.5 测试 JAR。
```

编译通过的类：

```text
com.noah.raidenhancement.RaidEnhancementPatch
com.noah.raidenhancement.config.KeyDiagnosticsConfig
com.noah.raidenhancement.raid.RaidIndependentBossbarManager
```

未把编译用 NeoForge stub 放入 JAR。

## 生成文件

```text
raid_enhancement_patch-0.9.0.5-bossbar-visible-authority-audit-alpha.jar
raid_enhancement_patch-0.9.0.5-bossbar-visible-authority-audit-alpha-github-ready-source.zip
```

## 是否已进游戏实测

否。

本版需要用户实机测试，重点确认：

```text
玩家视觉 BossBar 是否带 [REP]；
BossBarAuthorityAudit 是否记录 independent / vanilla BossBar 权威状态；
0.9.0.3 settlementKey 回归是否仍通过。
```

## 当前已知问题

```text
0.9.0.4 第二波视觉回充失败；
0.9.0.5 不承诺修复回充，只做 visible authority audit；
临时 JAR 不是 Gradle clean build 产物，GitHub Actions 构建结果应作为正式归档候选。
```
