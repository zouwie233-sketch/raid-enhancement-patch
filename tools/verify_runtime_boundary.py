#!/usr/bin/env python3
"""Static architecture, persistence isolation and raid registration contracts for 0.9.2.1."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java/com/noah/raidenhancement"
EXPECTED_VERSION = "0.9.2.1-cross-save-lifecycle-isolation-alpha"


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
    require("RaidLifecycleSnapshotRepository" in controller
            and "repository.replace(properties);" in controller
            and "lifecycleSnapshotsDirty" in controller,
            "save-scoped/coalesced lifecycle persistence is missing")
    require("raid_spawn_queue.properties" in config and "1, 32" in config and "1, 128" in config,
            "clamped server spawn-queue config is missing")
    for safety_check in ["hasChunkAt", "isWithinBounds", "noCollision", "containsFluidOrHazard", "hasStableSupport"]:
        require(safety_check in resolver, f"safe-spawn check disappeared: {safety_check}")


def verify_registration_and_server_lifecycle() -> None:
    entrypoint = text(JAVA_ROOT / "RaidEnhancementPatch.java")
    lifecycle = text(JAVA_ROOT / "event/RaidServerLifecycleEvents.java")
    controller = text(JAVA_ROOT / "raid/RaidExtraWaveController.java")
    require("new RaidServerLifecycleEvents()" in entrypoint,
            "server lifecycle listener is not registered")
    require("ServerStoppingEvent" in lifecycle
            and "checkpointBeforeServerStop(event.getServer());" in lifecycle,
            "stopping event does not checkpoint active queues")
    require("ServerStartingEvent" in lifecycle and "RaidRuntimeRegistry.start(event.getServer());" in lifecycle,
            "starting event does not create the server-scoped runtime context")
    require("RaidRuntimeRegistry.beginStopping(event.getServer());" in lifecycle
            and "RaidRuntimeRegistry.checkpoint(event.getServer());" in lifecycle,
            "stopping event does not drive the runtime context lifecycle")
    require("RaidRuntimeRegistry.close(event.getServer());" in lifecycle,
            "stopped event does not remove the server-scoped runtime context")
    require("ServerStoppedEvent" in lifecycle and "clearRuntimeStateAfterServerStop();" in lifecycle,
            "stopped event does not clear process-local raid state")
    require("RaidSessionManager.clearRuntimeStateAfterServerStop();" in lifecycle
            and "RaidEncounterAuthority.clearRuntimeStateAfterServerStop();" in lifecycle,
            "stopped event leaves raid session/read-model state in the old server process")
    require("method.invoke(state.nativeRaid, safeWave, entity, blockPos, true);" in controller,
            "already-inserted raiders are not using raid-only registration")
    require("method.invoke(state.nativeRaid, safeWave, entity, blockPos, false);" not in controller,
            "duplicate world-insertion joinRaid call remains")
    for runtime_clear in ["STATES.clear()", "TERMINATED_RAID_KEYS.clear()",
                          "LAST_HUD_SNAPSHOTS.clear()", "PERSISTED_LIFECYCLE_SNAPSHOTS.clear()",
                          "lifecycleSnapshotsLoaded = false"]:
        require(runtime_clear in controller, f"server-stop runtime reset missing: {runtime_clear}")


def verify_server_scoped_context() -> None:
    registry = text(JAVA_ROOT / "runtime/RaidRuntimeRegistry.java")
    context = text(JAVA_ROOT / "runtime/RaidRuntimeContext.java")
    diagnostics = text(JAVA_ROOT / "runtime/RaidDiagnosticsContext.java")
    store = text(JAVA_ROOT / "runtime/ServerScopedContextStore.java")
    key_diagnostics = text(JAVA_ROOT / "raid/RaidKeyDiagnostics.java")
    repository = text(JAVA_ROOT / "persistence/RaidLifecycleSnapshotRepository.java")
    require("IdentityHashMap" in store and "closeAndRemove" in store,
            "server context store is not identity-scoped or explicitly removable")
    require("ServerScopedContextStore<MinecraftServer, RaidRuntimeContext>" in registry,
            "runtime registry is not scoped by concrete MinecraftServer")
    require("RaidDiagnosticsContext diagnostics" in context and "diagnostics.close();" in context,
            "runtime context does not own and release diagnostic state")
    require("RaidLifecycleSnapshotRepository lifecycleSnapshots" in context
            and "lifecycleSnapshots.close();" in context,
            "runtime context does not own and release save-scoped lifecycle persistence")
    require("MAX_RATE_LIMIT_ENTRIES = 512" in diagnostics and "lastLogByEventAndKey.clear();" in diagnostics,
            "diagnostic state is not bounded or cleared")
    require("RaidRuntimeRegistry.require(serverLevel).diagnostics()" in key_diagnostics,
            "key diagnostics still bypasses the server-scoped diagnostic owner")
    require("LAST_LOG_BY_EVENT_AND_KEY" not in key_diagnostics,
            "legacy process-global key diagnostic rate-limit map remains")
    require("private MinecraftServer server;" in repository and "server = null;" in repository,
            "lifecycle repository retains its concrete server after close")


def verify_save_scoped_lifecycle_persistence() -> None:
    controller = text(JAVA_ROOT / "raid/RaidExtraWaveController.java")
    repository = text(JAVA_ROOT / "persistence/RaidLifecycleSnapshotRepository.java")
    saved_data = text(JAVA_ROOT / "persistence/RaidLifecycleSavedData.java")
    codec = text(JAVA_ROOT / "persistence/RaidLifecyclePropertiesCodec.java")
    require("extends SavedData" in saved_data and "setDirty();" in saved_data,
            "raid lifecycle metadata is not backed by dirty-tracked Minecraft SavedData")
    require("currentServer.overworld()" in repository
            and "getDataStorage().computeIfAbsent" in repository,
            "raid lifecycle SavedData is not attached to the current save's Overworld")
    require('DATA_FILE_ID = "raid_enhancement_patch_raid_session_lifecycle"' in repository,
            "save-scoped lifecycle data id drifted")
    require("MAX_PAYLOAD_BYTES = 4 * 1024 * 1024" in codec,
            "SavedData payload no longer has a hard safety bound")
    require("lifecyclePersistencePath" not in controller
            and "RAID_SESSION_LIFECYCLE_PERSISTENCE_FILE" not in controller
            and "config/raid_enhancement_patch/raid_session_lifecycle.properties" not in repository,
            "legacy version-global lifecycle sidecar is still a runtime persistence source")
    require('properties.setProperty("storageScope", "minecraft-server-overworld-saved-data")' in controller,
            "persisted lifecycle payload does not declare its save scope")


def main() -> None:
    verify_versions()
    verify_single_tick_entrypoint()
    verify_runtime_boundaries()
    verify_mixins()
    verify_no_removed_bridges()
    verify_bounded_spawn_queue()
    verify_registration_and_server_lifecycle()
    verify_server_scoped_context()
    verify_save_scoped_lifecycle_persistence()
    java_count = sum(1 for _ in JAVA_ROOT.rglob("*.java"))
    require(java_count == 95, f"unexpected top-level Java source count: {java_count}")
    print(f"[runtime-boundary] PASS: version={EXPECTED_VERSION}, javaSources={java_count}")


if __name__ == "__main__":
    main()
