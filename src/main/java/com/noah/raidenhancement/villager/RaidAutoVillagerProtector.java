package com.noah.raidenhancement.villager;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.raid.RaidDetectionHelper;
import com.noah.raidenhancement.raid.RaidExtraWaveController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Stage 0.3.4 automatic raid villager protection bridge.
 *
 * This class does not modify Raid internals. It performs low-frequency scans
 * around loaded players, detects villagers inside active raid areas, protects
 * them, and releases auto-protected villagers when the raid area is no longer
 * active.
 */
public final class RaidAutoVillagerProtector {
    public static final String AUTO_SOURCE = "auto_raid_protection";

    private RaidAutoVillagerProtector() {
    }

    public static void tick(ServerLevel level) {
        if (!RaidEnhancementConfig.AUTO_RAID_VILLAGER_PROTECTION_ENABLED || level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime % RaidEnhancementConfig.AUTO_RAID_VILLAGER_SCAN_INTERVAL_TICKS == 0L) {
            protectRaidVillagersNearPlayers(level);
        }
        if (gameTime % RaidEnhancementConfig.AUTO_RAID_VILLAGER_RELEASE_INTERVAL_TICKS == 0L) {
            releaseFinishedRaidVillagers(level);
        }
    }

    private static int protectRaidVillagersNearPlayers(ServerLevel level) {
        int affected = 0;
        int scanned = 0;
        Set<UUID> visitedVillagers = new HashSet<>();
        for (Object playerObject : playersSnapshot(level)) {
            if (!(playerObject instanceof Entity center) || center.level().isClientSide()) {
                continue;
            }
            double radius = RaidEnhancementConfig.AUTO_RAID_VILLAGER_SCAN_RADIUS;
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, center.getBoundingBox().inflate(radius));
            for (Villager villager : villagers) {
                if (scanned >= RaidEnhancementConfig.AUTO_RAID_MAX_VILLAGERS_SCANNED_PER_SWEEP) {
                    return affected;
                }
                if (!villager.isAlive() || !visitedVillagers.add(villager.getUUID())) {
                    continue;
                }
                scanned++;
                if (!RaidDetectionHelper.isVillagerInsideActiveRaid(level, villager)) {
                    continue;
                }
                boolean newlyTracked = !VillagerProtectionController.isProtected(villager);
                boolean protectedNow = VillagerProtectionController.protect(villager,
                        RaidEnhancementConfig.AUTO_RAID_VILLAGER_PROTECTION_DURATION_TICKS,
                        AUTO_SOURCE);
                if (protectedNow && newlyTracked) {
                    affected++;
                }
            }
        }
        return affected;
    }

    private static int releaseFinishedRaidVillagers(ServerLevel level) {
        int released = 0;
        for (UUID uuid : VillagerProtectionController.protectedUuidsSnapshot()) {
            Optional<ProtectedVillagerState> stateOptional = VillagerProtectionController.getState(uuid);
            if (stateOptional.isEmpty()) {
                continue;
            }
            ProtectedVillagerState state = stateOptional.get();
            boolean managedProtectionSource = AUTO_SOURCE.equals(state.source())
                    || RaidEnhancementConfig.EXTRA_WAVE_VILLAGER_PROTECTION_SOURCE.equals(state.source());
            if (!managedProtectionSource) {
                continue;
            }
            if (!state.dimensionId().equals(level.dimension().location().toString())) {
                continue;
            }
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof Villager villager) || !villager.isAlive()) {
                VillagerProtectionController.unprotectUuid(uuid);
                released++;
                continue;
            }
            if (!RaidDetectionHelper.isVillagerInsideActiveRaid(level, villager)
                    && !RaidExtraWaveController.isExtraWaveThreatNear(level, villager)) {
                if (VillagerProtectionController.unprotect(villager, true)) {
                    released++;
                }
            }
        }
        return released;
    }

    private static List<?> playersSnapshot(ServerLevel level) {
        try {
            Method playersMethod = level.getClass().getMethod("players");
            Object result = playersMethod.invoke(level);
            if (result instanceof List<?> list) {
                return List.copyOf(list);
            }
        } catch (ReflectiveOperationException ignored) {
            // Fail closed. Later Mixin stages will provide stronger RaidSession hooks.
        }
        return List.of();
    }
}
