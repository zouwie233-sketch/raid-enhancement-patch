package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidEnhancementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Bounded, server-authoritative spawn-position validation for patch-owned raid entities.
 *
 * <p>The resolver is deliberately separate from wave planning. Wave composition and raid
 * progression decide <em>what</em> should spawn; this class only decides whether a concrete
 * entity can safely occupy a concrete location. Every candidate is checked against the
 * entity's real bounding box, loaded-chunk state, world border, fluids, common hazards and
 * ground support. Search work is capped both per entity and per server tick so a high-pressure
 * wave cannot replace the old suffocation bug with a new main-thread spike.</p>
 */
public final class SafeRaidSpawnResolver {
    private static final double EPSILON = 1.0E-7D;
    private static final Map<ServerLevel, TickBudget> TICK_BUDGETS = new WeakHashMap<>();
    private static final List<int[]> SEARCH_OFFSETS = buildSearchOffsets();

    private SafeRaidSpawnResolver() {
    }

    public static Resolution resolveGround(ServerLevel level, Entity entity, int requestedX, int requestedY, int requestedZ) {
        return resolve(level, entity, requestedX, requestedY, requestedZ, SpawnMode.GROUND);
    }

    public static Resolution resolveAir(ServerLevel level, Entity entity, int requestedX, int requestedY, int requestedZ) {
        return resolve(level, entity, requestedX, requestedY, requestedZ, SpawnMode.AIR);
    }

    private static Resolution resolve(ServerLevel level, Entity entity, int requestedX, int requestedY, int requestedZ,
                                      SpawnMode mode) {
        if (level == null || entity == null) {
            return Resolution.failure(Status.INVALID_INPUT, 0);
        }

        int perEntityLimit = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_MAX_CHECKS_PER_ENTITY);
        int horizontalRadius = Math.max(0, RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_SEARCH_RADIUS);
        TickBudget tickBudget = budgetFor(level);
        int checks = 0;

        for (int[] offset : SEARCH_OFFSETS) {
            if (checks >= perEntityLimit) {
                break;
            }
            if (Math.max(Math.abs(offset[0]), Math.abs(offset[1])) > horizontalRadius) {
                break;
            }

            int x = requestedX + offset[0];
            int z = requestedZ + offset[1];
            int probeY = clamp(requestedY, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
            BlockPos chunkProbe = new BlockPos(x, probeY, z);
            if (!level.hasChunkAt(chunkProbe) || !level.getWorldBorder().isWithinBounds(chunkProbe)) {
                continue;
            }

            int surfaceY = mode == SpawnMode.AIR
                    ? Math.max(requestedY, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                    + Math.max(6, RaidEnhancementConfig.EXTRA_WAVE_SAFE_AIR_CLEARANCE))
                    : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            for (int candidateY : candidateHeights(requestedY, surfaceY, mode)) {
                if (checks >= perEntityLimit) {
                    break;
                }
                if (!tickBudget.tryConsume(level.getGameTime())) {
                    return Resolution.failure(Status.TICK_BUDGET_EXHAUSTED, checks);
                }
                checks++;

                if (candidateY <= level.getMinBuildHeight() || candidateY >= level.getMaxBuildHeight() - 1) {
                    continue;
                }
                entity.moveTo(x + 0.5D, candidateY, z + 0.5D, entity.getYRot(), entity.getXRot());
                AABB box = entity.getBoundingBox();
                if (!withinBuildHeight(level, box)) {
                    continue;
                }
                if (!level.noCollision(entity, box)) {
                    continue;
                }
                if (mode == SpawnMode.GROUND && containsFluidOrHazard(level, box)) {
                    continue;
                }
                if (mode == SpawnMode.GROUND && !hasStableSupport(level, box)) {
                    continue;
                }
                return new Resolution(Status.SAFE, x + 0.5D, candidateY, z + 0.5D, checks);
            }
        }

        return Resolution.failure(Status.NO_SAFE_POSITION, checks);
    }

    private static TickBudget budgetFor(ServerLevel level) {
        TickBudget budget = TICK_BUDGETS.get(level);
        if (budget == null) {
            budget = new TickBudget();
            TICK_BUDGETS.put(level, budget);
        }
        return budget;
    }

