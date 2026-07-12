package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.compat.CachedReflection;
import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Last-resort anti-grief rollback for Raids Enhanced's Golem of Last Resort.
 *
 * <p>0.9.1.7 turns the old global tick list into per-level, per-golem work queues. Reflection
 * member discovery is cached, repeated damage refreshes one existing snapshot instead of creating
 * duplicates, block checks are budgeted, and a block is never restored while the triggering golem
 * still intersects that block. This keeps the compatibility fallback without allowing it to
 * monopolize the server thread or form a break/restore/retrap loop.</p>
 */
public final class GolemBlockRollbackGuard {
    private static final String GOLEM_CLASS = "com.finderfeed.raids_enhanced.content.entities.golem_of_last_resort.GolemOfLastResort";
    private static final String ITEM_ENTITY_CLASS = "net.minecraft.world.entity.item.ItemEntity";
    private static final String BLOCK_POS_CLASS = "net.minecraft.core.BlockPos";
    private static final String BLOCK_STATE_CLASS = "net.minecraft.world.level.block.state.BlockState";
    private static final String AABB_CLASS = "net.minecraft.world.phys.AABB";

    private static final int MAX_ACTIVE_SNAPSHOTS_PER_LEVEL = 64;
    private static final int MAX_BLOCK_CHECKS_PER_LEVEL_TICK = 256;
    private static final int MAX_BLOCK_CHECKS_PER_SNAPSHOT_VISIT = 64;
    private static final int MAX_COLLISION_DELAY_TICKS = 200;
    private static final int MAX_CLEANUP_ZONES_PER_LEVEL_TICK = 32;

    private static final Map<Object, LevelWork> WORK_BY_LEVEL = new IdentityHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private GolemBlockRollbackGuard() {
    }

    public static synchronized void onPotentialGolemDamage(Object entity) {
        if (entity == null || !isGolemOfLastResort(entity)) {
            return;
        }
        if (RaidsEnhancedCompatConfig.golemOfLastResortBlockBreakingEnabled()
                || !RaidsEnhancedCompatConfig.golemRollbackGuardEnabled()) {
            return;
        }
        try {
            captureSnapshot(entity);
        } catch (Throwable throwable) {
            warnOnce("snapshot-capture", "snapshot capture failed", throwable);
        }
    }

    public static synchronized void tick(Object serverLevel) {
        if (serverLevel == null) {
            return;
        }
        LevelWork work = WORK_BY_LEVEL.get(serverLevel);
        if (work == null || work.isEmpty()) {
            WORK_BY_LEVEL.remove(serverLevel);
            return;
        }

        advanceSnapshotTimers(work);
        processSnapshots(work);
        processCleanupZones(work);

        if (work.isEmpty()) {
            WORK_BY_LEVEL.remove(serverLevel);
        }
    }

    public static synchronized int activeSnapshotCount() {
        int total = 0;
        for (LevelWork work : WORK_BY_LEVEL.values()) {
            total += work.snapshotsByGolem.size();
        }
        return total;
    }

    public static String hotfixMarker() {
        return "golemRollbackQueue=per-level-per-golem reflectionDiscovery=cached"
                + " blockCheckBudget=" + MAX_BLOCK_CHECKS_PER_LEVEL_TICK
                + " collisionDelayTicks=" + MAX_COLLISION_DELAY_TICKS;
    }

