# raid_enhancement_patch 0.8.9.6.2 - Villager Effect Visibility Hotfix

基线：0.8.9.6.1-security-buff-repair-hotfix。

本次只修复一个体验边界：0.8.9.6.1 为了让安防铁傀儡抗性显示出来，把 MobEffectCompat 的可见性改成了全局可见，导致旧村民保护效果也显示为药水图标。

0.8.9.6.2 调整为：

- 安防铁傀儡的 Resistance 仍然使用可见效果和图标；
- 村民旧保护效果恢复为隐藏/无图标，不新增新的村民药水效果；
- 不移除原本已经存在的村民保护机制；
- 不改变村民生命钳制、外部伤害保护、防线崩溃扣血、超时惩罚等逻辑。

未改动：HUD、第 8 波桥接、第 9～11 波额外波状态机、离场/回归冻结、Raids Enhanced 特殊袭击者生成、原版 Raid 胜负状态、清怪或传送逻辑。
