# 安装说明：0.8.9.9.0-raid-encounter-authority-refactor-alpha

## 安装

1. 删除旧版：
   `raid_enhancement_patch-0.8.9.8.8-bossbar-title-mixin-hotfix.jar`
2. 放入新版：
   `raid_enhancement_patch-0.8.9.9.0-raid-encounter-authority-refactor-alpha.jar`
3. 建议测试前删除旧运行期状态：
   `config/raid_enhancement_patch/raid_session_lifecycle.properties`

## 本版目标

这是第一版袭击遭遇权威层重构，不新增玩法，重点是把波次、UI、桥接、未来胜利门控收束到同一个运行期快照读模型。

## 优先测试

- 普通 + 不祥 5：8 波结束，不进入额外波。
- 困难 + 不祥 2：8 波结束，不进入额外波。
- 困难 + 不祥 3：8 + 第 9 波。
- 困难 + 不祥 5：8 + 第 9/10/11 波。
- 顶部 BossBar 标题是否稳定显示：`袭击 第 X / Y 波`。
