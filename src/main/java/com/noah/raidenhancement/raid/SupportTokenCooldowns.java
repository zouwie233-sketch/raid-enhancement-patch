package com.noah.raidenhancement.raid;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Internal cooldown layer for battle-support items.
 *
 * We intentionally do not use vanilla ItemCooldowns here. Vanilla cooldowns apply to the Item type
 * globally for the player and can show a cooldown overlay on the mod's creative-tab preview stacks.
 * This map enforces the same gameplay cooldown without leaking into inventory/creative UI rendering.
 */
public final class SupportTokenCooldowns {
    private static final Map<Key, Long> COOLDOWNS = new LinkedHashMap<>();
    private static long lastCleanupGameTime;

    private SupportTokenCooldowns() {
    }

    public static long remainingTicks(UUID playerId, String tokenId, long gameTime) {
        if (playerId == null || tokenId == null || tokenId.isBlank()) {
            return 0L;
        }
        Long expire = COOLDOWNS.get(new Key(playerId, tokenId));
        if (expire == null) {
            return 0L;
        }
        long remaining = expire - Math.max(0L, gameTime);
        if (remaining <= 0L) {
            COOLDOWNS.remove(new Key(playerId, tokenId));
            return 0L;
        }
        return remaining;
    }

    public static void start(UUID playerId, String tokenId, long gameTime, int cooldownTicks) {
        if (playerId == null || tokenId == null || tokenId.isBlank() || cooldownTicks <= 0) {
            return;
        }
        COOLDOWNS.put(new Key(playerId, tokenId), Math.max(0L, gameTime) + cooldownTicks);
        cleanup(gameTime);
    }

    public static void cleanup(long gameTime) {
        if (gameTime - lastCleanupGameTime < 200L) {
            return;
        }
        lastCleanupGameTime = gameTime;
        Iterator<Map.Entry<Key, Long>> iterator = COOLDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Long> entry = iterator.next();
            if (entry.getValue() <= gameTime) {
                iterator.remove();
            }
        }
    }

    private record Key(UUID playerId, String tokenId) {
    }
}
