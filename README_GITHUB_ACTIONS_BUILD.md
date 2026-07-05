# GitHub Actions 构建说明

本包是 0.9.0.3-settlement-raidinstance-key-alpha 的 GitHub 重新建库可用源码包。

## 使用方法

1. 新建 GitHub 仓库。
2. 把本文件夹内的所有内容上传到仓库根目录。
3. 确认仓库根目录直接存在：
   - build.gradle
   - settings.gradle
   - gradle.properties
   - gradlew
   - gradlew.bat
   - gradle/wrapper/gradle-wrapper.jar
   - src/
   - .github/workflows/build-mod.yml
4. 进入 Actions → Build Minecraft Mod JAR → Run workflow。
5. 构建成功后下载 Artifacts：raid-enhancement-built-jar。

## 说明

- 当前 workflow 不使用 gradle/actions/setup-gradle，避免 Wrapper JAR 校验拦截。
- workflow 会在 Gradle 启动前清理 settings.gradle / build.gradle / gradle.properties 等文件的 UTF-8 BOM。
- 0.9.0.3 是 settlementKey 热修候选版，不是稳定版。
- 本版不修 BossBar 回充，只修 VictorySettlement 防重复结算 key，并保留 KeyDiag / BossBarDiag 输出。
