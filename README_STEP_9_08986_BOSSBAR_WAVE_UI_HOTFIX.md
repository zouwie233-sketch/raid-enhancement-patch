# 第九步热修：0.8.9.8.6 BossBar 波次 UI 修复

## 问题
用户反馈 0.8.9.8.5 的自定义 HUD 仍未显示，并且不希望使用下方 Actionbar/聊天方式显示波次。

## 修复
1. 移除 `sendRaidWaveHudActionBar` 的下方 Actionbar 输出。
2. 默认关闭 `WAVE_TIME_DISPLAY_WAVE_START_CHAT_ENABLED` 与 `WAVE_TIME_DISPLAY_WARNING_CHAT_ENABLED`，避免底部聊天刷屏。
3. 在服务端 `publishHudSnapshot` 后，通过反射找到原版 Raid 的 `ServerBossEvent`，把顶部原版袭击 BossBar 标题改为：
   - `袭击 第 X / Y 波`
   - 额外波时追加 `｜额外波`
   - 有清剿计时时追加 `｜清剿 mm:ss`
4. 该 UI 不依赖客户端自定义渲染事件，也不使用下方 Actionbar，因此更适合与整合包 HUD/小地图/光影共存。

## 未改动
- 未改动 RaidWaveAuthority 表。
- 未改动 9～11 额外波刷怪结构。
- 未改动胜利结算、奖励、扫荡、恩情。
- 未改动 Raids Enhanced 魔像回滚与掉落物清理。
