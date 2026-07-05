package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import com.noah.raidenhancement.villager.ProtectedVillagerState;
import com.noah.raidenhancement.villager.VillagerProtectionController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Step 4 compatibility-first wave expansion bridge.
 *
 * This intentionally avoids a hard Mixin into Raid for the first wave-expansion
 * stage. Instead, it finds active raids near loaded players and
 * protected villagers, then raises the raid's private numGroups field to the
 * configured target table. The controller never lowers numGroups, which avoids
 * fighting vanilla or other mods if they have already expanded a raid further.
 */
public final class RaidWaveExpansionController {
    private static final String RAID_NUM_GROUPS_FIELD = "numGroups";
    private static final String RAID_GROUPS_SPAWNED_FIELD = "groupsSpawned";
    private static final String RAID_OMEN_METHOD = "getRaidOmenLevel";
    private static final String GET_RAID_AT_METHOD = "getRaidAt";
    private static final String BLOCK_POSITION_METHOD = "blockPosition";
    private static final String LEVEL_DIFFICULTY_METHOD = "getDifficulty";

    private static final Map<Object, Integer> LAST_APPLIED_WAVES = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean warnedFailure;
    private static boolean warnedNoRaidAt;
    private static boolean warnedNoNumGroups;
    private static boolean warnedNativeWaveCap;

    private RaidWaveExpansionController() {
    }

    public static void tick(ServerLevel level) {
        if (!RaidEnhancementConfig.WAVE_EXPANSION_ENABLED || level == null) {
            return;
        }
        try {
            int touched = 0;
            Set<Object> visitedRaids = Collections.newSetFromMap(new IdentityHashMap<>());
            touched += applyNearPlayers(level, visitedRaids, RaidEnhancementConfig.WAVE_EXPANSION_MAX_RAIDS_PER_SWEEP - touched);
            if (touched < RaidEnhancementConfig.WAVE_EXPANSION_MAX_RAIDS_PER_SWEEP) {
                touched += applyNearProtectedVillagers(level, visitedRaids, RaidEnhancementConfig.WAVE_EXPANSION_MAX_RAIDS_PER_SWEEP - touched);
            }
        } catch (Throwable throwable) {
            if (!warnedFailure) {
                warnedFailure = true;
                System.out.println("[Raid Enhancement Patch] Raid wave expansion tick failed once and was suppressed: " + throwable);
            }
        }
    }

    private static int applyNearPlayers(ServerLevel level, Set<Object> visitedRaids, int remainingBudget) {
        if (remainingBudget <= 0) {
            return 0;
        }
        int applied = 0;
        for (Object playerObject : playersSnapshot(level)) {
            if (applied >= remainingBudget) {
                break;
            }
            if (!(playerObject instanceof Entity player) || player.level().isClientSide()) {
                continue;
            }
            Object basePos = invokeNoArg(player, BLOCK_POSITION_METHOD);
            applied += applyAroundPosition(level, basePos, visitedRaids, remainingBudget - applied);
        }
        return applied;
    }

    private static int applyNearProtectedVillagers(ServerLevel level, Set<Object> visitedRaids, int remainingBudget) {
        if (remainingBudget <= 0) {
            return 0;
        }
        int applied = 0;
        for (UUID uuid : VillagerProtectionController.protectedUuidsSnapshot()) {
            if (applied >= remainingBudget) {
                break;
            }
            Optional<ProtectedVillagerState> stateOptional = VillagerProtectionController.getState(uuid);
            if (stateOptional.isEmpty()) {
                continue;
            }
            ProtectedVillagerState state = stateOptional.get();
            if (!state.dimensionId().equals(level.dimension().location().toString())) {
                continue;
            }
            Entity entity = level.getEntity(uuid);
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            Object basePos = invokeNoArg(entity, BLOCK_POSITION_METHOD);
            applied += applyAroundPosition(level, basePos, visitedRaids, remainingBudget - applied);
        }
        return applied;
    }

    private static int applyAroundPosition(ServerLevel level, Object basePos, Set<Object> visitedRaids, int remainingBudget) {
        if (basePos == null || remainingBudget <= 0) {
            return 0;
        }
        int applied = 0;
        for (int[] offset : sampleOffsets()) {
            if (applied >= remainingBudget) {
                break;
            }
            Object pos = offset[0] == 0 && offset[1] == 0 && offset[2] == 0 ? basePos : offsetPosition(basePos, offset[0], offset[1], offset[2]);
            if (pos == null) {
                continue;
            }
            Object raid = getRaidAt(level, pos);
            if (raid == null || !visitedRaids.add(raid)) {
                continue;
            }
            if (applyWaveExpansion(level, raid)) {
                applied++;
            }
        }
        return applied;
    }

