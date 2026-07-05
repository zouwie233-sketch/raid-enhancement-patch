package com.noah.raidenhancement.favor;

import java.util.UUID;

/** Server-side authority row for one player and one rescued village region. */
public final class VillageFavorRecord {
    public static final int DATA_VERSION = 2;

    public final String key;
    public final String dimensionId;
    public final int centerX;
    public final int centerY;
    public final int centerZ;
    public final int radius;
    public final UUID playerUuid;
    public final long createdTime;
    public long lastUpdatedTime;

    /** Long-term relationship level, raised only by authoritative raid victory settlement. */
    public int favorLevel;
    public int victoryCount;
    public int highestOmenLevelWon;
    public int raidMeritScore;

    /** Gift / greeting cooldowns are player x village scoped, not villager scoped. */
    public long lastGiftTime;
    public long lastGreetingTime;
    public int totalClaimedGiftCount;
    public int giftClaimsInCurrentPeriod;
    public long giftPeriodStartTime;

    public VillageFavorRecord(String key, String dimensionId, int centerX, int centerY, int centerZ, int radius,
                              UUID playerUuid, long createdTime) {
        this.key = key;
        this.dimensionId = dimensionId;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = Math.max(16, radius);
        this.playerUuid = playerUuid;
        this.createdTime = Math.max(0L, createdTime);
        this.lastUpdatedTime = Math.max(0L, createdTime);
        this.favorLevel = 1;
        this.victoryCount = 1;
        this.highestOmenLevelWon = 1;
        this.raidMeritScore = 1;
        this.giftPeriodStartTime = Math.max(0L, createdTime);
    }

    public boolean contains(double x, double y, double z) {
        double dx = x - (centerX + 0.5D);
        double dy = y - centerY;
        double dz = z - (centerZ + 0.5D);
        double radiusSq = (double) radius * (double) radius;
        return dx * dx + dy * dy + dz * dz <= radiusSq;
    }

    public double distanceSq(double x, double y, double z) {
        double dx = x - (centerX + 0.5D);
        double dy = y - centerY;
        double dz = z - (centerZ + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }
}
