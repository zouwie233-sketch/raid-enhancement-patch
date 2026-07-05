# raid_enhancement_patch 0.8.9.6.1-security-buff-repair-hotfix

基线：0.8.9.6-village-security-deployment-doctrine。

本热修复只处理安防铁傀儡正规军强化没有明显生效的问题：

1. 抗性效果现在会在铁傀儡实体加入世界后再次应用，避免预插入阶段效果被静默忽略。
2. MobEffectCompat 创建效果时改为 visible=true、showIcon=true，抗性效果应能在支持效果显示的界面中被看到。
3. 每波开始的存活安防铁傀儡恢复会被强制记录为本波维护；如果波次开始瞬间维护被加载边界错过，下一次安防 tick 会补做一次。
4. 新刷安防铁傀儡加入世界后会再次满血维护并应用抗性。
5. 存活单位恢复文本改用 messages.combatBuff 控制，不再误挂在 messages.redeployment 下。

未改 HUD、第 8 波桥接、第 9～11 波额外波状态机、离场/回归冻结、Raids Enhanced 特殊袭击者生成、原版 Raid 胜负状态、清怪或传送逻辑。
