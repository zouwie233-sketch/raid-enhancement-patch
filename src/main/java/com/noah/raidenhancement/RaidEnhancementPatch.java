package com.noah.raidenhancement;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.config.BattleSupportConfig;
import com.noah.raidenhancement.config.VictorySettlementConfig;
import com.noah.raidenhancement.config.VillageFavorConfig;
import com.noah.raidenhancement.config.KeyDiagnosticsConfig;
import com.noah.raidenhancement.event.VillagerProtectionEvents;
import com.noah.raidenhancement.event.BattleSupportEvents;
import com.noah.raidenhancement.event.VillageFavorEvents;
import com.noah.raidenhancement.event.RaidWaveExpansionEvents;
import com.noah.raidenhancement.raid.RaidSessionManager;
import com.noah.raidenhancement.raid.RaidKeyDiagnostics;
import com.noah.raidenhancement.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import net.neoforged.fml.common.Mod;

/**
 * Step 3 entrypoint for the independent NeoForge raid enhancement patch.
 *
 * Stage 0.4.0 starts Step 4. It keeps the user-tested 0.3.9 villager
 * protection baseline and adds compatibility-first raid total wave expansion.
 * Multi-spawn points and special raider injection remain disabled.
 */
@Mod(RaidEnhancementPatch.MOD_ID)
public final class RaidEnhancementPatch {
    public static final String MOD_ID = "raid_enhancement_patch";
    public static final String VERSION = "0.9.1.3-bossbar-module-boundary-alpha";

    public RaidEnhancementPatch(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        RaidEnhancementConfig.loadOrCreate();
        BattleSupportConfig.loadOrCreate();
        VictorySettlementConfig.loadOrCreate();
        VillageFavorConfig.loadOrCreate();
        KeyDiagnosticsConfig.loadOrCreate();
        RaidKeyDiagnostics.logStartup(VERSION);
        RaidSessionManager.bootstrap();
        VillagerProtectionController.bootstrap();
        registerNeoForgeEventBus(new VillagerProtectionEvents());
        registerNeoForgeEventBus(new RaidWaveExpansionEvents());
        registerNeoForgeEventBus(new BattleSupportEvents());
        registerNeoForgeEventBus(new VillageFavorEvents());
        registerClientHudIfAvailable();
        // Compatibility hotfix 0.3.3: debug command registration is disabled.
        // Earlier staged builds compiled Brigadier command descriptors from sandbox stubs,
        // which crashed during world creation in large modpacks.
        System.out.println("[Raid Enhancement Patch] Loaded " + VERSION + ". Keeps the 0.9.1.0 BossBar / settlementKey safety anchor and the 0.9.1.2 Key audit polish fields, then adds BossBar module boundary diagnostics only. It does not change BossBar progress math, waveChange, baselineReset, settlement keys, key formats, raid waves, rewards, VillageFavor, villager gifts, persistent data, VictoryBarAttachGuard behavior or the ServerBossEventRaidTitleMixin disabled state.");
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
