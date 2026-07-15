package com.noah.raidenhancement.event;

import com.noah.raidenhancement.compat.CachedReflection;
import com.noah.raidenhancement.raid.BattleSupportController;
import com.noah.raidenhancement.raid.MercenaryGolemController;
import com.noah.raidenhancement.raid.GolemBlockRollbackGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;


/** Game-bus hooks for item-driven battle support. Uses reflection for NeoForge 21.1.x event accessor compatibility. */
public final class BattleSupportEvents {
    private static boolean warnedAccessorFailure;

    @SubscribeEvent
    public void onPlayerAttackEntity(AttackEntityEvent event) {
        Object rawTarget = callNoArg(event, "getTarget");
        if (rawTarget instanceof Entity target
                && MercenaryGolemController.shouldCancelPlayerAttackToMercenaryGolem(target)) {
            cancelEvent(event);
        }
    }

    @SubscribeEvent
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        Object rawEntity = callNoArg(event, "getEntity");
        GolemBlockRollbackGuard.onPotentialGolemDamage(rawEntity);
        if (BattleSupportController.absorbShieldDamage(rawEntity, event)) {
            return;
        }
        Object rawSource = callNoArg(event, "getSource");
        if (rawEntity instanceof Entity entity
                && rawSource instanceof DamageSource source
                && MercenaryGolemController.shouldCancelPlayerDamageToMercenaryGolem(entity, source)) {
            cancelEvent(event);
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
                System.out.println("[Raid Enhancement Patch] BattleSupportEvents could not access event method " + methodName + " reflectively: " + throwable);
            }
            return null;
        }
    }

    private static void cancelEvent(Object event) {
        if (event == null) {
            return;
        }
        try {
            CachedReflection.invoke(event, "setCanceled", true);
        } catch (Throwable throwable) {
            if (!warnedAccessorFailure) {
                warnedAccessorFailure = true;
                System.out.println("[Raid Enhancement Patch] BattleSupportEvents could not cancel event reflectively: " + throwable);
            }
        }
    }
}
