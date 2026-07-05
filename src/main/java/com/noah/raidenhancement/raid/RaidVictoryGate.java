package com.noah.raidenhancement.raid;

/**
 * 0.8.9.9.0 first-pass victory gate.
 *
 * This class deliberately contains only pure rule decisions. It does not award,
 * spawn, clear entities, mutate vanilla Raid, or touch world state.
 */
public final class RaidVictoryGate {
    private RaidVictoryGate() {
    }

    public static boolean shouldSuppressNativeVictory(RaidEncounterSnapshot snapshot) {
        if (snapshot == null || !snapshot.hasCustomExtraWaves()) {
            return false;
        }
        return !snapshot.customChainCompleted() || snapshot.bridgeActive();
    }

    public static boolean canSettle(RaidEncounterSnapshot snapshot, boolean nativeRaidFinished, boolean nearbyRaidersCleared) {
        if (snapshot == null || !nativeRaidFinished || !nearbyRaidersCleared) {
            return false;
        }
        if (!snapshot.hasCustomExtraWaves()) {
            return true;
        }
        return snapshot.customChainCompleted();
    }
}
