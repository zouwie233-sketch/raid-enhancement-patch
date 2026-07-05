package com.noah.raidenhancement.raid;

/**
 * Immutable read model for one active raid encounter.
 *
 * This is the first step toward a proper RaidRuntimeSession authority: UI,
 * bridge logic and future victory gates can consume the same snapshot instead
 * of re-reading multiple controller-specific fields.
 */
public record RaidEncounterSnapshot(
        String key,
        String dimensionId,
        int centerX,
        int centerY,
        int centerZ,
        String difficultyName,
        int omenLevel,
        int currentWave,
        int totalWaves,
        int nativeWaveLimit,
        int customWaveCount,
        int customWavesCompleted,
        boolean customWave,
        boolean bridgeActive,
        boolean victoryPending,
        long gameTime,
        boolean clearTimerAvailable,
        boolean clearTimedOut,
        boolean clearClockPaused,
        long clearTimeoutGameTime
) {
    public boolean hasCustomExtraWaves() {
        return customWaveCount > 0;
    }

    public boolean customChainCompleted() {
        return customWaveCount <= 0 || customWavesCompleted >= customWaveCount;
    }

    public String clearTimerText(long nowGameTime) {
        if (!clearTimerAvailable) {
            return "";
        }
        if (clearClockPaused) {
            return "暂停";
        }
        if (clearTimedOut) {
            return "超时";
        }
        if (clearTimeoutGameTime <= 0L) {
            return "";
        }
        long remainingTicks = clearTimeoutGameTime - Math.max(0L, nowGameTime);
        int remainingSeconds = (int) Math.max(0L, (remainingTicks + 19L) / 20L);
        if (remainingSeconds <= 0) {
            return "超时";
        }
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        return (minutes < 10 ? "0" + minutes : String.valueOf(minutes))
                + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }
}
