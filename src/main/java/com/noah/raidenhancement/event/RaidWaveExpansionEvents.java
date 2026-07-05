package com.noah.raidenhancement.event;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.raid.RaidWaveExpansionController;
import com.noah.raidenhancement.raid.RaidExtraWaveController;
import com.noah.raidenhancement.raid.VictorySettlementController;
import com.noah.raidenhancement.raid.RaidIndependentBossbarManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.lang.reflect.Method;

/** Server-level event bridge for raid wave expansion. Uses reflection for NeoForge 21.1.x event accessor compatibility. */
public final class RaidWaveExpansionEvents {
    private static boolean warnedFailure;
    private static boolean warnedAccessorFailure;

    @SubscribeEvent
    public void onLevelTickPre(LevelTickEvent.Pre event) {
        tickPre(callNoArg(event, "getLevel"));
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        tickPost(callNoArg(event, "getLevel"));
    }

    /**
     * 0.8.9.9.0: Pre tick is now reserved for native Raid.numGroups repair only.
     * Discovery, custom wave bridge, UI read-model publishing and settlement run in
     * Post tick so the same wave cannot be advanced twice in one server tick.
     */
    private void tickPre(Object levelObject) {
        if (!RaidEnhancementConfig.WAVE_EXPANSION_ENABLED) {
            return;
        }
        if (!(levelObject instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            RaidWaveExpansionController.tick(serverLevel);
        } catch (Throwable throwable) {
            warnFailure("pre", throwable);
        }
    }

    private void tickPost(Object levelObject) {
        if (!RaidEnhancementConfig.WAVE_EXPANSION_ENABLED) {
            return;
        }
        if (!(levelObject instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            RaidExtraWaveController.tick(serverLevel);
            VictorySettlementController.tick(serverLevel);
            RaidIndependentBossbarManager.tick(serverLevel);
        } catch (Throwable throwable) {
            warnFailure("post", throwable);
        }
    }

    private static void warnFailure(String phase, Throwable throwable) {
        if (!warnedFailure) {
            warnedFailure = true;
            System.out.println("[Raid Enhancement Patch] Raid wave expansion " + phase
                    + " event failed once and was suppressed: " + throwable);
        }
    }

    private static Object callNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable throwable) {
            if (!warnedAccessorFailure) {
                warnedAccessorFailure = true;
                System.out.println("[Raid Enhancement Patch] RaidWaveExpansionEvents could not access event method " + methodName + " reflectively: " + throwable);
            }
            return null;
        }
    }
}