    private static void captureSnapshot(Object entity) throws ReflectiveOperationException {
        Object level = CachedReflection.invoke(entity, "level");
        if (level == null || isClientSide(level)) {
            return;
        }

        String golemId = entityIdentity(entity);
        if (golemId == null) {
            return;
        }

        LevelWork work = WORK_BY_LEVEL.computeIfAbsent(level, LevelWork::new);
        Snapshot existing = work.snapshotsByGolem.get(golemId);
        if (existing != null) {
            existing.refresh(entity, RaidsEnhancedCompatConfig.golemRollbackWindowTicks());
            return;
        }

        int x = toInt(CachedReflection.invoke(entity, "getBlockX"), floorDouble(CachedReflection.invoke(entity, "getX")));
        int y = toInt(CachedReflection.invoke(entity, "getBlockY"), floorDouble(CachedReflection.invoke(entity, "getY")));
        int z = toInt(CachedReflection.invoke(entity, "getBlockZ"), floorDouble(CachedReflection.invoke(entity, "getZ")));

        int horizontal = RaidsEnhancedCompatConfig.golemRollbackHorizontalRadius();
        int down = RaidsEnhancedCompatConfig.golemRollbackDownRadius();
        int up = RaidsEnhancedCompatConfig.golemRollbackUpRadius();
        int maxBlocks = RaidsEnhancedCompatConfig.golemRollbackMaxBlocksPerSnapshot();

        Snapshot snapshot = new Snapshot(level, golemId, entity, RaidsEnhancedCompatConfig.golemRollbackWindowTicks());
        snapshot.baselineItemIds.addAll(collectBaselineItemIds(level, x, y, z, horizontal, down, up));

        outer:
        for (int yy = y - down; yy <= y + up; yy++) {
            for (int xx = x - horizontal; xx <= x + horizontal; xx++) {
                for (int zz = z - horizontal; zz <= z + horizontal; zz++) {
                    if (snapshot.entries.size() >= maxBlocks) {
                        break outer;
                    }
                    double dx = (xx + 0.5D) - (x + 0.5D);
                    double dz = (zz + 0.5D) - (z + 0.5D);
                    if ((dx * dx + dz * dz) > ((horizontal + 0.5D) * (horizontal + 0.5D))) {
                        continue;
                    }
                    Object pos = newBlockPos(xx, yy, zz);
                    Object state = CachedReflection.invoke(level, "getBlockState", pos);
                    if (state == null || isAirBlockState(state)) {
                        continue;
                    }
                    snapshot.entries.add(new Entry(pos, state, xx, yy, zz));
                }
            }
        }

        if (snapshot.entries.isEmpty()) {
            return;
        }
        work.snapshotsByGolem.put(golemId, snapshot);
        while (work.snapshotsByGolem.size() > MAX_ACTIVE_SNAPSHOTS_PER_LEVEL) {
            Iterator<String> iterator = work.snapshotsByGolem.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static void advanceSnapshotTimers(LevelWork work) {
        for (Snapshot snapshot : work.snapshotsByGolem.values()) {
            if (snapshot.observationTicksRemaining > 0) {
                snapshot.observationTicksRemaining--;
            } else {
                snapshot.collisionDelayTicks++;
            }
        }
    }

    private static void processSnapshots(LevelWork work) {
        int remainingBudget = MAX_BLOCK_CHECKS_PER_LEVEL_TICK;
        int snapshotsToVisit = work.snapshotsByGolem.size();
        while (remainingBudget > 0 && snapshotsToVisit-- > 0 && !work.snapshotsByGolem.isEmpty()) {
            Iterator<Map.Entry<String, Snapshot>> iterator = work.snapshotsByGolem.entrySet().iterator();
            Map.Entry<String, Snapshot> mapEntry = iterator.next();
            String golemId = mapEntry.getKey();
            Snapshot snapshot = mapEntry.getValue();
            iterator.remove();

            SnapshotProcessResult result;
            try {
                int visitBudget = Math.min(MAX_BLOCK_CHECKS_PER_SNAPSHOT_VISIT, remainingBudget);
                result = restoreSnapshotBudgeted(work, snapshot, visitBudget);
            } catch (Throwable throwable) {
                warnOnce("snapshot-restore", "snapshot restore failed", throwable);
                result = new SnapshotProcessResult(1, true);
            }
            remainingBudget -= Math.max(1, result.checkedBlocks());
            if (!result.removeSnapshot()) {
                work.snapshotsByGolem.put(golemId, snapshot);
            }
        }
    }

    private static SnapshotProcessResult restoreSnapshotBudgeted(LevelWork work, Snapshot snapshot, int budget)
            throws ReflectiveOperationException {
        if (snapshot.entries.isEmpty()) {
            return new SnapshotProcessResult(0, true);
        }

        GolemBounds bounds = currentGolemBounds(snapshot);
        int checked = 0;
        boolean completedCycle = false;
        while (checked < Math.max(1, budget)) {
            Entry entry = snapshot.entries.get(snapshot.scanCursor);
            snapshot.scanCursor++;
            checked++;

            Object current = CachedReflection.invoke(snapshot.level, "getBlockState", entry.pos);
            if (current != null && !Objects.equals(current, entry.state)) {
                snapshot.cycleSawDifference = true;
                if (bounds.blocksRestoration(entry)) {
                    snapshot.cycleSawCollisionDelay = true;
                    if (!snapshot.collisionDelayLogged && RaidsEnhancedCompatConfig.debugLogsEnabled()) {
                        snapshot.collisionDelayLogged = true;
                        System.out.println("[Raid Enhancement Patch] Golem rollback delayed a block restore because the golem still intersects the escape space. golem=" + snapshot.golemId);
                    }
                } else {
                    setBlock(snapshot.level, entry.pos, entry.state);
                    registerCleanupZone(work, snapshot, entry);
                }
            }

            if (snapshot.scanCursor >= snapshot.entries.size()) {
                snapshot.scanCursor = 0;
                completedCycle = true;
                break;
            }
        }

        if (!completedCycle) {
            return new SnapshotProcessResult(checked, false);
        }

        boolean remove = false;
        if (snapshot.observationTicksRemaining <= 0) {
            if (!snapshot.cycleSawDifference) {
                remove = true;
            } else if (snapshot.collisionDelayTicks >= MAX_COLLISION_DELAY_TICKS) {
                // Non-colliding differences have already been restored during this complete pass.
                // Any remaining differences are left untouched rather than sealing the golem back
                // into terrain after the bounded delay expires.
                remove = true;
                if (snapshot.cycleSawCollisionDelay && RaidsEnhancedCompatConfig.debugLogsEnabled()) {
                    System.out.println("[Raid Enhancement Patch] Golem rollback discarded collision-blocked restores after the bounded delay. golem=" + snapshot.golemId);
                }
            }
        }

        snapshot.cycleSawDifference = false;
        snapshot.cycleSawCollisionDelay = false;
        return new SnapshotProcessResult(checked, remove);
    }

    private static GolemBounds currentGolemBounds(Snapshot snapshot) {
        Object golem = snapshot.golemReference.get();
        if (golem == null) {
            return GolemBounds.absent();
        }
        try {
            Object currentLevel = CachedReflection.invoke(golem, "level");
            if (currentLevel != snapshot.level) {
                return GolemBounds.absent();
            }
        } catch (Throwable ignored) {
            // If the level accessor is unavailable, preserve the conservative collision policy below.
        }
        try {
            Object removed = CachedReflection.invoke(golem, "isRemoved");
            if (Boolean.TRUE.equals(removed)) {
                return GolemBounds.absent();
            }
        } catch (Throwable ignored) {
            // Older runtime shapes may not expose isRemoved; keep treating the reference as live.
        }
        try {
            Object aabb = CachedReflection.invoke(golem, "getBoundingBox");
            if (aabb == null) {
                return GolemBounds.presentUnknown();
            }
            return GolemBounds.known(
                    numberField(aabb, "minX"), numberField(aabb, "minY"), numberField(aabb, "minZ"),
                    numberField(aabb, "maxX"), numberField(aabb, "maxY"), numberField(aabb, "maxZ")
            );
        } catch (Throwable ignored) {
            return GolemBounds.presentUnknown();
        }
    }

    private static Set<String> collectBaselineItemIds(Object level, int x, int y, int z, int horizontal, int down, int up) {
        Set<String> ids = new HashSet<>();
        if (!RaidsEnhancedCompatConfig.golemDropCleanupEnabled()) {
            return ids;
        }
        try {
            int extra = RaidsEnhancedCompatConfig.golemDropCleanupBaselineExtraRadius();
            for (Object itemEntity : getItemEntities(level,
                    x - horizontal - extra, y - down - 1, z - horizontal - extra,
                    x + horizontal + extra + 1, y + up + 2, z + horizontal + extra + 1)) {
                String id = entityIdentity(itemEntity);
                if (id != null) {
                    ids.add(id);
                }
            }
        } catch (Throwable throwable) {
            warnOnce("baseline-item-capture", "baseline item capture failed", throwable);
        }
        return ids;
    }

    private static void registerCleanupZone(LevelWork work, Snapshot snapshot, Entry entry) {
        if (!RaidsEnhancedCompatConfig.golemDropCleanupEnabled()) {
            return;
        }
        BlockKey key = new BlockKey(entry.x, entry.y, entry.z);
        CleanupZone existing = work.cleanupZones.get(key);
        if (existing != null) {
            existing.ticksRemaining = Math.max(existing.ticksRemaining, RaidsEnhancedCompatConfig.golemDropCleanupWindowTicks());
            existing.baselineItemIds.addAll(snapshot.baselineItemIds);
            return;
        }
        work.cleanupZones.put(key, new CleanupZone(
                snapshot.level,
                entry.x + 0.5D,
                entry.y + 0.5D,
                entry.z + 0.5D,
                RaidsEnhancedCompatConfig.golemDropCleanupRadius(),
                RaidsEnhancedCompatConfig.golemDropCleanupWindowTicks(),
                snapshot.baselineItemIds
        ));
        int maxZones = RaidsEnhancedCompatConfig.golemDropCleanupMaxZones();
        while (work.cleanupZones.size() > maxZones) {
            Iterator<BlockKey> iterator = work.cleanupZones.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static void processCleanupZones(LevelWork work) {
        if (work.cleanupZones.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<BlockKey, CleanupZone>> expiryIterator = work.cleanupZones.entrySet().iterator();
        while (expiryIterator.hasNext()) {
            CleanupZone zone = expiryIterator.next().getValue();
            zone.ticksRemaining--;
            if (zone.ticksRemaining <= 0) {
                expiryIterator.remove();
            }
        }
        if (work.cleanupZones.isEmpty()) {
            return;
        }

        int remainingItemBudget = RaidsEnhancedCompatConfig.golemDropCleanupMaxItemsPerTick();
        int zonesToVisit = Math.min(MAX_CLEANUP_ZONES_PER_LEVEL_TICK, work.cleanupZones.size());
        while (zonesToVisit-- > 0 && remainingItemBudget > 0 && !work.cleanupZones.isEmpty()) {
            Iterator<Map.Entry<BlockKey, CleanupZone>> iterator = work.cleanupZones.entrySet().iterator();
            Map.Entry<BlockKey, CleanupZone> mapEntry = iterator.next();
            BlockKey key = mapEntry.getKey();
            CleanupZone zone = mapEntry.getValue();
            iterator.remove();
            try {
                remainingItemBudget -= cleanupZone(zone, remainingItemBudget);
            } catch (Throwable throwable) {
                warnOnce("drop-cleanup", "drop cleanup tick failed", throwable);
            }
            if (zone.ticksRemaining > 0) {
                work.cleanupZones.put(key, zone);
            }
        }
    }

    private static int cleanupZone(CleanupZone zone, int remainingBudget) throws ReflectiveOperationException {
        if (remainingBudget <= 0) {
            return 0;
        }
        double radiusSq = zone.radius * zone.radius;
        int maxAge = RaidsEnhancedCompatConfig.golemDropCleanupMaxItemAgeTicks();
        int removed = 0;
        List<?> items = getItemEntities(zone.level,
                zone.x - zone.radius, zone.y - zone.radius, zone.z - zone.radius,
                zone.x + zone.radius, zone.y + zone.radius, zone.z + zone.radius);

        for (Object itemEntity : items) {
            if (removed >= remainingBudget) {
                break;
            }
            String id = entityIdentity(itemEntity);
            if (id != null && zone.baselineItemIds.contains(id)) {
                continue;
            }
            if (maxAge > 0) {
                int age = itemAge(itemEntity);
                if (age >= 0 && age > maxAge) {
                    continue;
                }
            }
            double itemX = entityCoordinate(itemEntity, "getX");
            double itemY = entityCoordinate(itemEntity, "getY");
            double itemZ = entityCoordinate(itemEntity, "getZ");
            if (Double.isNaN(itemX) || Double.isNaN(itemY) || Double.isNaN(itemZ)) {
                continue;
            }
            double dx = itemX - zone.x;
            double dy = itemY - zone.y;
            double dz = itemZ - zone.z;
            if ((dx * dx + dy * dy + dz * dz) > radiusSq) {
                continue;
            }
            discardEntity(itemEntity);
            removed++;
        }
        return removed;
    }

    private static List<?> getItemEntities(Object level, double minX, double minY, double minZ,
                                           double maxX, double maxY, double maxZ)
            throws ReflectiveOperationException {
        Object aabb = newAabb(minX, minY, minZ, maxX, maxY, maxZ);
        Class<?> itemClass = CachedReflection.findClass(ITEM_ENTITY_CLASS);
        if (itemClass == null) {
            return List.of();
        }

        Method twoArgument = CachedReflection.findMethod(level.getClass(), "getEntitiesOfClass", itemClass, aabb);
        if (twoArgument != null) {
            try {
                Object result = twoArgument.invoke(level, itemClass, aabb);
                if (result instanceof List<?> list) {
                    return list;
                }
            } catch (Throwable ignored) {
                // Continue to cached fallback descriptors.
            }
        }

        Predicate<Object> itemPredicate = object -> object != null && itemClass.isInstance(object);
        Method threeArgument = CachedReflection.findMethod(level.getClass(), "getEntitiesOfClass", itemClass, aabb, itemPredicate);
        if (threeArgument != null) {
            try {
                Object result = threeArgument.invoke(level, itemClass, aabb, itemPredicate);
                if (result instanceof List<?> list) {
                    return list;
                }
            } catch (Throwable ignored) {
                // Continue to generic cached fallback.
            }
        }

        Method generic = CachedReflection.findMethod(level.getClass(), "getEntities", null, aabb, itemPredicate);
        if (generic != null) {
            try {
                Object result = generic.invoke(level, null, aabb, itemPredicate);
                if (result instanceof List<?> list) {
                    return list;
                }
            } catch (Throwable ignored) {
                // Return an empty list below.
            }
        }
        return List.of();
    }

    private static void setBlock(Object level, Object pos, Object state) throws ReflectiveOperationException {
        Method setBlock = CachedReflection.findMethod(level.getClass(), "setBlock", pos, state, 3);
        if (setBlock != null) {
            setBlock.invoke(level, pos, state, 3);
            return;
        }
        Method update = CachedReflection.findMethod(level.getClass(), "setBlockAndUpdate", pos, state);
        if (update == null) {
            throw new NoSuchMethodException(level.getClass().getName() + ".setBlock/setBlockAndUpdate");
        }
        update.invoke(level, pos, state);
    }

    private static Object newBlockPos(int x, int y, int z) throws ReflectiveOperationException {
        Class<?> blockPosClass = CachedReflection.findClass(BLOCK_POS_CLASS);
        if (blockPosClass == null) {
            throw new ClassNotFoundException(BLOCK_POS_CLASS);
        }
        return CachedReflection.construct(blockPosClass, x, y, z);
    }

    private static Object newAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
            throws ReflectiveOperationException {
        Class<?> aabbClass = CachedReflection.findClass(AABB_CLASS);
        if (aabbClass == null) {
            throw new ClassNotFoundException(AABB_CLASS);
        }
        return CachedReflection.construct(aabbClass, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean isAirBlockState(Object state) {
        try {
            return Boolean.TRUE.equals(CachedReflection.invoke(state, "isAir"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isClientSide(Object level) {
        try {
            return Boolean.TRUE.equals(CachedReflection.readField(level, "isClientSide"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isGolemOfLastResort(Object entity) {
        return entity.getClass().getName().equals(GOLEM_CLASS);
    }

    private static int toInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static int floorDouble(Object value) {
        return value instanceof Number number ? (int) Math.floor(number.doubleValue()) : 0;
    }

    private static double entityCoordinate(Object entity, String methodName) {
        try {
            Object value = CachedReflection.invoke(entity, methodName);
            return value instanceof Number number ? number.doubleValue() : Double.NaN;
        } catch (Throwable ignored) {
            return Double.NaN;
        }
    }

    private static String entityIdentity(Object entity) {
        try {
            Object value = CachedReflection.invoke(entity, "getUUID");
            if (value instanceof UUID uuid) {
                return uuid.toString();
            }
            return value == null ? null : value.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int itemAge(Object itemEntity) {
        try {
            Object value = CachedReflection.invoke(itemEntity, "getAge");
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
            // Field fallbacks below are also cached.
        }
        Integer directAge = intField(itemEntity, "age");
        if (directAge != null) {
            return directAge;
        }
        Integer tickCount = intField(itemEntity, "tickCount");
        return tickCount != null ? tickCount : -1;
    }

    private static Integer intField(Object target, String fieldName) {
        try {
            Field field = CachedReflection.findField(target.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            Object value = field.get(target);
            return value instanceof Number number ? number.intValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double numberField(Object target, String fieldName) throws ReflectiveOperationException {
        Object value = CachedReflection.readField(target, fieldName);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException("Non-numeric field " + target.getClass().getName() + "." + fieldName);
    }

    private static void discardEntity(Object entity) {
        try {
            CachedReflection.invoke(entity, "discard");
            return;
        } catch (Throwable ignored) {
            // Try remove(RemovalReason.DISCARDED) below.
        }
        try {
            Class<?> reasonClass = CachedReflection.findClass("net.minecraft.world.entity.Entity$RemovalReason");
            if (reasonClass != null) {
                for (Object constant : reasonClass.getEnumConstants()) {
                    if (constant.toString().contains("DISCARD")) {
                        CachedReflection.invoke(entity, "remove", constant);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Try kill below.
        }
        try {
            CachedReflection.invoke(entity, "kill");
        } catch (Throwable throwable) {
            warnOnce("item-discard", "item discard failed", throwable);
        }
    }

    private static void warnOnce(String key, String message, Throwable throwable) {
        if (WARNED.add(key) || RaidsEnhancedCompatConfig.debugLogsEnabled()) {
            System.out.println("[Raid Enhancement Patch] Golem rollback/drop cleanup guard " + message + ": " + throwable);
        }
    }

    private static final class LevelWork {
        final Object level;
        final LinkedHashMap<String, Snapshot> snapshotsByGolem = new LinkedHashMap<>();
        final LinkedHashMap<BlockKey, CleanupZone> cleanupZones = new LinkedHashMap<>();

        LevelWork(Object level) {
            this.level = level;
        }

        boolean isEmpty() {
            return snapshotsByGolem.isEmpty() && cleanupZones.isEmpty();
        }
    }

    private static final class Snapshot {
        final Object level;
        final String golemId;
        final List<Entry> entries = new ArrayList<>();
        final Set<String> baselineItemIds = new HashSet<>();
        WeakReference<Object> golemReference;
        int observationTicksRemaining;
        int collisionDelayTicks;
        int scanCursor;
        boolean cycleSawDifference;
        boolean cycleSawCollisionDelay;
        boolean collisionDelayLogged;

        Snapshot(Object level, String golemId, Object golem, int observationTicksRemaining) {
            this.level = level;
            this.golemId = golemId;
            this.golemReference = new WeakReference<>(golem);
            this.observationTicksRemaining = Math.max(1, observationTicksRemaining);
        }

        void refresh(Object golem, int windowTicks) {
            this.golemReference = new WeakReference<>(golem);
            this.observationTicksRemaining = Math.max(this.observationTicksRemaining, Math.max(1, windowTicks));
            this.collisionDelayTicks = 0;
        }
    }

    private static final class Entry {
        final Object pos;
        final Object state;
        final int x;
        final int y;
        final int z;

        Entry(Object pos, Object state, int x, int y, int z) {
            this.pos = pos;
            this.state = state;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class CleanupZone {
        final Object level;
        final double x;
        final double y;
        final double z;
        final double radius;
        final Set<String> baselineItemIds;
        int ticksRemaining;

        CleanupZone(Object level, double x, double y, double z, double radius, int ticksRemaining,
                    Set<String> baselineItemIds) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ticksRemaining = ticksRemaining;
            this.baselineItemIds = new HashSet<>(baselineItemIds);
        }
    }

    private record BlockKey(int x, int y, int z) {
    }

    private record SnapshotProcessResult(int checkedBlocks, boolean removeSnapshot) {
    }

    private record GolemBounds(boolean present, boolean known,
                               double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ) {
        static GolemBounds absent() {
            return new GolemBounds(false, true, 0, 0, 0, 0, 0, 0);
        }

        static GolemBounds presentUnknown() {
            return new GolemBounds(true, false, 0, 0, 0, 0, 0, 0);
        }

        static GolemBounds known(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ) {
            return new GolemBounds(true, true, minX, minY, minZ, maxX, maxY, maxZ);
        }

        boolean blocksRestoration(Entry entry) {
            if (!present) {
                return false;
            }
            if (!known) {
                return true;
            }
            return maxX > entry.x && minX < entry.x + 1.0D
                    && maxY > entry.y && minY < entry.y + 1.0D
                    && maxZ > entry.z && minZ < entry.z + 1.0D;
        }
    }
}
