package com.noah.raidenhancement.villager;

import java.util.Objects;
import java.util.UUID;

/** Runtime protection state for one villager. */
public final class ProtectedVillagerState {
    private final UUID villagerUuid;
    private final String dimensionId;
    private final long protectedAtGameTime;
    private long expiresAtGameTime;
    private final String source;
    private long lastEffectRefreshGameTime;
    private float allowedHealth = -1.0F;

    public ProtectedVillagerState(UUID villagerUuid, String dimensionId, long protectedAtGameTime, long expiresAtGameTime, String source) {
        this.villagerUuid = Objects.requireNonNull(villagerUuid, "villagerUuid");
        this.dimensionId = dimensionId == null || dimensionId.isBlank() ? "unknown" : dimensionId;
        this.protectedAtGameTime = protectedAtGameTime;
        this.expiresAtGameTime = expiresAtGameTime;
        this.source = source == null || source.isBlank() ? "unknown" : source;
        this.lastEffectRefreshGameTime = protectedAtGameTime;
    }

    public UUID villagerUuid() {
        return villagerUuid;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public long protectedAtGameTime() {
        return protectedAtGameTime;
    }

    public long expiresAtGameTime() {
        return expiresAtGameTime;
    }

    public String source() {
        return source;
    }

    public long lastEffectRefreshGameTime() {
        return lastEffectRefreshGameTime;
    }

    public void markEffectRefreshed(long gameTime) {
        this.lastEffectRefreshGameTime = gameTime;
    }

    public void extendUntil(long expiresAtGameTime) {
        if (expiresAtGameTime > this.expiresAtGameTime) {
            this.expiresAtGameTime = expiresAtGameTime;
        }
    }

    public boolean hasHealthClamp() {
        return allowedHealth >= 0.0F;
    }

    public float allowedHealth() {
        return allowedHealth;
    }

    public void enableHealthClamp(float health) {
        if (health < 0.0F) {
            return;
        }
        if (!hasHealthClamp()) {
            this.allowedHealth = health;
        } else {
            this.allowedHealth = Math.min(this.allowedHealth, health);
        }
    }

    public void updateAllowedHealth(float health) {
        if (health >= 0.0F) {
            this.allowedHealth = health;
        }
    }

    public void disableHealthClamp() {
        this.allowedHealth = -1.0F;
    }

    public boolean isExpired(long gameTime) {
        return gameTime >= expiresAtGameTime;
    }
}
