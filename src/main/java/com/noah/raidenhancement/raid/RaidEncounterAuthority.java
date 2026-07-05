package com.noah.raidenhancement.raid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 0.8.9.9.0 first-pass encounter authority.
 *
 * This is intentionally a read-model authority rather than a full rewrite. The
 * existing controller still drives vanilla compatibility and spawning, but all
 * presentation and future gate decisions now have one canonical snapshot source.
 */
public final class RaidEncounterAuthority {
    private static final Map<String, RaidEncounterSnapshot> SNAPSHOTS = new LinkedHashMap<>();
    private static boolean announced;
    private static boolean debugHeaderPrinted;

    private RaidEncounterAuthority() {
    }

    public static void publish(RaidEncounterSnapshot snapshot) {
        if (snapshot == null || snapshot.key() == null || snapshot.key().isBlank()) {
            return;
        }
        SNAPSHOTS.put(snapshot.key(), snapshot);
        if (!announced) {
            announced = true;
            System.out.println("[Raid Enhancement Patch] 0.8.9.9.0 RaidEncounterAuthority read-model is active. BossBar, future victory gates and wave bridge checks should consume the same encounter snapshot.");
        }
    }

    public static RaidEncounterSnapshot get(String key) {
        return key == null ? null : SNAPSHOTS.get(key);
    }

    public static List<RaidEncounterSnapshot> snapshots() {
        return List.copyOf(SNAPSHOTS.values());
    }

    public static void remove(String key) {
        if (key != null) {
            SNAPSHOTS.remove(key);
        }
    }

    public static void prune(long gameTime, long staleTicks) {
        long maxAge = Math.max(20L, staleTicks);
        SNAPSHOTS.entrySet().removeIf(entry -> {
            RaidEncounterSnapshot snapshot = entry.getValue();
            return snapshot == null || gameTime - snapshot.gameTime() > maxAge;
        });
    }

    public static String title(String key, long gameTime) {
        RaidEncounterSnapshot snapshot = get(key);
        return snapshot == null ? null : RaidBossbarTitleFormatter.format(snapshot, gameTime);
    }

    public static boolean canUseNativeVictory(RaidEncounterSnapshot snapshot, boolean nativeVictory, boolean nearbyRaidersCleared) {
        if (snapshot == null || !nativeVictory || !nearbyRaidersCleared) {
            return false;
        }
        return !snapshot.hasCustomExtraWaves() || snapshot.customChainCompleted();
    }

    public static void debugDiscovery(String key, String difficulty, int rawOmen, int normalizedOmen,
                                      int planTarget, int nativeLimit, int nativeNumGroups,
                                      int groupsSpawned, int customExtraWaves) {
        if (!debugHeaderPrinted) {
            debugHeaderPrinted = true;
            System.out.println("[Raid Enhancement Patch] Raid wave audit debug: key | difficulty | rawOmen -> omen | target | nativeLimit | numGroups | groupsSpawned | customExtraWaves");
        }
        System.out.println("[Raid Enhancement Patch] Raid wave audit: " + key
                + " | " + difficulty
                + " | " + rawOmen + " -> " + normalizedOmen
                + " | target=" + planTarget
                + " | nativeLimit=" + nativeLimit
                + " | numGroups=" + nativeNumGroups
                + " | groupsSpawned=" + groupsSpawned
                + " | customExtraWaves=" + customExtraWaves);
    }
}
