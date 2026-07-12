package com.noah.raidenhancement.villager;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import net.minecraft.server.level.ServerLevel;
import com.noah.raidenhancement.compat.MobEffectCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime controller for protected villagers.
 *
 * Stage 0.3.0 intentionally stores protection state in memory. Future stages
 * will bind this to RaidSession and perform raid-end cleanup automatically.
 */
public final class VillagerProtectionController {
    private static final Map<UUID, ProtectedVillagerState> PROTECTED_VILLAGERS = new LinkedHashMap<>();
    public static final String VILLAGE_SECURITY_HEALTH_LOCK_SOURCE = "village_security_health_lock";
    private static boolean bootstrapped;
    private static boolean warnedHealthClampFailure;

    private VillagerProtectionController() {
    }

    public static synchronized void bootstrap() {
        if (!bootstrapped) {
            bootstrapped = true;
            System.out.println("[Raid Enhancement Patch] VillagerProtectionController bootstrapped.");
        }
    }

    public static synchronized boolean isProtected(UUID uuid) {
        return uuid != null && PROTECTED_VILLAGERS.containsKey(uuid);
    }

    public static synchronized boolean isProtected(Villager villager) {
        return villager != null && isProtected(villager.getUUID());
    }

    public static synchronized Optional<ProtectedVillagerState> getState(UUID uuid) {
        return Optional.ofNullable(PROTECTED_VILLAGERS.get(uuid));
    }

    public static synchronized int trackedCount() {
        return PROTECTED_VILLAGERS.size();
    }

    public static synchronized List<UUID> protectedUuidsSnapshot() {
        return new ArrayList<>(PROTECTED_VILLAGERS.keySet());
    }

