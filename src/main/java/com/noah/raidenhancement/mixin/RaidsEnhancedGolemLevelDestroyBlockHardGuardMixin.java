package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Final hard guard: if any path tries Level.destroyBlock(pos, drop, golem), block it.
 * This is event-local and only runs when blocks are actually being destroyed, not every tick.
 */
@Mixin(value = Level.class, priority = 650)
public abstract class RaidsEnhancedGolemLevelDestroyBlockHardGuardMixin {
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void raid_enhancement_patch$blockGolemLevelDestroyBlock(BlockPos pos, boolean dropBlock, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (entity != null
                    && isGolemOfLastResort(entity)
                    && !RaidsEnhancedCompatConfig.golemOfLastResortBlockBreakingEnabled()) {
                cir.setReturnValue(false);
            }
        } catch (Throwable throwable) {
            if (entity != null && isGolemOfLastResort(entity)) {
                cir.setReturnValue(false);
            }
        }
    }

    private static boolean isGolemOfLastResort(Object entity) {
        return entity.getClass().getName().equals("com.finderfeed.raids_enhanced.content.entities.golem_of_last_resort.GolemOfLastResort");
    }
}
