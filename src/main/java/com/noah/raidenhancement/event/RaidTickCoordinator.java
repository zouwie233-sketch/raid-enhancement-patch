package com.noah.raidenhancement.event;

import com.mojang.logging.LogUtils;
import com.noah.raidenhancement.compat.CachedReflection;
import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.raid.BattleSupportController;
import com.noah.raidenhancement.raid.GolemBlockRollbackGuard;
import com.noah.raidenhancement.raid.MercenaryGolemController;
import com.noah.raidenhancement.raid.RaidExtraWaveController;
import com.noah.raidenhancement.raid.RaidIndependentBossbarManager;
import com.noah.raidenhancement.raid.RaidWaveExpansionController;
import com.noah.raidenhancement.raid.VictorySettlementController;
import com.noah.raidenhancement.villager.RaidAutoVillagerProtector;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

/**
 * Single server-level tick entrypoint for all Raid Enhancement runtime modules.
 *
 * <p>This foundation stage deliberately preserves the legacy execution order and failure
 * boundaries. Controllers still own their existing state and behavior; later stages can move
 * work behind explicit budgets without multiplying event listeners again.</p>
 */
public final class RaidTickCoordinator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean warnedAccessorFailure;
    private static boolean warnedWavePreFailure;
    private static boolean warnedVillagerPostFailure;
    private static boolean warnedRaidPostFailure;
    private static boolean warnedBattlePostFailure;

    @SubscribeEvent
    public void onLevelTickPre(LevelTickEvent.Pre event) {
        ServerLevel level = serverLevel(event);
        if (level == null || !RaidEnhancementConfig.WAVE_EXPANSION_ENABLED) {
            return;
        }
        try {
            RaidWaveExpansionController.tick(level);
        } catch (Throwable throwable) {
            if (!warnedWavePreFailure) {
                warnedWavePreFailure = true;
                LOGGER.error("Raid wave expansion pre-tick failed once and was suppressed", throwable);
            }
        }
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        ServerLevel level = serverLevel(event);
        if (level == null) {
            return;
        }

        tickVillagerProtection(level);
        tickRaidRuntime(level);
        tickBattleSupport(level);
    }

    private static void tickVillagerProtection(ServerLevel level) {
        if (!RaidEnhancementConfig.VILLAGER_PROTECTION_ENABLED) {
            return;
        }
        try {
            long gameTime = level.getGameTime();
            if (gameTime % RaidEnhancementConfig.VILLAGER_PROTECTION_SWEEP_INTERVAL_TICKS == 0L) {
                VillagerProtectionController.maintainLevel(level);
            }
            RaidAutoVillagerProtector.tick(level);
        } catch (Throwable throwable) {
            if (!warnedVillagerPostFailure) {
                warnedVillagerPostFailure = true;
                LOGGER.error("Villager protection post-tick failed once and was suppressed", throwable);
            }
        }
    }

    private static void tickRaidRuntime(ServerLevel level) {
        if (!RaidEnhancementConfig.WAVE_EXPANSION_ENABLED) {
            return;
        }
        try {
            RaidExtraWaveController.tick(level);
            VictorySettlementController.tick(level);
            RaidIndependentBossbarManager.tick(level);
        } catch (Throwable throwable) {
            if (!warnedRaidPostFailure) {
                warnedRaidPostFailure = true;
                LOGGER.error("Raid runtime post-tick failed once and was suppressed", throwable);
            }
        }
    }

    private static void tickBattleSupport(ServerLevel level) {
        try {
            BattleSupportController.tick(level);
            MercenaryGolemController.tick(level);
            GolemBlockRollbackGuard.tick(level);
        } catch (Throwable throwable) {
            if (!warnedBattlePostFailure) {
                warnedBattlePostFailure = true;
                LOGGER.error("Battle support post-tick failed once and was suppressed", throwable);
            }
        }
    }

    private static ServerLevel serverLevel(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object level = CachedReflection.invoke(event, "getLevel");
            return level instanceof ServerLevel serverLevel ? serverLevel : null;
        } catch (Throwable throwable) {
            if (!warnedAccessorFailure) {
                warnedAccessorFailure = true;
                LOGGER.error("Raid tick coordinator could not access the NeoForge level tick event", throwable);
            }
            return null;
        }
    }
}
