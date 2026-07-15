#!/usr/bin/env python3
"""Static architecture contract checks for the 0.9.1.9 runtime-boundary stage."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java/com/noah/raidenhancement"
EXPECTED_VERSION = "0.9.1.9-runtime-boundary-alpha"


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


def main() -> None:
    verify_versions()
    verify_single_tick_entrypoint()
    verify_runtime_boundaries()
    verify_mixins()
    verify_no_removed_bridges()
    java_count = sum(1 for _ in JAVA_ROOT.rglob("*.java"))
    require(java_count == 84, f"unexpected top-level Java source count: {java_count}")
    print(f"[runtime-boundary] PASS: version={EXPECTED_VERSION}, javaSources={java_count}")


if __name__ == "__main__":
    main()
