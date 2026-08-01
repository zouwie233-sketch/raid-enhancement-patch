#!/usr/bin/env python3
"""Static architecture and persistent bounded-spawn contracts for 0.9.1.11."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java/com/noah/raidenhancement"
EXPECTED_VERSION = "0.9.1.11-raid-spawn-persistence-beta"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"[runtime-boundary] FAIL: {message}")


def text(path: Path) -> str:
    require(path.is_file(), f"missing file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def verify_versions() -> None:
    gradle = text(ROOT / "gradle.properties")
    entrypoint = text(JAVA_ROOT / "RaidEnhancementPatch.java")
    metadata = text(ROOT / "src/main/resources/META-INF/neoforge.mods.toml")
    require(f"mod_version={EXPECTED_VERSION}" in gradle, "gradle.properties version drift")
    require(f'VERSION = "{EXPECTED_VERSION}"' in entrypoint, "entrypoint version drift")
    require(f'version="{EXPECTED_VERSION}"' in metadata, "neoforge.mods.toml version drift")


def verify_single_tick_entrypoint() -> None:
    tick_users = []
    for path in JAVA_ROOT.rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        if "LevelTickEvent" in source:
            tick_users.append(path.relative_to(JAVA_ROOT).as_posix())
    require(tick_users == ["event/RaidTickCoordinator.java"],
            f"LevelTickEvent must have one owner, found: {tick_users}")

    coordinator = text(JAVA_ROOT / "event/RaidTickCoordinator.java")
    calls = [
        "tickVillagerProtection(level);",
        "tickRaidRuntime(level);",
        "tickBattleSupport(level);",
    ]
    positions = [coordinator.find(call) for call in calls]
    require(all(position >= 0 for position in positions), "coordinator is missing a legacy tick group")
    require(positions == sorted(positions), "legacy post-tick execution order changed")


def verify_runtime_boundaries() -> None:
    bossbar = text(JAVA_ROOT / "raid/RaidIndependentBossbarManager.java")
    battle = text(JAVA_ROOT / "raid/BattleSupportController.java")
    require('getDeclaredField("STATES")' not in bossbar,
            "BossBar still reflects into RaidExtraWaveController.STATES")
    require("stateByKey(" not in bossbar, "legacy BossBar private-state bridge remains")
    require('getDeclaredField("SESSIONS")' not in battle,
            "Battle support still reflects into VillageSecurityController.SESSIONS")
    require("RaidRuntimeView runtime" in bossbar, "BossBar does not consume RaidRuntimeView")
    require("VillageSecurityRuntimeView" in battle,
            "Battle support does not consume VillageSecurityRuntimeView")


def verify_mixins() -> None:
    config_path = ROOT / "src/main/resources/raid_enhancement_patch.mixins.json"
    config = json.loads(text(config_path))
    configured = set(config.get("mixins", []))
    sources = {path.stem for path in (JAVA_ROOT / "mixin").glob("*.java")}
    require(configured == sources,
            f"Mixin source/config drift: configuredOnly={sorted(configured - sources)}, "
            f"sourceOnly={sorted(sources - configured)}")


def verify_no_removed_bridges() -> None:
    removed = [
        JAVA_ROOT / "event/RaidWaveExpansionEvents.java",
        JAVA_ROOT / "mixin/ServerBossEventRaidTitleMixin.java",
        JAVA_ROOT / "raid/RaidBossbarTitleOverride.java",
    ]
    require(not any(path.exists() for path in removed), "removed legacy bridge source reappeared")


def verify_bounded_spawn_queue() -> None:
    controller = text(JAVA_ROOT / "raid/RaidExtraWaveController.java")
    queue = text(JAVA_ROOT / "raid/RaidSpawnWorkQueue.java")
    tick_budget = text(JAVA_ROOT / "raid/RaidSpawnTickBudget.java")
    codec = text(JAVA_ROOT / "raid/RaidSpawnQueuePersistenceCodec.java")
    key_service = text(JAVA_ROOT / "raid/RaidKeyService.java")
    resolver = text(JAVA_ROOT / "raid/SafeRaidSpawnResolver.java")
    config = text(JAVA_ROOT / "config/RaidEnhancementConfig.java")
    require("processPendingSpawnWork(level, state, gameTime);" in controller,
            "raid controller does not drain bounded spawn work")
    require("spawnCustomWaveWithMainAndReinforcements" not in controller,
            "legacy all-at-once custom spawning remains")
    require("RAID_SPAWN_QUEUE_MAX_SLOTS_PER_TICK = 8" in config,
            "bounded per-tick slot limit changed")
    require("RAID_SPAWN_QUEUE_MAX_ATTEMPTS_PER_SLOT = 12" in config,
            "bounded per-slot retry limit changed")
    require("RAID_SPAWN_QUEUE_GLOBAL_MAX_SLOTS_PER_LEVEL_TICK = 16" in config,
            "shared per-level tick limit changed")
    require("reservedBatchIds" in queue and "preferredAnchorIndex() + pending.attempts" in queue,
            "spawn idempotency or anchor rotation contract is missing")
    require("WeakHashMap" in tick_budget and "budget.remaining -= granted" in tick_budget,
            "shared tick admission budget contract is missing")
    require("QueueSnapshot" in queue and "RestoreReport restore" in queue,
            "queue snapshot/restore contract is missing")
    require("MAX_PENDING_SLOTS = 512" in codec and "spawnQueueSnapshot" in controller,
            "bounded persistence codec or lifecycle integration is missing")
    require("persistLifecycleSnapshot(state, gameTime, true);" in controller,
            "spawn queue does not checkpoint event-driven changes")
    require("nativeRaidNumericId" in key_service and "differentNativeRaid" in controller,
            "stable native raid identity guard is missing")
    require("StandardCopyOption.ATOMIC_MOVE" in controller and "lifecycleSnapshotsDirty" in controller,
            "atomic/coalesced lifecycle persistence is missing")
    require("raid_spawn_queue.properties" in config and "1, 32" in config and "1, 128" in config,
            "clamped server spawn-queue config is missing")
    for safety_check in ["hasChunkAt", "isWithinBounds", "noCollision", "containsFluidOrHazard", "hasStableSupport"]:
        require(safety_check in resolver, f"safe-spawn check disappeared: {safety_check}")


def main() -> None:
    verify_versions()
    verify_single_tick_entrypoint()
    verify_runtime_boundaries()
    verify_mixins()
    verify_no_removed_bridges()
    verify_bounded_spawn_queue()
    java_count = sum(1 for _ in JAVA_ROOT.rglob("*.java"))
    require(java_count == 87, f"unexpected top-level Java source count: {java_count}")
    print(f"[runtime-boundary] PASS: version={EXPECTED_VERSION}, javaSources={java_count}")


if __name__ == "__main__":
    main()
