# Build Verification 0.9.1.7

Version: `0.9.1.7-reflection-cache-hotfix-alpha`

## Environment limitation

The included Gradle Wrapper attempted to obtain Gradle 8.12, but the current environment cannot resolve `services.gradle.org`. NeoForge dependency resolution is therefore unavailable and a Gradle `clean build` is not claimed.

## Hotfix artifact method

1. Use the provided 0.9.1.6 project JAR as the binary baseline.
2. Compile only the changed/new project classes with Java 21 against the baseline and compile-time-only external descriptor stubs.
3. Compare descriptors of recompiled Minecraft/NeoForge-facing classes against their 0.9.1.6 bytecode where applicable.
4. Exclude every compile stub from the output.
5. Replace version metadata and package the complete project JAR under the final alpha version name.
6. Run archive, class-version, source/JAR version, Mixin-list, stub-exclusion and changed-entry checks.

## Changed/new runtime classes

- `com.noah.raidenhancement.RaidEnhancementPatch`
- `com.noah.raidenhancement.compat.CachedReflection` and nested key classes
- `com.noah.raidenhancement.compat.MobEffectCompat`
- `com.noah.raidenhancement.event.BattleSupportEvents`
- `com.noah.raidenhancement.event.VillagerProtectionEvents`
- `com.noah.raidenhancement.raid.GolemBlockRollbackGuard` and nested queue/state classes
- `com.noah.raidenhancement.villager.ProtectedVillagerState`
- `com.noah.raidenhancement.villager.VillagerProtectionController`

## Local checks

- Java 21 compilation: passed.
- Cached-reflection and rollback smoke test: passed.
- Per-golem snapshot deduplication smoke test: passed.
- Collision-delayed restore smoke test: passed.
- Safe restore after golem leaves smoke test: passed.
- Stable tick-path reflection-cache-size smoke test: passed.
- Full Minecraft/NeoForge runtime test: pending user test.
- Spark comparison: pending user test.
