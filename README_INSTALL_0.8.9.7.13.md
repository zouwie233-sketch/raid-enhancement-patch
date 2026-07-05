# 安装说明 0.8.9.7.13-creative-output-itemlike-hotfix

修复 0.8.9.7.12 打开创造物品栏崩溃的问题。

核心修复：创造栏内容输出从 CreativeModeTab.Output#accept(Item) 改为 NeoForge 21.1.234 兼容的 accept(ItemLike)。

安装：移除旧版 raid_enhancement_patch JAR，只保留本版本 JAR。

保留：洞察/集敌令牌、高级集敌不消耗、内部冷却隔离、圣盾 30/100、雇佣兵颜色与持久化、第 9->10->11 波桥接修复。
