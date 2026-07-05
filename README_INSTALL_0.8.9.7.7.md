# Raid Enhancement Patch 0.8.9.7.7 - Mercenary Player Color System

基线：0.8.9.7.6-mercenary-performance-persistence。

本版本只优化雇佣兵铁傀儡的玩家颜色识别系统，不改 HUD、第 8 波桥接、第 9～11 波额外波、离场 / 回归、Raids Enhanced 特殊袭击者、原版 Raid 胜负、清怪或传送袭击者逻辑。

## 新增内容

- 雇佣兵铁傀儡支持按召唤玩家分配不同发光颜色。
- 使用原版 scoreboard team 颜色实现发光轮廓颜色。
- 默认采用高可见度颜色池：GOLD、YELLOW、GREEN、AQUA、BLUE、LIGHT_PURPLE、RED、DARK_PURPLE、DARK_RED、DARK_AQUA、WHITE、GRAY。
- 默认独占颜色：不同玩家不会共用同一种颜色。
- 颜色耗尽时默认拒绝新玩家签发雇佣兵契约，避免颜色混淆。
- 玩家颜色分配持久化保存到：

```text
config/raid_enhancement_patch/mercenary_player_colors.properties
```

- 雇佣兵实体自身也保存颜色标签，世界重载后可恢复到对应颜色队伍。
- 队伍命名使用专属前缀，默认 `rep_m_`，只把雇佣兵实体加入队伍，不把玩家加入队伍，避免污染后续任务书团队系统。

## 新增配置项

配置文件仍为：

```text
config/raid_enhancement_patch/battle_support_items.properties
```

新增配置：

```properties
mercenary.glowing.perPlayerColor=true
mercenary.glowing.exclusivePlayerColors=true
mercenary.glowing.releaseColorWhenNoActiveMercenaries=false
mercenary.glowing.colorExhaustedPolicy=DENY
mercenary.glowing.colorPalette=GOLD,YELLOW,GREEN,AQUA,BLUE,LIGHT_PURPLE,RED,DARK_PURPLE,DARK_RED,DARK_AQUA,WHITE,GRAY
mercenary.glowing.teamPrefix=rep_m_
```

说明：

- `perPlayerColor=true`：每个召唤玩家使用自己的颜色。
- `exclusivePlayerColors=true`：不同玩家默认独占颜色。
- `releaseColorWhenNoActiveMercenaries=false`：玩家颜色长期保留，不因为当前没有雇佣兵而释放。
- `colorExhaustedPolicy=DENY`：颜色耗尽后拒绝新玩家召唤。可改为 `REUSE` 允许颜色复用。
- `teamPrefix=rep_m_`：雇佣兵发光队伍前缀，建议不要改成通用名称。

## 安装

移除旧版：

```text
raid_enhancement_patch-0.8.9.7.6-mercenary-performance-persistence.jar
```

只放入新版：

```text
raid_enhancement_patch-0.8.9.7.7-mercenary-player-color-system.jar
```

不要多个版本同时放入 mods 文件夹。

## 测试重点

1. 单人召唤雇佣兵，确认有发光轮廓。
2. 多名玩家先后召唤雇佣兵，确认颜色不同。
3. 退出并重新进入世界，确认已有雇佣兵颜色恢复。
4. 再次召唤同一玩家的雇佣兵，确认仍使用原颜色。
5. 检查任务书或其他团队系统没有把玩家加入 `rep_m_` 队伍。
