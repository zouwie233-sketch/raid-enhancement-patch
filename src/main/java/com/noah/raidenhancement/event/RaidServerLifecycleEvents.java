package com.noah.raidenhancement.event;

import com.noah.raidenhancement.raid.RaidExtraWaveController;
import com.noah.raidenhancement.raid.RaidEncounterAuthority;
import com.noah.raidenhancement.raid.RaidSessionManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Owns server lifecycle transitions for the persistent extra-wave runtime.
 *
 * <p>The stopping phase checkpoints active work while server state is still
 * available. The stopped phase then discards process-local mirrors without
 * deleting the sidecar, forcing the next integrated or dedicated server to
 * recover from validated persisted data.</p>
 */
public final class RaidServerLifecycleEvents {
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RaidExtraWaveController.checkpointBeforeServerStop();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        RaidExtraWaveController.clearRuntimeStateAfterServerStop();
        RaidEncounterAuthority.clearRuntimeStateAfterServerStop();
        RaidSessionManager.clearRuntimeStateAfterServerStop();
    }
}
