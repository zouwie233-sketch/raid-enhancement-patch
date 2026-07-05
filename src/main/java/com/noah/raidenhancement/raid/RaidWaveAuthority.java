package com.noah.raidenhancement.raid;

/**
 * 0.8.9.8.5 authoritative raid-wave structure.
 *
 * This is the single source used by native numGroups writing, HUD display,
 * extra-wave bridge gating, victory suppression and settlement waiting.
 * Waves 1-8 belong to the native Raid. Only waves 9-11 are owned by the
 * custom extra-wave bridge. Runtime observations such as groupsSpawned are
 * progress facts; they must not rewrite the configured target total.
 */
public final class RaidWaveAuthority {
    public static final int NATIVE_WAVE_LIMIT = 8;

    private RaidWaveAuthority() {
    }

    public static RaidWaveExpansionPlan plan(String difficultyName, int omenLevel) {
        return RaidWaveExpansionPlan.forRuntimeDifficulty(difficultyName, clampOmen(omenLevel));
    }

    public static int targetTotalWaves(String difficultyName, int omenLevel) {
        return plan(difficultyName, omenLevel).totalWaves();
    }

    public static int nativeWaveLimitForTarget(int targetTotalWaves) {
        int target = Math.max(1, targetTotalWaves);
        return Math.min(target, NATIVE_WAVE_LIMIT);
    }

    public static int customFirstWave(int targetTotalWaves) {
        return hasCustomExtraWaves(targetTotalWaves) ? NATIVE_WAVE_LIMIT + 1 : 0;
    }

    public static int customWaveCount(int targetTotalWaves) {
        return Math.max(0, Math.max(1, targetTotalWaves) - NATIVE_WAVE_LIMIT);
    }

    public static boolean hasCustomExtraWaves(int targetTotalWaves) {
        return customWaveCount(targetTotalWaves) > 0;
    }

    public static boolean isCustomWave(int logicalWave, int targetTotalWaves) {
        return hasCustomExtraWaves(targetTotalWaves)
                && logicalWave > NATIVE_WAVE_LIMIT
                && logicalWave <= Math.max(1, targetTotalWaves);
    }

    public static int clampDisplayWave(int observedOrLogicalWave, int targetTotalWaves) {
        return clamp(observedOrLogicalWave, 1, Math.max(1, targetTotalWaves));
    }

    public static int clampOmen(int omenLevel) {
        return clamp(omenLevel, 1, 5);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
