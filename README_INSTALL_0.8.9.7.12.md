# raid_enhancement_patch 0.8.9.7.12-registry-create-compat-hotfix

## 修复内容

- 修复 0.8.9.7.11 在启动加载阶段因 `DeferredRegister.create(Object, String)` 方法描述符不匹配导致的崩溃。
- 新增源码级反射兼容方案，避免后续通过不同 NeoForge 1.21.1 开发桩重建时再次把注册表工厂调用编译成错误描述符。
- 保留 0.8.9.7.11 的四个控制令牌、内部冷却隔离、高级集敌令牌默认不消耗、洞察/集敌配置项。

## 安装

移除旧版：

```text
raid_enhancement_patch-0.8.9.7.11-control-tokens-cooldown-isolation.jar
```

只放入新版：

```text
raid_enhancement_patch-0.8.9.7.12-registry-create-compat-hotfix.jar
```

不要多个版本同时放入 mods 文件夹。
