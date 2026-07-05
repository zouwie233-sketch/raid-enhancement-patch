# raid_enhancement_patch 0.8.9.2-village-security-polish

## 基线

基于已测试通过的：

- 0.8.9.1-village-security-configurable

本版本只做村庄安防系统的文本体验、边界反馈、debug 开关和说明文档补充。
没有改 HUD、额外波桥接、离场/回归冻结、Raids Enhanced 特殊袭击者生成、原版 Raid 失败状态、袭击者清理或传送逻辑。

## 新增内容

### 1. 胜利分级文本

袭击胜利时，村民防卫同盟会根据本场安防防线表现给出分级：

- 完胜：本场袭击中安防铁傀儡防线没有任何一波全灭。
- 惨胜：袭击最终胜利，但至少一波安防铁傀儡全灭并导致村民生命惩罚。

规则非常保守，只读取本模组安防系统已记录的防守失败次数，不影响原版 Raid 结果。

### 2. 防守失败次数统计

每一波安防铁傀儡全灭时：

- 仍然只触发一次村民生命惩罚；
- 记录 defenseFailureCount；
- 记录本次受惩罚村民数量；
- 进入战后总结。

### 3. 战后总结信息

袭击结束后聊天栏会补充一条战后总结。

胜利总结包含：

- 整备波次数；
- 防守失败次数；
- 村民受冲击人次；
- 回收临时安防铁傀儡数量；
- 普通雇佣军返回数量。

失败/非胜利总结包含：

- 最高接战波次；
- 防守失败次数；
- 村民受冲击人次；
- 回收临时安防铁傀儡数量。

### 4. 更好的 debug 日志开关

新增配置项：

```properties
debug.enabled=false
debug.wavePreparation=false
debug.breach=false
debug.completion=false
debug.golemTrim=false
```

默认关闭详细 debug，避免正常游玩时控制台刷屏。
需要排查时，先打开：

```properties
debug.enabled=true
```

再按需要开启具体子项。

### 5. README 和安装说明

新增：

- README_STEP_8_0892.md
- README_INSTALL_0.8.9.2.md

用于说明版本定位、安装、配置项和测试建议。

## 新增配置项

```properties
messages.victoryGrade=true
messages.battleSummary=true

debug.enabled=false
debug.wavePreparation=false
debug.breach=false
debug.completion=false
debug.golemTrim=false
```

## 兼容性说明

本版本继续面向：

- Minecraft 1.21.1
- NeoForge 21.1.234
- Raids Enhanced 1.0.2
- fdlib 1.0.9

Raids Enhanced 与 fdlib 仍然是可选兼容依赖。
