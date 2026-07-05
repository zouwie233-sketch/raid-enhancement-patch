package com.noah.raidenhancement.raid;

import net.minecraft.server.level.ServerLevel;
import com.noah.raidenhancement.config.RaidEnhancementConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Reflection-based raid detection helper for compatibility-first staged builds.
 *
 * The class intentionally avoids direct references to Raid / BlockPos descriptors.
 * This keeps the staged jar safer in large modpacks while we are still building
 * the final Mixin-based raid system.
 */
public final class RaidDetectionHelper {
    private static final String RAIDER_CLASS_NAME = "net.minecraft.world.entity.raid.Raider";
    private static volatile Boolean raidPositionMethodAvailable;

    private RaidDetectionHelper() {
    }

    public static boolean isVillagerInsideActiveRaid(ServerLevel level, Villager villager) {
        if (level == null || villager == null || villager.level().isClientSide()) {
            return false;
        }
        Boolean byRaidMethod = tryVanillaRaidPositionCheck(level, villager);
        if (byRaidMethod != null) {
            return byRaidMethod;
        }
        return hasNearbyVanillaRaiderFallback(level, villager);
    }

    /**
     * Tries vanilla ServerLevel raid position methods without baking method descriptors.
     * Returns null when the current runtime does not expose a compatible method.
     */
    private static Boolean tryVanillaRaidPositionCheck(ServerLevel level, Villager villager) {
        if (Boolean.FALSE.equals(raidPositionMethodAvailable)) {
            return null;
        }
        Object blockPos = invokeNoArg(villager, "blockPosition");
        if (blockPos == null) {
            raidPositionMethodAvailable = false;
            return null;
        }
        try {
            Method getRaidAt = level.getClass().getMethod("getRaidAt", blockPos.getClass());
            Object raid = getRaidAt.invoke(level, blockPos);
            raidPositionMethodAvailable = true;
            return isActiveRaidObject(raid);
        } catch (ReflectiveOperationException ignored) {
            // Try isRaided below.
        }
        try {
            Method isRaided = level.getClass().getMethod("isRaided", blockPos.getClass());
            Object result = isRaided.invoke(level, blockPos);
            raidPositionMethodAvailable = true;
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException ignored) {
            raidPositionMethodAvailable = false;
        }
        return null;
    }


    private static boolean isActiveRaidObject(Object raid) {
        if (raid == null) {
            return false;
        }
        return !invokeBoolean(raid, "isStopped")
                && !invokeBoolean(raid, "isVictory")
                && !invokeBoolean(raid, "isLoss");
    }

    private static boolean invokeBoolean(Object target, String methodName) {
        Object result = invokeNoArg(target, methodName);
        return result instanceof Boolean bool && bool;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    /**
     * Fallback only used when vanilla raid-position methods are unavailable.
     * It is deliberately conservative: protection requires an actual vanilla Raider
     * near the villager, which avoids global scanning or protecting ordinary villages.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean hasNearbyVanillaRaiderFallback(ServerLevel level, Villager villager) {
        try {
            Class<?> raiderClass = Class.forName(RAIDER_CLASS_NAME);
            List<?> nearbyRaiders = level.getEntitiesOfClass((Class) raiderClass,
                    villager.getBoundingBox().inflate(RaidEnhancementConfig.AUTO_RAID_RAIDER_FALLBACK_RADIUS));
            for (Object raider : nearbyRaiders) {
                if (raider instanceof Entity entity && entity.isAlive()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // If the Raider class or method signature is not available, fail closed.
        }
        return false;
    }
}
