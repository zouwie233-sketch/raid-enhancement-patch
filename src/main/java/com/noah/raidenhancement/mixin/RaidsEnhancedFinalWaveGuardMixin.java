package com.noah.raidenhancement.mixin;

import com.noah.raidenhancement.raid.RaidWaveExpansionPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Prevents Raids Enhanced from treating the temporary vanilla-safe cap as the
 * real final wave.
 *
 * 0.4.7 design: vanilla Raid.numGroups is capped to a stable native value to
 * avoid Minecraft 1.21.1's eight-entry spawn-table crash. Raids Enhanced checks
 * wave == numGroups inside REMixinHandler.raidMixin(...), so a native cap would
 * otherwise spawn its final-wave special raider too early. This guard cancels
 * that call when the configured logical target wave is still later than the
 * current vanilla-safe wave. Future custom extra-wave logic can invoke or
 * reproduce special-raider spawning at the true final wave.
 */
@Mixin(targets = "com.finderfeed.raids_enhanced.REMixinHandler", priority = 700, remap = false)
public abstract class RaidsEnhancedFinalWaveGuardMixin {
    @Inject(method = "raidMixin", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void raid_enhancement_patch$cancelEarlyRaidsEnhancedFinalWave(
            Raid raid,
            BlockPos pos,
            int wave,
            int numGroups,
            CallbackInfo ci
    ) {
        try {
            int logicalTarget = raid_enhancement_patch$logicalTargetWave(raid);
            if (logicalTarget > numGroups && wave < logicalTarget) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Fail open: if compatibility reflection fails, avoid crashing the raid.
        }
    }

    private static int raid_enhancement_patch$logicalTargetWave(Object raid) {
        int omen = raid_enhancement_patch$invokeInt(raid, "getRaidOmenLevel", 1);
        Object level = raid_enhancement_patch$invokeNoArg(raid, "getLevel");
        String difficulty = raid_enhancement_patch$difficultyName(level);
        return RaidWaveExpansionPlan.forRuntimeDifficulty(difficulty, omen).totalWaves();
    }

    private static String raid_enhancement_patch$difficultyName(Object level) {
        Object difficulty = raid_enhancement_patch$invokeNoArg(level, "getDifficulty");
        return difficulty == null ? "NORMAL" : String.valueOf(difficulty).toUpperCase(java.util.Locale.ROOT);
    }

    private static int raid_enhancement_patch$invokeInt(Object target, String methodName, int fallback) {
        Object value = raid_enhancement_patch$invokeNoArg(target, methodName);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static Object raid_enhancement_patch$invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
