package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables the real block destruction burst of Raids Enhanced's Golem of Last Resort.
 *
 * The original entity calls destroyBlocks() from its own tick method and only destroys
 * blocks after its private damage-trigger timer reaches zero. Cancelling here avoids
 * world damage without monitoring all entities, all hurt events, or all block events.
 */
@Mixin(targets = "com.finderfeed.raids_enhanced.content.entities.golem_of_last_resort.GolemOfLastResort", priority = 700, remap = false)
public abstract class RaidsEnhancedGolemOfLastResortBlockBreakMixin {
    @Shadow(remap = false)
    private int destroyBlocksTick;

    @Inject(method = "destroyBlocks", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void raid_enhancement_patch$disableRealBlockBreaking(CallbackInfo ci) {
        try {
            if (!RaidsEnhancedCompatConfig.golemOfLastResortBlockBreakingEnabled()) {
                if (RaidsEnhancedCompatConfig.resetPendingBreakTimerWhenBlocked()) {
                    this.destroyBlocksTick = 0;
                }
                ci.cancel();
            }
        } catch (Throwable throwable) {
            // Fail closed for world protection: if config loading fails, block destruction stays disabled.
            this.destroyBlocksTick = 0;
            ci.cancel();
            if (RaidsEnhancedCompatConfig.debugLogsEnabled()) {
                System.out.println("[Raid Enhancement Patch] Blocked Golem of Last Resort block breaking after compatibility error: " + throwable);
            }
        }
    }
}
