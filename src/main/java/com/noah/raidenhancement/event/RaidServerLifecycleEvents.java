package com.noah.raidenhancement.event;

import com.noah.raidenhancement.raid.RaidExtraWaveController;
import com.noah.raidenhancement.raid.RaidEncounterAuthority;
import com.noah.raidenhancement.raid.RaidSessionManager;
import com.noah.raidenhancement.runtime.RaidRuntimeRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Owns server lifecycle transitions for the persistent extra-wave runtime.
 *
 * <p>The stopping phase checkpoints active work while server state is still
 * available. The stopped phase then discards process-local mirrors without
 * deleting save-scoped SavedData, forcing only the same save to recover from
 * its own validated persisted data.</p>
 */
public final class RaidServerLifecycleEvents {
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RaidRuntimeRegistry.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RaidRuntimeRegistry.beginStopping(event.getServer());
        try {
            RaidExtraWaveController.checkpointBeforeServerStop(event.getServer());
        } finally {
            RaidRuntimeRegistry.checkpoint(event.getServer());
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        try {
            RaidExtraWaveController.clearRuntimeStateAfterServerStop();
            RaidEncounterAuthority.clearRuntimeStateAfterServerStop();
            RaidSessionManager.clearRuntimeStateAfterServerStop();
        } finally {
            RaidRuntimeRegistry.close(event.getServer());
        }
    }
}
