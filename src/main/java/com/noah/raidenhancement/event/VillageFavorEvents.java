package com.noah.raidenhancement.event;

import com.noah.raidenhancement.favor.VillageFavorSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Interaction-only bridge for the structural village favor V1 system. */
public final class VillageFavorEvents {
    private static boolean warnedFailure;

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event == null) {
            return;
        }
        try {
            Object hand = callNoArg(event, "getHand");
            if (hand != null && hand.toString().toUpperCase(java.util.Locale.ROOT).contains("OFF")) {
                return;
            }
            Object playerObject = callNoArg(event, "getEntity");
            Object targetObject = callNoArg(event, "getTarget");
            Object levelObject = callNoArg(event, "getLevel");
            if (!(playerObject instanceof Player player) || !(targetObject instanceof Entity target)) {
                return;
            }
            ServerLevel serverLevel = levelObject instanceof ServerLevel direct ? direct : tryPlayerServerLevel(player);
            if (serverLevel == null) {
                return;
            }
            VillageFavorSystem.onVillagerInteracted(serverLevel, player, target);
        } catch (Throwable throwable) {
            if (!warnedFailure) {
                warnedFailure = true;
                System.out.println("[Raid Enhancement Patch] Village favor interaction event failed once and was suppressed: " + throwable);
            }
        }
    }

    private static Object callNoArg(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ServerLevel tryPlayerServerLevel(Player player) {
        try {
            Object level = player.getClass().getMethod("level").invoke(player);
            return level instanceof ServerLevel serverLevel ? serverLevel : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
