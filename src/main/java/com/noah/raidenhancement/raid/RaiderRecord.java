package com.noah.raidenhancement.raid;

import java.util.Objects;
import java.util.UUID;

/** Runtime cache record for one known raider in the current raid wave. */
public record RaiderRecord(
        UUID entityUuid,
        String entityId,
        RaiderCategory category,
        int waveIndex,
        long firstSeenGameTime,
        long lastSeenGameTime,
        int lastKnownX,
        int lastKnownY,
        int lastKnownZ,
        boolean aliveWhenLastSeen,
        int timeWeightSeconds,
        boolean raidCaptainWhenLastSeen
) {
    public RaiderRecord {
        Objects.requireNonNull(entityUuid, "entityUuid");
        Objects.requireNonNull(category, "category");
        entityId = entityId == null || entityId.isBlank() ? "unknown" : entityId;
        if (waveIndex < 1) {
            throw new IllegalArgumentException("waveIndex is one-based and must be >= 1");
        }
        if (firstSeenGameTime < 0L) {
            firstSeenGameTime = 0L;
        }
        if (lastSeenGameTime < firstSeenGameTime) {
            lastSeenGameTime = firstSeenGameTime;
        }
        if (timeWeightSeconds < 0) {
            timeWeightSeconds = 0;
        }
    }

    public RaiderRecord(UUID entityUuid, String entityId, RaiderCategory category, int waveIndex) {
        this(entityUuid, entityId, category, waveIndex, 0L, 0L, 0, 0, 0, true,
                RaiderTimeWeights.weightSeconds(entityId, category, false), false);
    }

    public RaiderRecord(UUID entityUuid, String entityId, RaiderCategory category, int waveIndex,
                        long firstSeenGameTime, long lastSeenGameTime,
                        int lastKnownX, int lastKnownY, int lastKnownZ, boolean aliveWhenLastSeen) {
        this(entityUuid, entityId, category, waveIndex, firstSeenGameTime, lastSeenGameTime,
                lastKnownX, lastKnownY, lastKnownZ, aliveWhenLastSeen,
                RaiderTimeWeights.weightSeconds(entityId, category, false), false);
    }

    public boolean isSpecial() {
        return category == RaiderCategory.RAIDS_ENHANCED_SPECIAL;
    }

    public RaiderRecord refreshed(String newEntityId, RaiderCategory newCategory, int newWaveIndex,
                                  long gameTime, int x, int y, int z, boolean alive) {
        return refreshed(newEntityId, newCategory, newWaveIndex, gameTime, x, y, z, alive,
                RaiderTimeWeights.weightSeconds(newEntityId, newCategory, raidCaptainWhenLastSeen), raidCaptainWhenLastSeen);
    }

    public RaiderRecord refreshed(String newEntityId, RaiderCategory newCategory, int newWaveIndex,
                                  long gameTime, int x, int y, int z, boolean alive,
                                  int newTimeWeightSeconds, boolean newRaidCaptain) {
        RaiderCategory resolvedCategory = category;
        if (newCategory == RaiderCategory.RAIDS_ENHANCED_SPECIAL || category == RaiderCategory.RAIDS_ENHANCED_SPECIAL) {
            resolvedCategory = RaiderCategory.RAIDS_ENHANCED_SPECIAL;
        } else if (newCategory != null) {
            resolvedCategory = newCategory;
        }
        String resolvedId = newEntityId == null || newEntityId.isBlank() || "unknown".equals(newEntityId)
                ? entityId
                : newEntityId;
        int resolvedWave = Math.max(1, newWaveIndex);
        boolean resolvedCaptain = raidCaptainWhenLastSeen || newRaidCaptain;
        int resolvedWeight = Math.max(timeWeightSeconds,
                newTimeWeightSeconds > 0
                        ? newTimeWeightSeconds
                        : RaiderTimeWeights.weightSeconds(resolvedId, resolvedCategory, resolvedCaptain));
        return new RaiderRecord(entityUuid, resolvedId, resolvedCategory, resolvedWave,
                firstSeenGameTime, Math.max(lastSeenGameTime, gameTime), x, y, z, alive,
                resolvedWeight, resolvedCaptain);
    }
}
