package com.noah.raidenhancement.raid;

/** Vanilla difficulty values used by the raid planning table. */
public enum RaidDifficulty {
    PEACEFUL,
    EASY,
    NORMAL,
    HARD;

    public static RaidDifficulty fromName(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        for (RaidDifficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(value.trim())) {
                return difficulty;
            }
        }
        return NORMAL;
    }
}
