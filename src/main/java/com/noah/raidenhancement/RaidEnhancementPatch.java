package com.noah.raidenhancement;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.config.BattleSupportConfig;
import com.noah.raidenhancement.config.VictorySettlementConfig;
import com.noah.raidenhancement.config.VillageFavorConfig;
import com.noah.raidenhancement.config.KeyDiagnosticsConfig;
import com.noah.raidenhancement.config.ConfigAuditService;
import com.noah.raidenhancement.event.VillagerProtectionEvents;
import com.noah.raidenhancement.event.BattleSupportEvents;
import com.noah.raidenhancement.event.VillageFavorEvents;
import com.noah.raidenhancement.event.RaidTickCoordinator;
import com.noah.raidenhancement.raid.RaidSessionManager;
import com.noah.raidenhancement.raid.RaidKeyDiagnostics;
import com.noah.raidenhancement.raid.GolemBlockRollbackGuard;
import com.noah.raidenhancement.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entrypoint for the independent raid enhancement patch.
 *
 * <p>0.9.1.9 introduces a single server-level tick coordinator while preserving
 * the tested controller order and all 0.9.1.8 gameplay behavior.</p>
 */
@Mod(RaidEnhancementPatch.MOD_ID)
public final class RaidEnhancementPatch {
    public static final String MOD_ID = "raid_enhancement_patch";
    public static final String VERSION = "0.9.1.9-runtime-boundary-alpha";

    public RaidEnhancementPatch(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        RaidEnhancementConfig.loadOrCreate();
        BattleSupportConfig.loadOrCreate();
        VictorySettlementConfig.loadOrCreate();
        VillageFavorConfig.loadOrCreate();
        KeyDiagnosticsConfig.loadOrCreate();
        ConfigAuditService.logStartup();
        RaidKeyDiagnostics.logStartup(VERSION);
        RaidSessionManager.bootstrap();
        VillagerProtectionController.bootstrap();
        registerNeoForgeEventBus(new RaidTickCoordinator());
        registerNeoForgeEventBus(new VillagerProtectionEvents());
        registerNeoForgeEventBus(new BattleSupportEvents());
        registerNeoForgeEventBus(new VillageFavorEvents());
        registerClientHudIfAvailable();
        // Compatibility hotfix 0.3.3: debug command registration is disabled.
        // Earlier staged builds compiled Brigadier command descriptors from sandbox stubs,
        // which crashed during world creation in large modpacks.
        System.out.println("[Raid Enhancement Patch] Loaded " + VERSION + ". Runtime-boundary foundation: one server-level tick coordinator preserves the tested villager, raid and battle-support execution order; BossBar reads active raid handles through a typed runtime view instead of reflecting into controller-private state. " + GolemBlockRollbackGuard.hotfixMarker() + ". Safe spawning, BossBar behavior, raid wave counts, settlement keys, rewards, VillageFavor, persistence formats, configuration defaults and Mixin enablement are unchanged.");
    }

    /**
     * Registers game-bus listeners through reflection so this staged jar does not bake in
     * an incorrect NeoForge.EVENT_BUS field descriptor when compiled in the sandbox.
     */
    private static void registerNeoForgeEventBus(Object listener) {
        try {
            Class<?> neoForgeClass = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object eventBus = neoForgeClass.getField("EVENT_BUS").get(null);
            eventBus.getClass().getMethod("register", Object.class).invoke(eventBus, listener);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Raid Enhancement Patch listener on NeoForge.EVENT_BUS", exception);
        }
    }

    private static void registerClientHudIfAvailable() {
        try {
            Class.forName("net.minecraft.client.Minecraft");
            Class<?> listenerClass = Class.forName("com.noah.raidenhancement.client.RaidHudClientEvents");
            Object listener = listenerClass.getConstructor().newInstance();
            registerNeoForgeEventBus(listener);
        } catch (ClassNotFoundException ignored) {
            // Dedicated servers do not have Minecraft client/HUD classes.
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Client raid HUD registration failed once and was skipped: " + throwable);
        }
    }

}
