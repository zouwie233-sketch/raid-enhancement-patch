# 安装说明：0.8.9.9.1-raid-crash-mixin-cache-hotfix

## 安装

1. 完全退出 Minecraft。
2. 删除旧版：
   - `raid_enhancement_patch-0.8.9.9.0-raid-encounter-authority-refactor-alpha.jar`
3. 放入新版：
   - `raid_enhancement_patch-0.8.9.9.1-raid-crash-mixin-cache-hotfix.jar`
4. 建议删除旧运行期袭击状态文件：
   - `config/raid_enhancement_patch/raid_session_lifecycle.properties`

不要删除：

- `victory_settlement_history.properties`
- `village_favor.properties`
- `battle_support_items.properties`
- `settlement_rewards/`

## 测试重点

优先复测困难 + 不祥 2：

- 第 8 波开始不应再崩溃。
- 顶部袭击 BossBar 仍应显示：`袭击 第 X / 8 波`。
- 不应进入第 9～11 波。

如果仍有 BossBar 标题闪回，请上传 `latest.log` / `debug.log`，下一步改成独立 BossBar 叠加方案。
