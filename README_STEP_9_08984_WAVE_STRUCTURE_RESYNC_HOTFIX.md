# 第九步热修：0.8.9.8.4 波次结构重同步

## 修复目标

修复困难 + 不祥 2 等 8 波袭击出现的：UI 到目标波后，后台又继续刷新 9-11 波额外袭击者；以及扩展波结束后村庄英雄效果不稳定的问题。

## 核心调整

1. 原版/本模组共同约定：1-8 波是 native raid domain，9-11 波才是 custom bridge domain。
2. native safe cap 从 7 修正为 8，保留 RaidWaveIndexClampMixin 作为安全兜底。
3. customExtraWavesNeeded 不再使用历史 plannedCustomExtraWaves 抬高额外波数量。
4. 生命周期快照不再允许新袭击继承旧袭击的 omen、目标波数、custom wave progress。
5. 胜利结算时额外刷新 Hero of the Village，修复自定义额外波链导致原版村庄英雄不稳定的问题。

## 不改动内容

- 不改奖励 JSON 结构。
- 不改村庄恩情机制。
- 不改战场扫荡配置。
- 不改 Raids Enhanced 最后防线魔像回滚与掉落物清理。
