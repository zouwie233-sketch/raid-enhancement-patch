package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.config.VillageFavorConfig;
import com.noah.raidenhancement.raid.RaidKeyDiagnostics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Facade that keeps raid capture, state, interaction, reward and display separated. */
public final class VillageFavorSystem {
    private VillageFavorSystem() {
    }

    public static int favorLevelFor(ServerLevel level, String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid) {
        if (!VillageFavorConfig.ENABLED || playerUuid == null) {
            return 0;
        }
        try {
            return VillageFavorState.favorLevelFor(level, dimensionId, centerX, centerY, centerZ, playerUuid);
        } catch (Throwable throwable) {
            debug("favorLevelFor failed", throwable);
            return 0;
        }
    }

    public static void recordRaidVictory(ServerLevel level, String dimensionId, int centerX, int centerY, int centerZ,
                                         Collection<? extends Player> players, long gameTime) {
        recordRaidVictory(level, dimensionId, centerX, centerY, centerZ, players, gameTime, 1, "NORMAL", false);
    }

    public static void recordRaidVictory(ServerLevel level, String dimensionId, int centerX, int centerY, int centerZ,
                                         Collection<? extends Player> players, long gameTime,
                                         int omenLevel, String difficultyName, boolean extraWaveCompleted) {
        if (!VillageFavorConfig.ENABLED || level == null || players == null || players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            if (player == null || !player.isAlive()) {
                continue;
            }
            try {
                String favorRecordKey = VillageFavorState.recordKey(dimensionId, centerX, centerY, centerZ, player.getUUID());
                RaidKeyDiagnostics.logFavorRecord("before-recordRaidVictory", level, dimensionId, centerX, centerY, centerZ,
                        player.getUUID(), favorRecordKey, gameTime, null);
                VillageFavorRecord record = VillageFavorState.recordVictory(level, dimensionId, centerX, centerY, centerZ,
                        VillageFavorConfig.VILLAGE_RADIUS, player.getUUID(), gameTime,
                        omenLevel, difficultyName, extraWaveCompleted);
                RaidKeyDiagnostics.logFavorRecord("after-recordRaidVictory", level, dimensionId, centerX, centerY, centerZ,
                        player.getUUID(), favorRecordKey, gameTime, record);
                if (record != null) {
                    FavorDisplay.sendVictoryRecorded(player, record);
                }
            } catch (Throwable throwable) {
                debug("recordRaidVictory failed for player", throwable);
            }
        }
    }

    public static void onVillagerInteracted(ServerLevel level, Player player, Entity villager) {
        if (!VillageFavorConfig.ENABLED || level == null || player == null || villager == null || !player.isAlive()) {
            return;
        }
        if (!isVillagerLike(villager)) {
            return;
        }
        try {
            String dimension = dimensionId(level);
            long gameTime = level.getGameTime();
            Optional<VillageFavorRecord> optional = VillageFavorState.findForInteraction(level, dimension, player.getUUID(),
                    villager.getX(), villager.getY(), villager.getZ());
            if (optional.isEmpty()) {
                RaidKeyDiagnostics.logFavorInteraction("no-record-for-villager", level, dimension, player.getUUID(),
                        villager.getX(), villager.getY(), villager.getZ(), null, gameTime);
                return;
            }
            VillageFavorRecord record = optional.get();
            RaidKeyDiagnostics.logFavorInteraction("matched-record-for-villager", level, dimension, player.getUUID(),
                    villager.getX(), villager.getY(), villager.getZ(), record, gameTime);
            boolean greetingReady = VillageFavorConfig.ENABLE_GREETING
                    && (record.lastGreetingTime <= 0L
                    || gameTime - record.lastGreetingTime >= Math.max(200L, VillageFavorConfig.GREETING_COOLDOWN_TICKS));
            boolean giftGiven = false;
            if (VillageFavorConfig.ENABLE_GIFT) {
                giftGiven = FavorReward.tryGiveGift(level, player, record, villager, gameTime);
            }
            if (greetingReady || giftGiven) {
                if (greetingReady) {
                    VillageFavorState.markGreeting(level, record, gameTime);
                }
                FavorDisplay.sendGreeting(player, record, giftGiven);
                FavorDisplay.playInteractionFeedback(level, player, villager);
            }
        } catch (Throwable throwable) {
            debug("onVillagerInteracted failed", throwable);
        }
    }

    private static boolean isVillagerLike(Entity entity) {
        if (entity == null) {
            return false;
        }
        Class<?> cursor = entity.getClass();
        while (cursor != null) {
            String name = cursor.getName();
            if ("net.minecraft.world.entity.npc.Villager".equals(name)
                    || "net.minecraft.world.entity.npc.AbstractVillager".equals(name)
                    || "net.minecraft.world.entity.npc.WanderingTrader".equals(name)) {
                return true;
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    private static String dimensionId(ServerLevel level) {
        try {
            Object dimension = level.getClass().getMethod("dimension").invoke(level);
            Object location = dimension == null ? null : dimension.getClass().getMethod("location").invoke(dimension);
            return location == null ? "unknown" : location.toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static void debug(String message, Throwable throwable) {
        if (VillageFavorConfig.ENABLE_DEBUG_LOG || VillageFavorConfig.ENABLE_GIFT_DEBUG_LOG) {
            System.out.println("[Raid Enhancement Patch] Village favor debug: " + message + ": " + throwable);
        }
    }
}
