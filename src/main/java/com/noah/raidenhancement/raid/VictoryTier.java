package com.noah.raidenhancement.raid;

/** Three-grade village raid victory outcome used by the settlement layer. */
public enum VictoryTier {
    PERFECT("perfect", "完胜"),
    VICTORY("victory", "胜利"),
    COSTLY("costly", "惨胜");

    private final String id;
    private final String displayName;

    VictoryTier(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static VictoryTier grade(int defenseFailures, int timeoutPenalties, int damagedVillagerEvents, int timeoutDamagedVillagerEvents) {
        int failures = Math.max(0, defenseFailures);
        int timeouts = Math.max(0, timeoutPenalties);
        int damaged = Math.max(0, damagedVillagerEvents) + Math.max(0, timeoutDamagedVillagerEvents);
        if (failures <= 0 && timeouts <= 0 && damaged <= 0) {
            return PERFECT;
        }
        if (failures >= Math.max(1, com.noah.raidenhancement.config.VictorySettlementConfig.COSTLY_MIN_DEFENSE_FAILURES)
                || timeouts >= Math.max(1, com.noah.raidenhancement.config.VictorySettlementConfig.COSTLY_MIN_TIMEOUT_PENALTIES)
                || damaged >= Math.max(1, com.noah.raidenhancement.config.VictorySettlementConfig.COSTLY_MIN_DAMAGED_VILLAGER_EVENTS)) {
            return COSTLY;
        }
        return VICTORY;
    }
}
