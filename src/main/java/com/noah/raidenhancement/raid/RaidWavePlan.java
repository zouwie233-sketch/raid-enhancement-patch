package com.noah.raidenhancement.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable-like plan snapshot for the current wave. */
public final class RaidWavePlan {
    private final int waveIndex;
    private final int totalWaves;
    private final RaidDifficulty difficulty;
    private final int omenLevel;
    private final List<AttackPoint> attackPoints;

    public RaidWavePlan(int waveIndex, int totalWaves, RaidDifficulty difficulty, int omenLevel, List<AttackPoint> attackPoints) {
        if (waveIndex < 1) {
            throw new IllegalArgumentException("waveIndex must be >= 1");
        }
        if (totalWaves < waveIndex) {
            throw new IllegalArgumentException("totalWaves must be >= waveIndex");
        }
        if (omenLevel < 0) {
            throw new IllegalArgumentException("omenLevel cannot be negative");
        }
        this.waveIndex = waveIndex;
        this.totalWaves = totalWaves;
        this.difficulty = difficulty == null ? RaidDifficulty.NORMAL : difficulty;
        this.omenLevel = omenLevel;
        this.attackPoints = Collections.unmodifiableList(new ArrayList<>(attackPoints == null ? List.of() : attackPoints));
    }

    public int waveIndex() {
        return waveIndex;
    }

    public int totalWaves() {
        return totalWaves;
    }

    public RaidDifficulty difficulty() {
        return difficulty;
    }

    public int omenLevel() {
        return omenLevel;
    }

    public List<AttackPoint> attackPoints() {
        return attackPoints;
    }

    public AttackPoint mainAttackPointOrNull() {
        return attackPoints.stream()
                .filter(point -> point.role() == AttackPointRole.MAIN_ATTACK_POINT)
                .findFirst()
                .orElse(null);
    }

    public boolean isFinalWave() {
        return waveIndex == totalWaves;
    }

    public boolean isMiddleWave() {
        return waveIndex == (int) Math.ceil(totalWaves * 0.5D);
    }

    public int totalPlannedVanillaRaiders() {
        return attackPoints.stream().mapToInt(AttackPoint::plannedVanillaRaiderCount).sum();
    }
}