    private static boolean applyWaveExpansion(ServerLevel level, Object raid) {
        if (raid == null || isRaidFinished(raid)) {
            return false;
        }
        int rawOmenLevel = invokeInt(raid, RAID_OMEN_METHOD, 1);
        int omenLevel = normalizedOmenLevel(rawOmenLevel);
        String difficultyName = runtimeDifficultyName(level);
        int desiredByPlan = RaidWaveAuthority.targetTotalWaves(difficultyName, omenLevel);
        int desired = nativeRaidNumGroupsFieldForPlan(desiredByPlan, omenLevel);
        Field numGroupsField = findField(raid.getClass(), RAID_NUM_GROUPS_FIELD);
        if (numGroupsField == null) {
            if (!warnedNoNumGroups) {
                warnedNoNumGroups = true;
                System.out.println("[Raid Enhancement Patch] Raid wave expansion could not find Raid.numGroups; wave expansion will fail closed.");
            }
            return false;
        }
        try {
            numGroupsField.setAccessible(true);
            int current = numGroupsField.getInt(raid);
            int spawned = getGroupsSpawned(raid);
            int nativeSafeMax = nativeNumGroupsFieldSafeMax(omenLevel);

            // 0.4.1 repair path: if a raid was previously saved with an unsafe
            // wave count from 0.4.0, lower it before vanilla attempts to spawn
            // wave 9+. This is the only case where this controller intentionally
            // lowers numGroups; it prevents the ArrayIndexOutOfBounds crash in
            // Raid.getDefaultNumSpawns.
            if (RaidEnhancementConfig.WAVE_EXPANSION_CAP_NATIVE_RAID_WAVES && current > nativeSafeMax) {
                // 0.4.2: lower unsafe raids even if wavesSpawned is already above the cap.
                // Keeping max(nativeSafeMax, spawned) preserved the unsafe value 8 and still
                // allowed vanilla to call the spawn-count table with index 8.
                int repaired = nativeSafeMax;
                if (repaired < current) {
                    numGroupsField.setInt(raid, repaired);
                    LAST_APPLIED_WAVES.put(raid, repaired);
                    warnNativeWaveCap(current, repaired);
                    return true;
                }
                return false;
            }

            // 0.8.9.8.3: make the native Raid.numGroups follow the same
            // authoritative plan used by the HUD and custom bridge. Earlier builds only
            // raised numGroups; if vanilla/another mod exposed a higher safe native
            // count, the actual raid could run more waves than our HUD target. Lowering
            // is allowed only before the raid has spawned past the desired point.
            if (current > desired) {
                if (spawned <= desired) {
                    numGroupsField.setInt(raid, desired);
                    LAST_APPLIED_WAVES.put(raid, desired);
                    return true;
                }
                return false;
            }
            if (desired <= current) {
                return false;
            }
            // Do not make nonsensical changes to raids that have already spawned beyond the table.
            if (spawned > desired) {
                return false;
            }
            numGroupsField.setInt(raid, desired);
            LAST_APPLIED_WAVES.put(raid, desired);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warnedNoNumGroups) {
                warnedNoNumGroups = true;
                System.out.println("[Raid Enhancement Patch] Raid wave expansion failed to update Raid.numGroups and will fail closed: " + exception);
            }
            return false;
        }
    }

    private static int nativeRaidNumGroupsFieldForPlan(int desiredByPlan, int omenLevel) {
        int nativeLogicalWaves = RaidWaveAuthority.nativeWaveLimitForTarget(desiredByPlan);
        if (!RaidEnhancementConfig.WAVE_EXPANSION_CAP_NATIVE_RAID_WAVES) {
            return desiredByPlan;
        }

        // Vanilla Raid#numGroups is the number of regular groups. Ominous raids
        // may add their bonus wave after numGroups. Setting numGroups directly to
        // the desired logical total therefore makes vanilla call its spawn table
        // with index 8 on an 8-wave ominous raid, which crashes because the table
        // only has indexes 0..7. Keep the native logical contract as 1..8, but
        // write the vanilla field as regular groups only: target native waves minus
        // the expected ominous bonus wave.
        int vanillaRegularGroups = nativeLogicalWaves;
        if (hasExpectedVanillaBonusWave(omenLevel) && vanillaRegularGroups > 1) {
            vanillaRegularGroups -= 1;
        }
        vanillaRegularGroups = clamp(vanillaRegularGroups, 1, vanillaSpawnTableSafeMaxIndex());

        if (desiredByPlan > nativeLogicalWaves) {
            warnNativeWaveCap(desiredByPlan, nativeLogicalWaves);
        }
        return vanillaRegularGroups;
    }

    private static boolean hasExpectedVanillaBonusWave(int omenLevel) {
        // In vanilla 1.21.1 the extra ominous/bonus group is only observed from
        // raid omen level 2 upward. Treating level 1 as a bonus-wave raid made
        // Hard + Omen I write numGroups=7 while the HUD target remained 8,
        // producing the tested 7/8 mismatch. Keep Omen I at a full 8 regular
        // groups, and reserve the one-field subtraction for Omen II+.
        return omenLevel > 1;
    }

    private static int nativeNumGroupsFieldSafeMax(int omenLevel) {
        // Without the vanilla bonus group, numGroups=8 is safe: vanilla will use
        // spawn-table indexes 0..7. With the bonus group, numGroups must remain
        // at 7 so the bonus group becomes logical wave 8 instead of index 8.
        return hasExpectedVanillaBonusWave(omenLevel) ? 7 : 8;
    }

    private static int nativeSafeMaxWaves() {
        // Deprecated compatibility helper. Keep the old conservative value for
        // callers that do not know the omen level, but RaidWaveExpansionController
        // itself must use nativeNumGroupsFieldSafeMax(omenLevel).
        return 7;
    }

    private static int vanillaSpawnTableSafeMaxIndex() {
        return 8;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int normalizedOmenLevel(int rawOmenLevel) {
        int normalized = rawOmenLevel - Math.max(0, RaidEnhancementConfig.RAID_OMEN_RUNTIME_LEVEL_OFFSET);
        if (normalized < 1) {
            return 1;
        }
        if (normalized > 5) {
            return 5;
        }
        return normalized;
    }

    private static void warnNativeWaveCap(int requested, int applied) {
        if (!warnedNativeWaveCap) {
            warnedNativeWaveCap = true;
            System.out.println("[Raid Enhancement Patch] Native raid wave expansion was capped from " + requested
                    + " to " + applied
                    + " because vanilla 1.21.1 raid spawn tables only contain 8 wave entries. "
                    + "Waves beyond 8 require a future custom spawn layer.");
        }
    }

    private static int getGroupsSpawned(Object raid) {
        Field groupsSpawned = findField(raid.getClass(), RAID_GROUPS_SPAWNED_FIELD);
        if (groupsSpawned == null) {
            return 0;
        }
        try {
            groupsSpawned.setAccessible(true);
            return groupsSpawned.getInt(raid);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean isRaidFinished(Object raid) {
        return invokeBoolean(raid, "isStopped")
                || invokeBoolean(raid, "isVictory")
                || invokeBoolean(raid, "isLoss");
    }

    private static Object getRaidAt(ServerLevel level, Object blockPos) {
        if (blockPos == null) {
            return null;
        }
        try {
            Method getRaidAt = level.getClass().getMethod(GET_RAID_AT_METHOD, blockPos.getClass());
            return getRaidAt.invoke(level, blockPos);
        } catch (ReflectiveOperationException exception) {
            if (!warnedNoRaidAt) {
                warnedNoRaidAt = true;
                System.out.println("[Raid Enhancement Patch] ServerLevel.getRaidAt was not available for wave expansion; expansion will rely on future Mixin stages if needed.");
            }
            return null;
        }
    }

    private static Object offsetPosition(Object blockPos, int x, int y, int z) {
        if (blockPos == null) {
            return null;
        }
        try {
            Method offset = blockPos.getClass().getMethod("offset", int.class, int.class, int.class);
            return offset.invoke(blockPos, x, y, z);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static int[][] sampleOffsets() {
        int radius = Math.max(0, RaidEnhancementConfig.WAVE_EXPANSION_POSITION_SAMPLE_RADIUS);
        if (radius <= 0) {
            return new int[][]{{0, 0, 0}};
        }
        return new int[][]{
                {0, 0, 0},
                {radius, 0, 0}, {-radius, 0, 0}, {0, 0, radius}, {0, 0, -radius},
                {radius, 0, radius}, {radius, 0, -radius}, {-radius, 0, radius}, {-radius, 0, -radius}
        };
    }

    private static String runtimeDifficultyName(ServerLevel level) {
        Object difficulty = invokeNoArg(level, LEVEL_DIFFICULTY_METHOD);
        if (difficulty instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return difficulty == null ? "NORMAL" : difficulty.toString();
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static int invokeInt(Object target, String methodName, int fallback) {
        Object result = invokeNoArg(target, methodName);
        if (result instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static boolean invokeBoolean(Object target, String methodName) {
        Object result = invokeNoArg(target, methodName);
        return result instanceof Boolean bool && bool;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static List<?> playersSnapshot(ServerLevel level) {
        try {
            Method playersMethod = level.getClass().getMethod("players");
            Object result = playersMethod.invoke(level);
            if (result instanceof List<?> list) {
                return List.copyOf(list);
            }
        } catch (ReflectiveOperationException ignored) {
            // Fail closed.
        }
        return List.of();
    }
}
