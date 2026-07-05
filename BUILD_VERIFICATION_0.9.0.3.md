# BUILD_VERIFICATION 0.9.0.3

版本：`0.9.0.3-settlement-raidinstance-key-alpha`

## 源码包定位

本源码包是 GitHub-ready Gradle 工程包。最终 zip 已按“仓库根目录即工程根目录”方式打包：打开 zip 后第一层应直接看到 `build.gradle`、`settings.gradle`、`gradlew`、`src/`、`.github/` 等文件/目录，不应再多包一层版本目录。

## 必要文件自检

| 项目 | 状态 | 路径 |
|---|---:|---|
| build.gradle | 是 | build.gradle |
| settings.gradle | 是 | settings.gradle |
| gradle.properties | 是 | gradle.properties |
| gradlew | 是 | gradlew |
| gradlew.bat | 是 | gradlew.bat |
| gradle-wrapper.jar | 是 | gradle/wrapper/gradle-wrapper.jar |
| gradle-wrapper.properties | 是 | gradle/wrapper/gradle-wrapper.properties |
| src/main/java | 是 | src/main/java |
| src/main/resources | 是 | src/main/resources |
| neoforge.mods.toml | 是 | src/main/resources/META-INF/neoforge.mods.toml |
| mixin config | 是 | src/main/resources/raid_enhancement_patch.mixins.json |
| .github/workflows/build-mod.yml | 是 | .github/workflows/build-mod.yml |
| README_INSTALL_0.9.0.3.md | 是 | README_INSTALL_0.9.0.3.md |
| CHANGELOG_0.9.0.3.md | 是 | CHANGELOG_0.9.0.3.md |
| BUILD_VERIFICATION_0.9.0.3.md | 是 | BUILD_VERIFICATION_0.9.0.3.md |

## Gradle 配置检查

`settings.gradle` 仓库策略：`RepositoriesMode.PREFER_PROJECT`。

本归档包已避免使用 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`。原因：`net.neoforged.moddev` 插件会在项目级添加 NeoForge 相关 Maven 仓库；若使用 `FAIL_ON_PROJECT_REPOS`，构建会在插件添加仓库阶段失败。

## 本地构建状态

当前 ChatGPT 沙盒内 **未完成** 本地 `./gradlew clean build`。

原因：当前沙盒没有 Gradle 8.12 Wrapper 发行版缓存，且无法解析 `services.gradle.org`，因此 Gradle Wrapper 无法下载 Gradle 发行版。

本地 Wrapper 验证尝试摘要：

```text
Downloading Gradle distribution from https://services.gradle.org/distributions/gradle-8.12-bin.zip
Exception in thread "main" java.net.UnknownHostException: services.gradle.org
	at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:567)
	at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:327)
```

因此本包不能声称“本地 clean build 通过”。

## GitHub Actions 构建预期

本包包含：

```text
.github/workflows/build-mod.yml
```

workflow 目标：

```text
push 到 main/master 或 workflow_dispatch 后，使用 Java 21 和 Gradle Wrapper 执行 ./gradlew clean build --stacktrace --no-daemon，并上传 build/libs/*.jar。
```

## 已确认 JAR 信息

用户已通过构建流程得到并上传了下列 JAR，且已完成静态审查与游戏内测试：

```text
raid_enhancement_patch-0.9.0.3-settlement-raidinstance-key-alpha.jar
```

静态审查信息：

```text
SHA-256: 054ca1f5514d950133310bce86844203e29c93e18b3e74709807db7eb7f3d651
Implementation-Version: 0.9.0.3-settlement-raidinstance-key-alpha
modId: raid_enhancement_patch
version: 0.9.0.3-settlement-raidinstance-key-alpha
```

## 游戏内实测状态

已进游戏实测。测试环境：

```text
Minecraft 1.21.1
NeoForge 21.1.234
Java 21
安装 Raids Enhanced 与 fdlib
```

实测结论：

```text
0.9.0.3 settlementKey 核心修复通过候选。
```

日志确认出现：

```text
settlementKeyMode=raidInstance
@raidInstance settlementKey
legacy-history-present-ignored
accepted-before-rewards
favor-record
```

0.9.0.3 阶段未出现：

```text
duplicate-blocked-by-history
@raidInstance:fallback
重复发奖励
崩溃
```

## 生成的 jar 名称

本沙盒未本地生成新 JAR。用户已上传并实测的 JAR 名称为：

```text
raid_enhancement_patch-0.9.0.3-settlement-raidinstance-key-alpha.jar
```

## 当前已知问题

```text
BossBar 视觉回充失败：第一波会下降，第二波不视觉回充，后续不继续正常下降。
```

此问题留给 `0.9.0.4-bossbar-display-layer-hotfix-alpha`，不得混入 0.9.0.3。

## 版本边界

0.9.0.3 必须保留 settlementKey / RaidInstanceKey 修复逻辑；不得混入 0.9.0.4 BossBar 未测试改动。
