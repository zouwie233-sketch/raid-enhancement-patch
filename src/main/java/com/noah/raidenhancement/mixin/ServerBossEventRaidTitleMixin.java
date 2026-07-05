package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.raid.RaidBossbarTitleOverride;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 0.8.9.8.8: rewrite the vanilla Raid bossbar title at ServerBossEvent#setName.
 *
 * This removes the visible title flicker caused by vanilla writing "Raid" and
 * the patch writing "Raid X/Y" on alternating server updates.
 */
@Mixin(value = ServerBossEvent.class, priority = 500)
public abstract class ServerBossEventRaidTitleMixin {
    @ModifyVariable(method = "setName", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Component raid_enhancement_patch$replaceManagedRaidTitle(Component original) {
        try {
            String title = RaidBossbarTitleOverride.titleForBossEvent(this);
            if (title != null && !title.isBlank()) {
                return Component.literal(title);
            }
        } catch (Throwable ignored) {
            // Display-only; never break bossbars.
        }
        return original;
    }
}