    private static int[] candidateHeights(int requestedY, int surfaceY, SpawnMode mode) {
        Set<Integer> ordered = new java.util.LinkedHashSet<>();
        if (mode == SpawnMode.AIR) {
            ordered.add(surfaceY);
            ordered.add(surfaceY + 4);
            ordered.add(surfaceY + 8);
        } else {
            ordered.add(requestedY);
            ordered.add(surfaceY);
            int vertical = Math.max(0, RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_VERTICAL_PROBE);
            for (int delta = 1; delta <= vertical; delta++) {
                ordered.add(surfaceY + delta);
                ordered.add(surfaceY - delta);
                ordered.add(requestedY + delta);
                ordered.add(requestedY - delta);
            }
        }
        return ordered.stream().mapToInt(Integer::intValue).toArray();
    }

    private static boolean withinBuildHeight(ServerLevel level, AABB box) {
        return box.minY >= level.getMinBuildHeight() && box.maxY <= level.getMaxBuildHeight();
    }

    private static boolean containsFluidOrHazard(ServerLevel level, AABB box) {
        int minX = floor(box.minX + EPSILON);
        int maxX = floor(box.maxX - EPSILON);
        int minY = floor(box.minY + EPSILON);
        int maxY = floor(box.maxY - EPSILON);
        int minZ = floor(box.minZ + EPSILON);
        int maxZ = floor(box.maxZ - EPSILON);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getFluidState(pos).isEmpty()) {
                        return true;
                    }
                    if (isHazard(level.getBlockState(pos))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasStableSupport(ServerLevel level, AABB box) {
        int floorY = floor(box.minY - 0.01D);
        double minX = box.minX + Math.min(0.2D, Math.max(0.02D, box.getXsize() * 0.15D));
        double maxX = box.maxX - Math.min(0.2D, Math.max(0.02D, box.getXsize() * 0.15D));
        double minZ = box.minZ + Math.min(0.2D, Math.max(0.02D, box.getZsize() * 0.15D));
        double maxZ = box.maxZ - Math.min(0.2D, Math.max(0.02D, box.getZsize() * 0.15D));

        Set<BlockPos> samples = new HashSet<>();
        samples.add(new BlockPos(floor((box.minX + box.maxX) * 0.5D), floorY, floor((box.minZ + box.maxZ) * 0.5D)));
        samples.add(new BlockPos(floor(minX), floorY, floor(minZ)));
        samples.add(new BlockPos(floor(minX), floorY, floor(maxZ)));
        samples.add(new BlockPos(floor(maxX), floorY, floor(minZ)));
        samples.add(new BlockPos(floor(maxX), floorY, floor(maxZ)));

        int stable = 0;
        for (BlockPos pos : samples) {
            BlockState state = level.getBlockState(pos);
            if (isHazard(state) || !level.getFluidState(pos).isEmpty()) {
                continue;
            }
            if (state.isFaceSturdy(level, pos, Direction.UP)) {
                stable++;
            }
        }
        int required = Math.max(1, (samples.size() + 1) / 2);
        return stable >= required;
    }

    private static boolean isHazard(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.SWEET_BERRY_BUSH);
    }

    private static List<int[]> buildSearchOffsets() {
        int maxRadius = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_SEARCH_RADIUS);
        List<int[]> offsets = new ArrayList<>();
        offsets.add(new int[]{0, 0});
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                offsets.add(new int[]{x, -radius});
                offsets.add(new int[]{x, radius});
            }
            for (int z = -radius + 1; z <= radius - 1; z++) {
                offsets.add(new int[]{-radius, z});
                offsets.add(new int[]{radius, z});
            }
        }
        return List.copyOf(offsets);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Status {
        SAFE,
        NO_SAFE_POSITION,
        TICK_BUDGET_EXHAUSTED,
        INVALID_INPUT
    }

    public record Resolution(Status status, double x, double y, double z, int checks) {
        public boolean found() {
            return status == Status.SAFE;
        }

        private static Resolution failure(Status status, int checks) {
            return new Resolution(status, 0.0D, 0.0D, 0.0D, checks);
        }
    }

    private enum SpawnMode {
        GROUND,
        AIR
    }

    private static final class TickBudget {
        private long gameTime = Long.MIN_VALUE;
        private int remaining;

        private boolean tryConsume(long currentGameTime) {
            if (gameTime != currentGameTime) {
                gameTime = currentGameTime;
                remaining = Math.max(1, RaidEnhancementConfig.EXTRA_WAVE_SAFE_SPAWN_MAX_CHECKS_PER_TICK);
            }
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
