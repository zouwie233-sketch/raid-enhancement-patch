package com.noah.raidenhancement.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Deprecated. Wave UI is now integrated into the vanilla raid bossbar title. */
public final class RaidHudClientEvents {
    public RaidHudClientEvents() {
    }

    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        // Intentionally disabled. Do not draw the old top-center gray custom HUD.
    }
}
