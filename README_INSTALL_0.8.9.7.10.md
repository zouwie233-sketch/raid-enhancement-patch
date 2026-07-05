# 安装说明：0.8.9.7.10-leveltick-event-compat-hotfix

## 文件

- raid_enhancement_patch-0.8.9.7.10-leveltick-event-compat-hotfix.jar

## 修复内容

修复 0.8.9.7.9 进入世界后服务端 tick 崩溃：

```text
NoSuchMethodError: LevelTickEvent$Post.getLevel():Object
BattleSupportEvents.onLevelTickPost
```

原因是新增战备/雇佣兵 tick 事件桥把 LevelTickEvent.Post#getLevel 编译成了错误返回描述符。
本版将 BattleSupportEvents 中 LevelTickEvent、AttackEntityEvent、LivingIncomingDamageEvent 的事件访问改为反射兼容路径，避免 NeoForge 21.1.234 运行时签名不匹配。

## 安装

从 mods 文件夹移除旧版：

```text
raid_enhancement_patch-0.8.9.7.9-shield-token-hotfix.jar
```

只放入新版：

```text
raid_enhancement_patch-0.8.9.7.10-leveltick-event-compat-hotfix.jar
```

不要多个版本同时放入 mods。

## 未改动

- HUD
- 第 8 波桥接
- 第 9～11 波额外波状态机
- Raids Enhanced 特殊袭击者生成逻辑
- 原版 Raid 胜负状态
- 战备令牌配方/图标
- 圣盾令牌 30/100 临时生命机制
- 雇佣兵颜色与持久化系统
