# 0.8.9.9.2 Vanilla Bonus Wave numGroups Hotfix

## 修复原因

0.8.9.9.1 中 Mixin 已经加载，但困难 + 不祥 2 进入第 8 波后仍崩溃：

`ArrayIndexOutOfBoundsException: Index 8 out of bounds for length 8`

这说明问题不只是 Mixin 加载，而是我们把原版 `Raid.numGroups` 直接写成逻辑总波数 8。原版不祥袭击会在常规波之外再加奖励波，因此 `numGroups=8` 会让原版尝试访问索引 8。

## 处理方式

逻辑上仍然保持：

- 1～8 波：原版 Raid 负责
- 9～11 波：自定义额外波负责

但写入原版字段时改为：

- 目标原版逻辑 8 波 + 不祥奖励波存在：`numGroups = 7`
- 原版奖励波作为第 8 波
- 困难不祥 3～5 仍然在原版 8 波后进入自定义 9～11 波

## 未改动

没有改战备令牌、村庄保护、胜利奖励、扫荡、恩情、Raids Enhanced 魔像回滚逻辑。
