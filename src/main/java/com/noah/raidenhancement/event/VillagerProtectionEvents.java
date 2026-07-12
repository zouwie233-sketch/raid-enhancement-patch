package com.noah.raidenhancement.event;

import com.noah.raidenhancement.compat.CachedReflection;
import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import com.noah.raidenhancement.raid.VillageSecurityController;
import com.noah.raidenhancement.villager.RaidAutoVillagerProtector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;


/** Game-bus listeners for stage 0.3.9 villager protection regression-fix. */
public final class VillagerProtectionEvents {
    private static boolean warnedTickFailure;
    private static boolean warnedAccessorFailure;

    @SubscribeEvent
    public void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!RaidEnhancementConfig.VILLAGER_PROTECTION_ENABLED) {
            return;
        }
        if (event.getTarget() instanceof Villager villager && VillagerProtectionController.isProtected(villager)) {
            event.setCanceled(true);
            // Compatibility hotfix 0.3.7: do not send an actionbar/chat component here.
            // The staged sandbox build previously baked an incompatible Component.translatable
            // invocation descriptor, which crashed when the player attacked a protected villager.
            // Cancelling the attack is enough for the protection mechanic; user-facing messages
            // can be restored later only from a real NeoForge Gradle build.
            return;
        }
        if (event.getTarget() instanceof Entity target
                && VillageSecurityController.shouldCancelPlayerDamageToVillageGolem(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!RaidEnhancementConfig.VILLAGER_PROTECTION_ENABLED) {
            return;
        }
        if (VillageSecurityController.shouldCancelPlayerDamageToVillageGolem(event.getEntity())
                && isDamageFromPlayer(event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (!VillagerProtectionController.isProtected(villager)) {
            return;
        }
        if (RaidEnhancementConfig.PROTECTED_VILLAGERS_CANCEL_ALL_DAMAGE || isDamageFromPlayer(event.getSource())) {
            event.setCanceled(true);
            VillagerProtectionController.applyProtectionEffects(villager);
        }
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        tickLevelPost(callNoArg(event, "getLevel"));
    }

    private void tickLevelPost(Object levelObject) {
        if (!RaidEnhancementConfig.VILLAGER_PROTECTION_ENABLED) {
            return;
        }
        if (!(levelObject instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            long gameTime = serverLevel.getGameTime();
            if (gameTime % RaidEnhancementConfig.VILLAGER_PROTECTION_SWEEP_INTERVAL_TICKS == 0L) {
                VillagerProtectionController.maintainLevel(serverLevel);
            }
            RaidAutoVillagerProtector.tick(serverLevel);
        } catch (Throwable throwable) {
            if (!warnedTickFailure) {
                warnedTickFailure = true;
                System.out.println("[Raid Enhancement Patch] Villager protection tick failed once and was suppressed: " + throwable);
            }
        }
    }

    private static Object callNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }
        try {
            return CachedReflection.invoke(target, methodName);
        } catch (Throwable throwable) {
            if (!warnedAccessorFailure) {
                warnedAccessorFailure = true;
                System.out.println("[Raid Enhancement Patch] VillagerProtectionEvents could not access event method " + methodName + " reflectively: " + throwable);
            }
            return null;
        }
    }

    private static boolean isDamageFromPlayer(DamageSource source) {
        if (source == null) {
            return false;
        }
        Entity causing = source.getEntity();
        if (causing instanceof Player) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player) {
            return true;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player) {
            return true;
        }
        return false;
    }
}
