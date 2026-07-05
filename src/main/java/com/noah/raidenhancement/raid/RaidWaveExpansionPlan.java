package com.noah.raidenhancement.raid;

/**
 * Step 4 raid-wave planning table.
 *
 * This stage only expands the total number of vanilla raid waves. Multi-spawn
 * points and Raids Enhanced special raider injection remain disabled until later
 * stages, even though the same table also records the future attack-point count.
 */
public final class RaidWaveExpansionPlan {
    private final int totalWaves;
    private final int attackPoints;

    private RaidWaveExpansionPlan(int totalWaves, int attackPoints) {
        this.totalWaves = totalWaves;
        this.attackPoints = attackPoints;
    }

    public int totalWaves() {
        return totalWaves;
    }

    public int attackPoints() {
        return attackPoints;
    }

    public static RaidWaveExpansionPlan forRuntimeDifficulty(String difficultyName, int omenLevel) {
        int omen = clampOmenLevel(omenLevel);
        String difficulty = difficultyName == null ? "NORMAL" : difficultyName.toUpperCase(java.util.Locale.ROOT);
        if (difficulty.contains("EASY")) {
            return easy(omen);
        }
        if (difficulty.contains("HARD")) {
            return hard(omen);
        }
        return normal(omen);
    }

    private static RaidWaveExpansionPlan easy(int omen) {
        return switch (omen) {
            case 1 -> new RaidWaveExpansionPlan(3, 1);
            case 2 -> new RaidWaveExpansionPlan(3, 1);
            case 3 -> new RaidWaveExpansionPlan(4, 2);
            case 4 -> new RaidWaveExpansionPlan(4, 2);
            default -> new RaidWaveExpansionPlan(5, 2);
        };
    }

    private static RaidWaveExpansionPlan normal(int omen) {
        return switch (omen) {
            case 1 -> new RaidWaveExpansionPlan(5, 1);
            case 2 -> new RaidWaveExpansionPlan(6, 2);
            case 3 -> new RaidWaveExpansionPlan(6, 2);
            case 4 -> new RaidWaveExpansionPlan(7, 2);
            default -> new RaidWaveExpansionPlan(8, 3);
        };
    }

    private static RaidWaveExpansionPlan hard(int omen) {
        return switch (omen) {
            case 1 -> new RaidWaveExpansionPlan(8, 2);
            case 2 -> new RaidWaveExpansionPlan(8, 2);
            case 3 -> new RaidWaveExpansionPlan(9, 3);
            case 4 -> new RaidWaveExpansionPlan(10, 3);
            default -> new RaidWaveExpansionPlan(11, 4);
        };
    }

    private static int clampOmenLevel(int omenLevel) {
        if (omenLevel < 1) {
            return 1;
        }
        if (omenLevel > 5) {
            return 5;
        }
        return omenLevel;
    }
}