    public static synchronized boolean protect(Villager villager, long durationTicks, String source) {
        if (!RaidEnhancementConfig.VILLAGER_PROTECTION_ENABLED || villager == null || villager.level().isClientSide()) {
            return false;
        }
        if (!(villager.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!PROTECTED_VILLAGERS.containsKey(villager.getUUID())
                && PROTECTED_VILLAGERS.size() >= RaidEnhancementConfig.MAX_PROTECTED_VILLAGERS_PER_RAID) {
            return false;
        }
        long now = serverLevel.getGameTime();
        long expires = now + Math.max(20L, durationTicks);
        String dimensionId = serverLevel.dimension().location().toString();
        ProtectedVillagerState existing = PROTECTED_VILLAGERS.get(villager.getUUID());
        boolean newlyProtected = existing == null || !existing.dimensionId().equals(dimensionId);
        ProtectedVillagerState state;
        if (!newlyProtected) {
            existing.extendUntil(expires);
            state = existing;
        } else {
            state = new ProtectedVillagerState(villager.getUUID(), dimensionId, now, expires, source);
            PROTECTED_VILLAGERS.put(villager.getUUID(), state);
        }
        if (RaidEnhancementConfig.PROTECTED_VILLAGER_EFFECTS_ENABLED
                && (newlyProtected || now - state.lastEffectRefreshGameTime()
                >= RaidEnhancementConfig.VILLAGER_PROTECTION_REFRESH_INTERVAL_TICKS)) {
            applyProtectionEffects(villager);
            state.markEffectRefreshed(now);
        }
        return true;
    }

    public static synchronized boolean unprotect(Villager villager, boolean removeEffects) {
        if (villager == null) {
            return false;
        }
        boolean removed = PROTECTED_VILLAGERS.remove(villager.getUUID()) != null;
        if (removed && removeEffects) {
            removeProtectionEffects(villager);
        }
        return removed;
    }

    public static synchronized boolean unprotectUuid(UUID uuid) {
        return uuid != null && PROTECTED_VILLAGERS.remove(uuid) != null;
    }


    public static synchronized boolean ensureVillageSecurityHealthClamp(Villager villager, long durationTicks) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED || villager == null || villager.level().isClientSide()) {
            return false;
        }
        if (!protect(villager, durationTicks, VILLAGE_SECURITY_HEALTH_LOCK_SOURCE)) {
            return false;
        }
        ProtectedVillagerState state = PROTECTED_VILLAGERS.get(villager.getUUID());
        if (state == null) {
            return false;
        }
        long gameTime = ((ServerLevel) villager.level()).getGameTime();
        float current = currentHealth(villager);
        state.enableHealthClamp(current);
        return maintainHealthClamp(villager, state, gameTime);
    }

    public static synchronized boolean enforceVillageSecurityHealthClamp(Villager villager) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED || villager == null || villager.level().isClientSide()) {
            return false;
        }
        ProtectedVillagerState state = PROTECTED_VILLAGERS.get(villager.getUUID());
        if (state == null || !state.hasHealthClamp() || !(villager.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return maintainHealthClamp(villager, state, serverLevel.getGameTime());
    }

    public static synchronized void updateVillageSecurityAllowedHealth(Villager villager) {
        if (!RaidEnhancementConfig.VILLAGE_SECURITY_VILLAGER_HEALTH_CLAMP_ENABLED || villager == null || villager.level().isClientSide()) {
            return;
        }
        ProtectedVillagerState state = PROTECTED_VILLAGERS.get(villager.getUUID());
        if (state != null && state.hasHealthClamp()) {
            state.updateAllowedHealth(currentHealth(villager));
        }
    }

    public static synchronized int releaseVillageSecurityHealthClamps(ServerLevel level, List<Villager> villagers, boolean removeSecurityOnlyProtection) {
        if (level == null || villagers == null || villagers.isEmpty()) {
            return 0;
        }
        int released = 0;
        for (Villager villager : villagers) {
            if (villager == null) {
                continue;
            }
            ProtectedVillagerState state = PROTECTED_VILLAGERS.get(villager.getUUID());
            if (state == null || !state.hasHealthClamp()) {
                continue;
            }
            state.disableHealthClamp();
            released++;
            if (removeSecurityOnlyProtection && VILLAGE_SECURITY_HEALTH_LOCK_SOURCE.equals(state.source())) {
                unprotect(villager, RaidEnhancementConfig.PROTECTED_VILLAGER_REMOVE_EFFECTS_ON_RELEASE);
            }
        }
        return released;
    }

    public static synchronized VillagerProtectionStats protectNearby(ServerLevel level, Entity center, double radius, long durationTicks, String source) {
        if (level == null || center == null) {
            return new VillagerProtectionStats(0, PROTECTED_VILLAGERS.size());
        }
        double clampedRadius = Math.max(1.0D, Math.min(radius, RaidEnhancementConfig.VILLAGER_PROTECTION_MAX_RADIUS));
        int affected = 0;
        for (Villager villager : level.getEntitiesOfClass(Villager.class, center.getBoundingBox().inflate(clampedRadius))) {
            if (protect(villager, durationTicks, source)) {
                affected++;
            }
        }
        return new VillagerProtectionStats(affected, PROTECTED_VILLAGERS.size());
    }

    public static synchronized VillagerProtectionStats unprotectNearby(ServerLevel level, Entity center, double radius, boolean removeEffects) {
        if (level == null || center == null) {
            return new VillagerProtectionStats(0, PROTECTED_VILLAGERS.size());
        }
        double clampedRadius = Math.max(1.0D, Math.min(radius, RaidEnhancementConfig.VILLAGER_PROTECTION_MAX_RADIUS));
        int affected = 0;
        for (Villager villager : level.getEntitiesOfClass(Villager.class, center.getBoundingBox().inflate(clampedRadius))) {
            if (unprotect(villager, removeEffects)) {
                affected++;
            }
        }
        return new VillagerProtectionStats(affected, PROTECTED_VILLAGERS.size());
    }

    public static synchronized void applyProtectionEffects(Villager villager) {
        if (!RaidEnhancementConfig.PROTECTED_VILLAGER_EFFECTS_ENABLED || villager == null || villager.level().isClientSide()) {
            return;
        }
        int duration = RaidEnhancementConfig.PROTECTED_VILLAGER_EFFECT_DURATION_TICKS;
        int refreshThreshold = Math.max(20, duration - RaidEnhancementConfig.VILLAGER_PROTECTION_REFRESH_INTERVAL_TICKS);
        MobEffectCompat.ensureEffect(villager, MobEffectCompat.RESISTANCE_NAMES, duration,
                RaidEnhancementConfig.PROTECTED_VILLAGER_RESISTANCE_AMPLIFIER, refreshThreshold);
        if (RaidEnhancementConfig.VILLAGE_SECURITY_ENABLED
                && RaidEnhancementConfig.VILLAGE_SECURITY_DISABLE_OLD_REGEN_ABSORPTION) {
            // Village-security phase 1 turns villager health into a raid resource.
            // Keep external damage blocked, but do not let the previous protection
            // regeneration/absorption layer erase security breach damage.
            MobEffectCompat.removeEffect(villager, MobEffectCompat.REGENERATION_NAMES);
            MobEffectCompat.removeEffect(villager, MobEffectCompat.ABSORPTION_NAMES);
        } else {
            MobEffectCompat.ensureEffect(villager, MobEffectCompat.REGENERATION_NAMES, duration,
                    RaidEnhancementConfig.PROTECTED_VILLAGER_REGENERATION_AMPLIFIER, refreshThreshold);
            MobEffectCompat.ensureEffect(villager, MobEffectCompat.ABSORPTION_NAMES, duration,
                    RaidEnhancementConfig.PROTECTED_VILLAGER_ABSORPTION_AMPLIFIER, refreshThreshold);
        }
        MobEffectCompat.ensureEffect(villager, MobEffectCompat.FIRE_RESISTANCE_NAMES, duration, 0, refreshThreshold);
    }

    public static synchronized void removeProtectionEffects(Villager villager) {
        if (!RaidEnhancementConfig.PROTECTED_VILLAGER_EFFECTS_ENABLED || villager == null || villager.level().isClientSide()) {
            return;
        }
        MobEffectCompat.removeEffect(villager, MobEffectCompat.RESISTANCE_NAMES);
        MobEffectCompat.removeEffect(villager, MobEffectCompat.REGENERATION_NAMES);
        MobEffectCompat.removeEffect(villager, MobEffectCompat.ABSORPTION_NAMES);
        MobEffectCompat.removeEffect(villager, MobEffectCompat.FIRE_RESISTANCE_NAMES);
    }

    public static synchronized int maintainLevel(ServerLevel level) {
        if (level == null) {
            return 0;
        }
        long now = level.getGameTime();
        int touched = 0;
        List<UUID> toRemove = new ArrayList<>();
        for (ProtectedVillagerState state : PROTECTED_VILLAGERS.values()) {
            if (!state.dimensionId().equals(level.dimension().location().toString())) {
                continue;
            }
            Entity entity = level.getEntity(state.villagerUuid());
            if (!(entity instanceof Villager villager) || state.isExpired(now) || !villager.isAlive()) {
                toRemove.add(state.villagerUuid());
                if (entity instanceof Villager removedVillager) {
                    if (RaidEnhancementConfig.PROTECTED_VILLAGER_REMOVE_EFFECTS_ON_RELEASE) {
                        removeProtectionEffects(removedVillager);
                    }
                }
                continue;
            }
            if (state.hasHealthClamp() && maintainHealthClamp(villager, state, now)) {
                touched++;
            }
            if (RaidEnhancementConfig.PROTECTED_VILLAGER_EFFECTS_ENABLED
                    && now - state.lastEffectRefreshGameTime() >= RaidEnhancementConfig.VILLAGER_PROTECTION_REFRESH_INTERVAL_TICKS) {
                applyProtectionEffects(villager);
                state.markEffectRefreshed(now);
                touched++;
            }
        }
        for (UUID uuid : toRemove) {
            PROTECTED_VILLAGERS.remove(uuid);
        }
        return touched;
    }

    public static synchronized int clearAllInLevel(ServerLevel level, boolean removeEffects) {
        if (level == null) {
            return 0;
        }
        int affected = 0;
        List<UUID> toRemove = new ArrayList<>();
        String dimensionId = level.dimension().location().toString();
        for (ProtectedVillagerState state : PROTECTED_VILLAGERS.values()) {
            if (!dimensionId.equals(state.dimensionId())) {
                continue;
            }
            Entity entity = level.getEntity(state.villagerUuid());
            if (removeEffects && entity instanceof Villager villager) {
                removeProtectionEffects(villager);
            }
            toRemove.add(state.villagerUuid());
            affected++;
        }
        for (UUID uuid : toRemove) {
            PROTECTED_VILLAGERS.remove(uuid);
        }
        return affected;
    }
    private static boolean enforceHealthClamp(Villager villager, ProtectedVillagerState state) {
        if (villager == null || state == null || !state.hasHealthClamp()) {
            return false;
        }
        try {
            float current = currentHealth(villager);
            float max = maxHealth(villager);
            float allowed = Math.max(0.0F, Math.min(state.allowedHealth(), Math.max(1.0F, max)));
            if (current > allowed + 0.01F) {
                villager.setHealth(allowed);
                return true;
            }
        } catch (Throwable throwable) {
            if (!warnedHealthClampFailure) {
                warnedHealthClampFailure = true;
                System.out.println("[Raid Enhancement Patch] Villager health clamp failed once and was suppressed: " + throwable);
            }
        }
        return false;
    }

    private static float currentHealth(Villager villager) {
        try {
            return villager.getHealth();
        } catch (Throwable ignored) {
            return 20.0F;
        }
    }

    private static float maxHealth(Villager villager) {
        try {
            return villager.getMaxHealth();
        } catch (Throwable ignored) {
            return 20.0F;
        }
    }

    private static void removeForbiddenHealingEffects(Villager villager) {
        if (villager == null || villager.level().isClientSide()) {
            return;
        }
        MobEffectCompat.removeEffect(villager, MobEffectCompat.REGENERATION_NAMES);
        MobEffectCompat.removeEffect(villager, MobEffectCompat.ABSORPTION_NAMES);
        MobEffectCompat.removeEffect(villager, MobEffectCompat.HEALTH_BOOST_NAMES);
    }

    private static boolean maintainHealthClamp(Villager villager, ProtectedVillagerState state, long gameTime) {
        if (villager == null || state == null || !state.hasHealthClamp()) {
            return false;
        }
        if (state.healthClampMaintainedAt(gameTime)) {
            return false;
        }
        state.markHealthClampMaintained(gameTime);
        removeForbiddenHealingEffects(villager);
        return enforceHealthClamp(villager, state);
    }

}
