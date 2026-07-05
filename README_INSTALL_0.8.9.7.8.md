# raid_enhancement_patch 0.8.9.7.8-wave-chain-bridge-hotfix

本版本基于 0.8.9.7.7，只修复额外波 9 -> 10 -> 11 桥接稳定性。

## 修复点

- 冻结自定义额外波总数，避免第 9 波清完后因原版 Raid 观测值变化导致额外波链被误判完成。
- 在额外波链未完成时，继续抑制原版 Raid Victory。
- 如果原版 Raid 在第 9 -> 第 10 波间隙已经短暂进入 Victory 状态，控制器会在桥接窗口内把其恢复为 ONGOING，以便第 10/11 波继续接入原 Raid。
- 不改普通战备令牌、雇佣兵颜色系统、HUD 布局、Raids Enhanced 生成表、清怪/传送逻辑。

## 安装

移除旧版 0.8.9.7.7，只保留：

`raid_enhancement_patch-0.8.9.7.8-wave-chain-bridge-hotfix.jar`
