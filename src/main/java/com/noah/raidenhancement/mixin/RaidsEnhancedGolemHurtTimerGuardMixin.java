package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Second-layer guard: Raids Enhanced sets destroyBlocksTick = 5 after a valid hurt.
 * Reset that timer immediately when block breaking is disabled, so later ticks have
 * no pending real block destruction to execute.
 */
@Mixin(targets = "com.finderfeed.raids_enhanced.content.entities.golem_of_last_resort.GolemOfLastResort", priority = 650, remap = false)
public abstract class RaidsEnhancedGolemHurtTimerGuardMixin {
    @Shadow(remap = false)
    private int destroyBlocksTick;

    @Inject(method = "hurt", at = @At("RETURN"), require = 0, remap = false)
    private void raid_enhancement_patch$clearPendingBlockBreakAfterHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!RaidsEnhancedCompatConfig.golemOfLastResortBlockBreakingEnabled()) {
                this.destroyBlocksTick = 0;
            }
        } catch (Throwable throwable) {
            // Fail closed for world protection.
            this.destroyBlocksTick = 0;
        }
    }
}
