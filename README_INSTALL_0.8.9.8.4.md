# 安装说明：0.8.9.8.4-wave-structure-resync-hotfix

1. 删除 mods 文件夹内旧版本 `raid_enhancement_patch-*.jar`。
2. 只放入 `raid_enhancement_patch-0.8.9.8.4-wave-structure-resync-hotfix.jar`。
3. 不要多个版本共存。
4. 建议删除旧的运行期波次快照：`config/raid_enhancement_patch/raid_lifecycle_snapshots.properties`，避免旧异常袭击状态继续影响新袭击。
5. 本版把波次结构统一为：1-8 波由原版 Raid 承担，9-11 波才进入自定义额外波桥接。
