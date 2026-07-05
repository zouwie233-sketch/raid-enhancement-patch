package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidEnhancementConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory manager for active raid sessions.
 *
 * This stage deliberately avoids SavedData and real Raid hooks. Later stages
 * will call this manager from Mixin/event code and may add persistence when it
 * becomes necessary for server restart recovery.
 */
public final class RaidSessionManager {
    private static final Map<String, RaidSession> ACTIVE_SESSIONS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private RaidSessionManager() {
    }

    public static void bootstrap() {
        if (!bootstrapped) {
            bootstrapped = true;
            System.out.println("[Raid Enhancement Patch] RaidSessionManager bootstrapped with the 0.8.8 known-raider cache, stabilized wave-timeout data layer, non-flickering top-HUD display, delayed wave clear-timer start, session lifecycle metadata layer, non-destructive timeout raider audit layer, and active-combat clock gate.");
        }
    }

    public static String makeSessionKey(String dimensionId, int raidId) {
        String dimension = dimensionId == null || dimensionId.isBlank() ? "unknown" : dimensionId;
        return dimension + "#" + raidId;
    }

    public static RaidSession getOrCreate(String dimensionId, int raidId, long gameTime) {
        String key = makeSessionKey(dimensionId, raidId);
        return ACTIVE_SESSIONS.computeIfAbsent(key, ignored -> new RaidSession(key, dimensionId, raidId, gameTime));
    }

    public static RaidSession getOrCreateByKey(String sessionKey, String dimensionId, int raidId, long gameTime) {
        String key = sessionKey == null || sessionKey.isBlank() ? makeSessionKey(dimensionId, raidId) : sessionKey;
        return ACTIVE_SESSIONS.computeIfAbsent(key, ignored -> new RaidSession(key, dimensionId, raidId, gameTime));
    }

    public static Optional<RaidSession> get(String dimensionId, int raidId) {
        return Optional.ofNullable(ACTIVE_SESSIONS.get(makeSessionKey(dimensionId, raidId)));
    }

    public static Optional<RaidSession> get(String sessionKey) {
        return Optional.ofNullable(ACTIVE_SESSIONS.get(sessionKey));
    }

    public static Collection<RaidSession> activeSessions() {
        return List.copyOf(ACTIVE_SESSIONS.values());
    }

    public static int activeSessionCount() {
        return ACTIVE_SESSIONS.size();
    }

    public static boolean closeSession(String dimensionId, int raidId, String reason, long gameTime, boolean failed) {
        Optional<RaidSession> session = get(dimensionId, raidId);
        if (session.isEmpty()) {
            return false;
        }
        if (failed) {
            session.get().markFailed(reason, gameTime);
        } else {
            session.get().markCompleted(reason, gameTime);
        }
        return true;
    }

    public static int cleanupClosedSessions() {
        int before = ACTIVE_SESSIONS.size();
        ACTIVE_SESSIONS.entrySet().removeIf(entry -> entry.getValue().isClosed());
        return before - ACTIVE_SESSIONS.size();
    }

    public static int cleanupInactiveSessions(long currentGameTime) {
        long maxInactiveTicks = Math.max(RaidEnhancementConfig.SESSION_CLEANUP_INTERVAL_TICKS * 6L,
                RaidEnhancementConfig.KNOWN_RAIDER_CACHE_INACTIVE_SESSION_TTL_TICKS);
        int before = ACTIVE_SESSIONS.size();
        ACTIVE_SESSIONS.entrySet().removeIf(entry -> currentGameTime - entry.getValue().lastUpdatedGameTime() > maxInactiveTicks);
        return before - ACTIVE_SESSIONS.size();
    }

    public static void clearAllForDebug() {
        ACTIVE_SESSIONS.clear();
    }
}
