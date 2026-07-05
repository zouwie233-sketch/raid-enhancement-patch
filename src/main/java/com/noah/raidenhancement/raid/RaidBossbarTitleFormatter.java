package com.noah.raidenhancement.raid;

/**
 * 0.8.9.9.0: one canonical formatter for raid bossbar wave titles.
 *
 * UI must only present the authoritative raid runtime facts. It must not infer
 * wave totals, custom-wave status, cleanup state or victory state on its own.
 */
public final class RaidBossbarTitleFormatter {
    private RaidBossbarTitleFormatter() {
    }

    public static String format(int currentWave, int totalWaves, boolean customWave, String clearTimerText) {
        int total = Math.max(1, totalWaves);
        int current = clamp(currentWave, 1, total);
        StringBuilder text = new StringBuilder(48);
        text.append("袭击 第 ").append(current).append(" / ").append(total).append(" 波");
        if (customWave) {
            text.append("｜最后攻势！");
        }
        if (clearTimerText != null && !clearTimerText.isBlank()) {
            text.append("｜清剿 ").append(clearTimerText.trim());
        }
        return text.toString();
    }

    public static String format(RaidEncounterSnapshot snapshot, long gameTime) {
        if (snapshot == null) {
            return "";
        }
        return format(snapshot.currentWave(), snapshot.totalWaves(), snapshot.customWave(), snapshot.clearTimerText(gameTime));
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
