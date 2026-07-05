# 0.8.9.7.1-component-compat-hotfix

基于 0.8.9.7-security-support-items 的启动热修复版本。

## 修复内容

修复 0.8.9.7 在 NeoForge 21.1.234 启动注册创造模式物品栏时崩溃的问题：

```text
java.lang.IncompatibleClassChangeError:
Method net.minecraft.network.chat.Component.translatable(String) must be InterfaceMethodref constant
```

原因是构建环境把 Minecraft 1.21.1 的 `Component` 静态工厂调用烘焙成了错误的常量池引用类型。

本热修复不改变玩法逻辑，只处理 Component 兼容。

## 未改动内容

- 不改 HUD；
- 不改第 8 波桥接；
- 不改第 9～11 波状态机；
- 不改离场 / 回归核心逻辑；
- 不改 Raids Enhanced 特殊袭击者逻辑；
- 不改原版 Raid 胜负状态；
- 不清怪、不传送袭击者。
