# 安装说明：raid_enhancement_patch-0.8.9.8.0-victory-settlement-sweep-favor-alpha

1. 删除 mods 文件夹内旧版本 `raid_enhancement_patch-*.jar`。
2. 只放入：`raid_enhancement_patch-0.8.9.8.0-victory-settlement-sweep-favor-alpha.jar`。
3. 不要多个版本共存。
4. 本版本会自动生成：
   - `config/raid_enhancement_patch/victory_settlement.properties`
   - `config/raid_enhancement_patch/settlement_rewards/perfect_victory.json`
   - `config/raid_enhancement_patch/settlement_rewards/victory.json`
   - `config/raid_enhancement_patch/settlement_rewards/costly_victory.json`
   - `config/raid_enhancement_patch/settlement_rewards/sweep_exchange.json`
   - `config/raid_enhancement_patch/settlement_rewards/experience_rewards.json`
   - `config/raid_enhancement_patch/settlement_rewards/favor_rewards.json`
5. 若已经有旧版 `raids_enhanced_compat.properties` 且最后防线魔像掉落物清理异常，可备份后删除，让新版重建。

## 测试重点

1. 能否启动进主菜单。
2. 能否进入世界。
3. 困难 + 不祥 1/2 的 8 波内袭击是否能正常胜利结算。
4. 困难 + 不祥 3/4/5 的 9～11 波是否不会在第 8 波提前结算。
5. 胜利后是否只结算一次。
6. 战报是否出现：完胜 / 胜利 / 惨胜 三档。
7. 胜利后经验、JSON 奖池物品是否发给附近/参战玩家。
8. 战场扫荡是否回收弩、铁斧、鞍等配置白名单物品。
9. 不死图腾默认是否保留，不被扫荡回收。
10. 再次进入有恩情的村庄附近是否显示恩情提示。
