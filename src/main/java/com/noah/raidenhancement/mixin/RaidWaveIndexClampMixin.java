package com.noah.raidenhancement.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Last-resort safety clamp for vanilla raid spawn tables.
 *
 * Minecraft 1.21.1 raid member spawn arrays contain indexes 0..7. If another
 * path leaves the raid with an unsafe wave value, this clamps the int argument
 * before Raid#getDefaultNumSpawns / getCount indexes the array.
 */
@Mixin(targets = "net.minecraft.world.entity.raid.Raid", priority = 900)
public abstract class RaidWaveIndexClampMixin {
    @ModifyVariable(method = "getDefaultNumSpawns", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int raid_enhancement_patch$clampMojmapDefaultSpawnWaveIndex(int wave) {
        return raid_enhancement_patch$clampRaidSpawnWaveIndex(wave);
    }

    @ModifyVariable(method = "getDefaultNumSpawns(Lnet/minecraft/world/entity/raid/Raid$RaiderType;IZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int raid_enhancement_patch$clampMojmapDefaultSpawnWaveIndexDescriptor(int wave) {
        return raid_enhancement_patch$clampRaidSpawnWaveIndex(wave);
    }

    @ModifyVariable(method = "getCount", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int raid_enhancement_patch$clampYarnNamedCountWaveIndex(int wave) {
        return raid_enhancement_patch$clampRaidSpawnWaveIndex(wave);
    }

    @ModifyVariable(method = "getBonusCount", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int raid_enhancement_patch$clampYarnNamedBonusWaveIndex(int wave) {
        return raid_enhancement_patch$clampRaidSpawnWaveIndex(wave);
    }


    /**
     * Stronger crash guard than the HEAD ModifyVariable hooks.  In the tested
     * NeoForge 1.21.1 runtime the method-level variable hook can be present in
     * the transformed Raid class but still not rewrite the value that is passed
     * from Raid#spawnGroup into Raid#getDefaultNumSpawns.  This hook clamps the
     * actual call argument at the invocation site, which is the path shown in
     * the crash reports: Raid#spawnGroup -> Raid#getDefaultNumSpawns(index=8).
     */
    @ModifyArg(
            method = "spawnGroup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/raid/Raid;getDefaultNumSpawns(Lnet/minecraft/world/entity/raid/Raid$RaiderType;IZ)I"
            ),
            index = 1,
            require = 0
    )
    private int raid_enhancement_patch$clampDefaultSpawnWaveArgFromSpawnGroup(int wave) {
        return raid_enhancement_patch$clampRaidSpawnWaveIndex(wave);
    }

    private static int raid_enhancement_patch$clampRaidSpawnWaveIndex(int wave) {
        if (wave < 0) {
            return 0;
        }
        return Math.min(wave, 7);
    }
}
