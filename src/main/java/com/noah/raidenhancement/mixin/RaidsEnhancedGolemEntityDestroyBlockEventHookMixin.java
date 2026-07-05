package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Third-layer guard: Raids Enhanced calls EventHooks.onEntityDestroyBlock(...) before
 * destroying blocks. Returning false here prevents the original destroyBlock call path
 * for the Golem of Last Resort even if the private-method guard is missed.
 */
@Mixin(targets = "net.neoforged.neoforge.event.EventHooks", priority = 650, remap = false)
public abstract class RaidsEnhancedGolemEntityDestroyBlockEventHookMixin {
    @Inject(method = "onEntityDestroyBlock", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void raid_enhancement_patch$blockGolemEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (entity != null
                    && isGolemOfLastResort(entity)
                    && !RaidsEnhancedCompatConfig.golemOfLastResortBlockBreakingEnabled()) {
                cir.setReturnValue(false);
            }
        } catch (Throwable throwable) {
            // Fail closed only for this exact entity class; avoid affecting unrelated mob griefing.
            if (entity != null && isGolemOfLastResort(entity)) {
                cir.setReturnValue(false);
            }
        }
    }

    private static boolean isGolemOfLastResort(Object entity) {
        return entity.getClass().getName().equals("com.finderfeed.raids_enhanced.content.entities.golem_of_last_resort.GolemOfLastResort");
    }
}
