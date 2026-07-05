package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.raid.RaidExtraWaveController;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 0.4.19: keep vanilla Raid from broadcasting victory while custom extra waves
 * still need to be chained. This prevents wave 10/11 from spawning after the
 * raid has already entered its victory state.
 */
@Mixin(value = Raid.class, priority = 700)
public abstract class RaidVictorySuppressMixin {
    @Inject(method = "isVictory", at = @At("HEAD"), cancellable = true, require = 0)
    private void raid_enhancement_patch$suppressVictoryDuringExtraWaveChain(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (RaidExtraWaveController.shouldSuppressVictory(this)) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
            // Fail open: never crash a raid because of the compatibility guard.
        }
    }
}
