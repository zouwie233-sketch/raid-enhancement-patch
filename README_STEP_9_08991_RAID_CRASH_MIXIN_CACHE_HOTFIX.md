# 0.8.9.9.1 工作报告：第 8 波崩溃与 Mixin 注解热修

## 问题来源

用户实测困难 + 不祥 2 时，第 8 波开始崩溃。崩溃报告显示：

```text
java.lang.ArrayIndexOutOfBoundsException: Index 8 out of bounds for length 8
at net.minecraft.world.entity.raid.Raid.getDefaultNumSpawns(Raid.java:719)
```

同时 debug.log 显示本模组多个 string-target mixin 未正确准备：

```text
The mixin '...RaidWaveIndexClampMixin' is missing an @Mixin annotation
```

根因不是波次表，而是上一版局部编译 mixin 时使用的注解 stub 保留策略错误，导致部分 Mixin 注解进入 RuntimeVisibleAnnotations，而当前 Mixin 处理器没有把它们识别为有效 @Mixin。

## 本版修复

1. 重新编译所有 mixin class，使 `@Mixin`、`@Inject`、`@ModifyVariable` 等注解进入 RuntimeInvisibleAnnotations，匹配 Sponge Mixin 的预期。
2. 恢复 `RaidWaveIndexClampMixin` 的有效性，用于把原版 Raid spawn table 索引钳制到 0..7，防止第 8/9 波索引越界。
3. 恢复 `RaidVictorySuppressMixin`、Raids Enhanced 兼容 mixin、魔像保护 mixin 的有效加载。
4. 给 BossBar 标题覆盖器增加短期 Identity 缓存：如果本 tick 无法立即从状态映射到 bossbar，也优先使用最近一次已确认的袭击标题，降低标题在“袭击”和“袭击 第 X/Y 波”之间闪回的概率。

## 没有改动

- 没改波次表。
- 没改奖励 JSON。
- 没改村庄恩情数据。
- 没改战备令牌注册链。
- 没改 Raids Enhanced 魔像方块回滚和掉落物清理逻辑。
- 没改 1～8 原版负责、9～11 自定义额外波的总结构。

## 仍需观察

如果第 8 波崩溃消失，但 BossBar 标题仍闪回，则说明原版或其他 UI 模组还有路径绕过 `ServerBossEvent#setName`。下一步应改为独立 ServerBossEvent 波次条，而不是继续抢写原版标题。
