# 权威信息所有权矩阵

状态：设计完成，等待实施确认  
原则：单一权威信息来源不是“一个类拥有全部信息”，而是“每一种事实只有一个可写所有者”。

## 事实所有权

| 事实 | 当前来源 | 目标权威所有者 | 持久化 | 只读消费者 | 禁止行为 |
|---|---|---|---|---|---|
| 原版 Raid 状态、原版组数、原版中心 | 原版 `Raid` + 多处反射 | `VanillaRaidPort` 读取的原版 `Raid` | Minecraft 原版存档 | `RaidEncounterService` | Mod 投影不得伪造原版事实 |
| 袭击实例身份 | 多种字符串 Key | `RaidEncounterId` | `RaidEncounterSavedData` | 全部领域模块 | 消费者不得自行拼接另一种实例 Key |
| 长期村庄身份 | 中心坐标字符串 | `VillageId` | 世界数据 | 好感、结算 | 不得与单次袭击身份混用 |
| 难度和不祥等级快照 | 原版对象、Session、State | `RaidEncounter` 创建快照 | 是 | 波次规则、结算、UI | 袭击中途不得因配置重载漂移 |
| 目标总波数和原版/自定义边界 | `RaidWaveAuthority` | `RaidRuleSnapshot` + `RaidWaveAuthority` 纯规则 | 随活动袭击保存 | 状态机、HUD、结算 | 运行时观测值不得重写目标总数 |
| 当前逻辑阶段 | 多个布尔字段 | `RaidEncounter.phase` | 是 | 门控、应用服务、投影 | 其他模块不得直接改阶段 |
| 当前逻辑波次 | State、Session、Snapshot | `RaidEncounter.logicalWave` | 是 | 生成、村庄防御、HUD | UI 不得回写波次 |
| 离开/返回冻结状态 | `ExtraWaveState` 多字段 | `RaidEncounter` 状态机 | 是 | 波次调度器 | 空实体扫描不得推进阶段 |
| 胜利是否允许 | Controller 内逻辑 | `RaidVictoryGate` 读取聚合快照 | 否，结果可重算 | Mixin、结算服务 | Mixin 不得遍历全局状态表 |
| 待生成槽位和批次幂等 | `RaidSpawnWorkQueue` | `RaidSpawnWorkQueue` | 是 | `RaidSpawnExecutor` | 规划器不得直接生成实体 |
| 生成坐标是否安全 | `SafeRaidSpawnResolver` | `SafeRaidSpawnResolver` | 否 | `RaidSpawnExecutor` | 兼容分支不得绕过最终安全裁决 |
| 村庄防御累计事实 | `SecuritySession`、`RaidSession` | `VillageDefenseState` | 是 | 评级、结算、HUD | 消息模块不得修改评分 |
| 袭击参与者 | Session 和附近玩家推断 | `RaidParticipationState` | 是 | 结算 | 结算不得自行建立第二套参与者规则 |
| 是否已结算 | 全局 Properties + Set | `SettlementLedger` | 世界 `SavedData` | 结算服务、诊断 | 奖励发放不得先于幂等预留 |
| 玩家×村庄好感 | `VillageFavorState` 和旧记录 | `VillageFavorRepository` | 世界 `SavedData` | 交互、礼物、结算 | 旧 V1 路径不得继续双写 |
| 支援令牌冷却 | 全局静态 Map | `SupportCooldownState` | 是 | 支援物品服务 | 不得跨世界共享 `gameTime` |
| 雇佣兵稳定身份 | 实体标签 + 静态 Map | Minecraft 实体标签为事实，`MercenaryIndex` 为缓存 | 实体随世界保存；索引不保存 | 跟随、效果、颜色 | 不得周期遍历全部已加载实体重建常规索引 |
| BossBar/HUD 内容 | 多个状态源和本地缓存 | `RaidPresentationProjection` | 否 | BossBar、ActionBar | 展示层不得拥有完成判定 |
| 诊断限流和计数 | 多个静态表 | `RaidDiagnosticsContext` | 否 | Logger | 诊断状态不得影响玩法门控 |

## 目标依赖方向

```text
NeoForge Events / Commands / Mixins
                |
                v
        application services
                |
                v
 domain encounter / spawn / village / settlement
                ^
                |
 persistence / Minecraft adapter / Raids Enhanced adapter

presentation <--- immutable query snapshots
```

## 包边界规则

| 目标包 | 职责 | 允许依赖 | 禁止依赖 |
|---|---|---|---|
| `domain.encounter` | 身份、阶段、转换、不变量 | Java 标准库、纯领域类型 | Minecraft、NeoForge、Mixin、反射、UI |
| `domain.spawn` | 生成计划、队列、幂等结果 | 领域类型 | 世界扫描、日志文件、BossBar |
| `domain.village` | 防御累计事实和评级 | 领域类型 | 实体反射、消息发送 |
| `application` | 用例编排、事务边界、事件派发 | domain、端口接口 | 具体 Mixin 和外部 Mod 类 |
| `infrastructure.minecraft` | 原版对象访问、实体生成、SavedData | Minecraft、domain 端口 | 玩法决策 |
| `integration.raidsenhanced` | 外部 Mod 防腐层 | 兼容 API/反射、domain 端口 | BossBar、结算策略 |
| `presentation` | BossBar、ActionBar、聊天 | 不可变快照 | 领域状态写入、持久化写入 |
| `config` | 配置解析、Codec、不可变规则注册表 | 数据类型 | 活动袭击直接变更 |

## 写入规则

1. 只有聚合或专属状态对象能修改其事实。
2. 应用服务通过命令请求转换，不直接设置字段。
3. 投影可以随时重建，因此不作为永久事实保存。
4. 原版事实与 Mod 事实冲突时，由显式协调策略处理并记录原因，不允许消费者自行选择“看起来合理”的值。
