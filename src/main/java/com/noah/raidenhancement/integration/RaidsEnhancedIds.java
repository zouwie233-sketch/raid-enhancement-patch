package com.noah.raidenhancement.integration;

import java.util.Set;

/** String constants for optional Raids Enhanced integration. */
public final class RaidsEnhancedIds {
    public static final String MOD_ID = "raidsenhanced";
    public static final String RAID_DRILL = "raidsenhanced:raid_drill";
    public static final String GOLEM_OF_LAST_RESORT = "raidsenhanced:golem_of_last_resort";
    public static final String ZAPPER = "raidsenhanced:zapper";
    public static final String RAID_BLIMP = "raidsenhanced:raid_blimp";

    public static final Set<String> SPECIAL_RAIDER_IDS = Set.of(
            RAID_DRILL,
            GOLEM_OF_LAST_RESORT,
            ZAPPER,
            RAID_BLIMP
    );

    private RaidsEnhancedIds() {
    }

    public static boolean isSpecialRaiderId(String entityId) {
        return entityId != null && SPECIAL_RAIDER_IDS.contains(entityId);
    }
}
