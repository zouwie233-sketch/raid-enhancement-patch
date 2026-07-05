package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.integration.RaidsEnhancedIds;

/**
 * Step 8.2: deterministic time-weight table for dynamic wave clear budgets.
 *
 * The values are seconds added per known raider. They model real clear cost,
 * not just health: ranged pressure, healing, summons, flight, and forced-hit
 * mechanics all matter. The raid drill intentionally has the highest weight
 * because its multi-hit removal mechanic cannot be bypassed by high damage.
 */
public final class RaiderTimeWeights {
    public static final String VANILLA_PILLAGER = "minecraft:pillager";
    public static final String VANILLA_VINDICATOR = "minecraft:vindicator";
    public static final String VANILLA_WITCH = "minecraft:witch";
    public static final String VANILLA_EVOKER = "minecraft:evoker";
    public static final String VANILLA_RAVAGER = "minecraft:ravager";

    private RaiderTimeWeights() {
    }

    public static int weightSeconds(String entityId, RaiderCategory category, boolean raidCaptain) {
        int base = baseWeightSeconds(entityId, category);
        if (raidCaptain) {
            base += Math.max(0, RaidEnhancementConfig.RAIDER_TIME_WEIGHT_RAID_CAPTAIN_BONUS_SECONDS);
        }
        return Math.max(0, base);
    }

    private static int baseWeightSeconds(String entityId, RaiderCategory category) {
        String id = normalize(entityId);
        if (RaidsEnhancedIds.RAID_DRILL.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_RAID_DRILL_SECONDS;
        }
        if (RaidsEnhancedIds.RAID_BLIMP.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_RAID_BLIMP_SECONDS;
        }
        if (RaidsEnhancedIds.GOLEM_OF_LAST_RESORT.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_GOLEM_OF_LAST_RESORT_SECONDS;
        }
        if (RaidsEnhancedIds.ZAPPER.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_ZAPPER_SECONDS;
        }
        if (VANILLA_PILLAGER.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_PILLAGER_SECONDS;
        }
        if (VANILLA_VINDICATOR.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_VINDICATOR_SECONDS;
        }
        if (VANILLA_WITCH.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_WITCH_SECONDS;
        }
        if (VANILLA_EVOKER.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_EVOKER_SECONDS;
        }
        if (VANILLA_RAVAGER.equals(id)) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_RAVAGER_SECONDS;
        }
        if (category == RaiderCategory.RAIDS_ENHANCED_SPECIAL) {
            return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_UNKNOWN_RAIDS_ENHANCED_SPECIAL_SECONDS;
        }
        return RaidEnhancementConfig.RAIDER_TIME_WEIGHT_UNKNOWN_RAIDER_SECONDS;
    }

    public static int baseBudgetSecondsForWave(int waveIndex) {
        if (waveIndex <= 3) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_1_TO_3;
        }
        if (waveIndex <= 6) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_4_TO_6;
        }
        if (waveIndex <= 8) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_7_TO_8;
        }
        if (waveIndex == 9) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_9;
        }
        if (waveIndex == 10) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_10;
        }
        return RaidEnhancementConfig.WAVE_TIME_BUDGET_BASE_SECONDS_WAVE_11_PLUS;
    }

    public static int extraWaveBonusSeconds(int waveIndex) {
        if (waveIndex == 9) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_EXTRA_WAVE_9_SECONDS;
        }
        if (waveIndex == 10) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_EXTRA_WAVE_10_SECONDS;
        }
        if (waveIndex >= 11) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_EXTRA_WAVE_11_PLUS_SECONDS;
        }
        return 0;
    }

    public static int maxBudgetSecondsForWave(int waveIndex) {
        if (waveIndex <= 3) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_1_TO_3;
        }
        if (waveIndex <= 6) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_4_TO_6;
        }
        if (waveIndex <= 8) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_7_TO_8;
        }
        if (waveIndex == 9) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_9;
        }
        if (waveIndex == 10) {
            return RaidEnhancementConfig.WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_10;
        }
        return RaidEnhancementConfig.WAVE_TIME_BUDGET_MAX_SECONDS_WAVE_11_PLUS;
    }

    private static String normalize(String entityId) {
        return entityId == null ? "" : entityId.trim().toLowerCase();
    }
}
