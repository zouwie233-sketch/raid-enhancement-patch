# Raid Enhancement Patch 0.8.9.7.5 - item-use-compat-hotfix

基线：0.8.9.7.4-creative-tab-hotfix。

本热修复只处理战备令牌右键使用时的运行时方法签名适配问题。

## 修复内容

- 移除新令牌物品类中对 `Player#getAbilities()` 的直接调用，避免错误编译成 `Player$Abilities` 返回类型。
- 移除新令牌物品类中对 `Player#getCooldowns()` 的直接调用，避免错误编译成 `Player$Cooldowns` 返回类型。
- 新增 `ItemUseCompat` 反射桥，统一处理：
  - 聊天提示发送；
  - 创造模式判断；
  - 成功使用后消耗物品；
  - 物品冷却。

## 未改动内容

- HUD 未改动；
- 第 8 波桥接未改动；
- 第 9～11 波额外波状态机未改动；
- 离场 / 回归逻辑未改动；
- Raids Enhanced 特殊袭击者逻辑未改动；
- 原版 Raid 胜负状态未改动；
- 不清怪、不传送袭击者；
- 村民保护药水图标隐藏逻辑未改动。

## 安装

从 mods 文件夹移除 0.8.9.7、0.8.9.7.1、0.8.9.7.2、0.8.9.7.3、0.8.9.7.4，只保留：

`raid_enhancement_patch-0.8.9.7.5-item-use-compat-hotfix.jar`

