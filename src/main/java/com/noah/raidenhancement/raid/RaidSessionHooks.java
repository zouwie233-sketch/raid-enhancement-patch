package com.noah.raidenhancement.raid;

import java.util.List;
import java.util.UUID;

/**
 * Narrow hook facade that future Mixin classes should call.
 *
 * Keeping this layer separate makes later Raid mixins simpler and gives future
 * development a single place to update if Minecraft's Raid internals change.
 */
public final class RaidSessionHooks {
    private RaidSessionHooks() {
    }

    public static RaidSession onWavePlanned(
            String dimensionId,
            int raidId,
            long gameTime,
            int waveIndex,
            int totalWaves,
            RaidDifficulty difficulty,
            int omenLevel,
            List<AttackPoint> attackPoints
    ) {
        RaidSession session = RaidSessionManager.getOrCreate(dimensionId, raidId, gameTime);
        session.beginWave(new RaidWavePlan(waveIndex, totalWaves, difficulty, omenLevel, attackPoints), gameTime);
        return session;
    }

    public static RaidSession onWavePlanned(
            String sessionKey,
            String dimensionId,
            int raidId,
            long gameTime,
            int waveIndex,
            int totalWaves,
            RaidDifficulty difficulty,
            int omenLevel,
            List<AttackPoint> attackPoints
    ) {
        RaidSession session = RaidSessionManager.getOrCreateByKey(sessionKey, dimensionId, raidId, gameTime);
        session.beginWave(new RaidWavePlan(waveIndex, totalWaves, difficulty, omenLevel, attackPoints), gameTime);
        return session;
    }

    public static boolean onRaiderJoined(
            String dimensionId,
            int raidId,
            long gameTime,
            UUID entityUuid,
            String entityId,
            RaiderCategory category,
            int waveIndex
    ) {
        RaidSession session = RaidSessionManager.getOrCreate(dimensionId, raidId, gameTime);
        session.touch(gameTime);
        return session.addRaider(new RaiderRecord(entityUuid, entityId, category, waveIndex,
                gameTime, gameTime, 0, 0, 0, true,
                RaiderTimeWeights.weightSeconds(entityId, category, false), false));
    }

    public static boolean onRaiderObserved(
            String sessionKey,
            String dimensionId,
            int raidId,
            long gameTime,
            UUID entityUuid,
            String entityId,
            RaiderCategory category,
            int waveIndex,
            int x,
            int y,
            int z,
            boolean alive
    ) {
        return onRaiderObserved(sessionKey, dimensionId, raidId, gameTime, entityUuid, entityId, category,
                waveIndex, x, y, z, alive, RaiderTimeWeights.weightSeconds(entityId, category, false), false);
    }

    public static boolean onRaiderObserved(
            String sessionKey,
            String dimensionId,
            int raidId,
            long gameTime,
            UUID entityUuid,
            String entityId,
            RaiderCategory category,
            int waveIndex,
            int x,
            int y,
            int z,
            boolean alive,
            int timeWeightSeconds,
            boolean raidCaptain
    ) {
        RaidSession session = RaidSessionManager.getOrCreateByKey(sessionKey, dimensionId, raidId, gameTime);
        return session.recordRaiderObservation(entityUuid, entityId, category, waveIndex, gameTime, x, y, z, alive,
                timeWeightSeconds, raidCaptain);
    }

    public static boolean onRaiderRemoved(String dimensionId, int raidId, UUID entityUuid, long gameTime) {
        return RaidSessionManager.get(dimensionId, raidId)
                .map(session -> {
                    session.touch(gameTime);
                    return session.removeRaider(entityUuid);
                })
                .orElse(false);
    }

    public static boolean onVillagerProtected(String dimensionId, int raidId, UUID villagerUuid, int gameTick) {
        RaidSession session = RaidSessionManager.getOrCreate(dimensionId, raidId, gameTick);
        return session.protectVillager(villagerUuid, gameTick);
    }

    public static boolean onRaidClosed(String dimensionId, int raidId, long gameTime, boolean failed, String reason) {
        return RaidSessionManager.closeSession(dimensionId, raidId, reason, gameTime, failed);
    }
}
