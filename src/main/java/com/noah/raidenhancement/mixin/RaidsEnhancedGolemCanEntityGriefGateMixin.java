package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 0.8.9.7.17 extra gate: Raids Enhanced's GolemOfLastResort.destroyBlocks()
 * first asks EventHooks.canEntityGrief(level, this). If this returns false,
 * the original method exits before scanning nearby blocks and before calling
 * onEntityDestroyBlock()/Level.destroyBlock(). This is the cheapest and earliest
 * effective world-protection gate for that entity.
 */
@Mixin(targets = "net.neoforged.neoforge.event.EventHooks", priority = 600, remap = false)
public abstract class RaidsEnhancedGolemCanEntityGriefGateMixin {
    @Inject(method = "canEntityGrief", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void raid_enhancement_patch$denyGolemLastResortGrief(Level level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
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
