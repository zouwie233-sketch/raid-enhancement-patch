package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.RaidsEnhancedCompatConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Last-resort anti-grief rollback for Raids Enhanced's Golem of Last Resort.
 *
 * This guard avoids world-wide scans. It snapshots only the small area around the golem when
 * that exact entity is hurt, restores changed non-air blocks for a short window, then creates
 * tiny short-lived cleanup zones only at block positions that were actually restored.
 */
public final class GolemBlockRollbackGuard {
    private static final String GOLEM_CLASS = "com.finderfeed.raids_enhanced.content.entities.golem_of_last_resort.GolemOfLastResort";
    private static final String ITEM_ENTITY_CLASS = "net.minecraft.world.entity.item.ItemEntity";
    private static final String AABB_CLASS = "net.minecraft.world.phys.AABB";

    private static final int MAX_ACTIVE_SNAPSHOTS = 64;
    private static final List<Snapshot> SNAPSHOTS = new ArrayList<>();
    private static final List<CleanupZone> CLEANUP_ZONES = new ArrayList<>();

    private static boolean warned;
    private static Constructor<?> blockPosConstructor;
    private static Constructor<?> aabbConstructor;
    private static Class<?> itemEntityClass;

    private GolemBlockRollbackGuard() {
    }

    public static void onPotentialGolemDamage(Object entity) {
        if (entity == null) {
            return;
        }
        if (!isGolemOfLastResort(entity)) {
            return;
        }
        if (RaidsEnhancedCompatConfig.golemOfLastResortBlockBreakingEnabled()) {
            return;
        }
        if (!RaidsEnhancedCompatConfig.golemRollbackGuardEnabled()) {
            return;
        }
        try {
            captureSnapshot(entity);
        } catch (Throwable throwable) {
            warnOnce("snapshot capture failed", throwable);
        }
    }

    public static void tick(Object serverLevel) {
        if (serverLevel == null) {
            return;
        }
        if (!SNAPSHOTS.isEmpty()) {
            Iterator<Snapshot> iterator = SNAPSHOTS.iterator();
            while (iterator.hasNext()) {
                Snapshot snapshot = iterator.next();
                if (snapshot.level != serverLevel) {
                    continue;
                }
                try {
                    restoreSnapshot(snapshot);
                } catch (Throwable throwable) {
                    warnOnce("snapshot restore failed", throwable);
                    iterator.remove();
                    continue;
                }
                snapshot.ticksRemaining--;
                if (snapshot.ticksRemaining <= 0) {
                    iterator.remove();
                }
            }
        }
        if (!CLEANUP_ZONES.isEmpty()) {
            try {
                tickCleanupZones(serverLevel);
            } catch (Throwable throwable) {
                warnOnce("drop cleanup tick failed", throwable);
            }
        }
    }

    private static void captureSnapshot(Object entity) throws ReflectiveOperationException {
        Object level = invoke(entity, "level");
        if (level == null || isClientSide(level)) {
            return;
        }

        int x = toInt(invoke(entity, "getBlockX"), floorDouble(invoke(entity, "getX")));
        int y = toInt(invoke(entity, "getBlockY"), floorDouble(invoke(entity, "getY")));
        int z = toInt(invoke(entity, "getBlockZ"), floorDouble(invoke(entity, "getZ")));

        int horizontal = RaidsEnhancedCompatConfig.golemRollbackHorizontalRadius();
        int down = RaidsEnhancedCompatConfig.golemRollbackDownRadius();
        int up = RaidsEnhancedCompatConfig.golemRollbackUpRadius();
        int maxBlocks = RaidsEnhancedCompatConfig.golemRollbackMaxBlocksPerSnapshot();

        Snapshot snapshot = new Snapshot(level, RaidsEnhancedCompatConfig.golemRollbackWindowTicks());
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
                    Object state = invoke(level, "getBlockState", pos);
                    if (state == null || isAirBlockState(state)) {
                        continue;
                    }
                    snapshot.entries.add(new Entry(pos, state, xx, yy, zz));
                }
            }
        }

        if (!snapshot.entries.isEmpty()) {
            SNAPSHOTS.add(snapshot);
            while (SNAPSHOTS.size() > MAX_ACTIVE_SNAPSHOTS) {
                SNAPSHOTS.remove(0);
            }
        }
    }

    private static Set<String> collectBaselineItemIds(Object level, int x, int y, int z, int horizontal, int down, int up) {
        Set<String> ids = new HashSet<>();
        if (!RaidsEnhancedCompatConfig.golemDropCleanupEnabled()) {
            return ids;
        }
        try {
            int extra = RaidsEnhancedCompatConfig.golemDropCleanupBaselineExtraRadius();
            double minX = x - horizontal - extra;
            double minY = y - down - 1;
            double minZ = z - horizontal - extra;
            double maxX = x + horizontal + extra + 1;
            double maxY = y + up + 2;
            double maxZ = z + horizontal + extra + 1;
            for (Object itemEntity : getItemEntities(level, minX, minY, minZ, maxX, maxY, maxZ)) {
                String id = entityIdentity(itemEntity);
                if (id != null) {
                    ids.add(id);
                }
            }
        } catch (Throwable throwable) {
            warnOnce("baseline item capture failed", throwable);
        }
        return ids;
    }

    private static void restoreSnapshot(Snapshot snapshot) throws ReflectiveOperationException {
        for (Entry entry : snapshot.entries) {
            Object current = invoke(snapshot.level, "getBlockState", entry.pos);
            if (current == null) {
                continue;
            }
            if (!Objects.equals(current, entry.state)) {
                setBlock(snapshot.level, entry.pos, entry.state);
                registerCleanupZone(snapshot, entry);
            }
        }
    }

    private static void registerCleanupZone(Snapshot snapshot, Entry entry) {
        if (!RaidsEnhancedCompatConfig.golemDropCleanupEnabled()) {
            return;
        }
        CleanupZone zone = new CleanupZone(
                snapshot.level,
                entry.x + 0.5D,
                entry.y + 0.5D,
                entry.z + 0.5D,
                RaidsEnhancedCompatConfig.golemDropCleanupRadius(),
                RaidsEnhancedCompatConfig.golemDropCleanupWindowTicks(),
                snapshot.baselineItemIds
        );
        CLEANUP_ZONES.add(zone);
        int maxZones = RaidsEnhancedCompatConfig.golemDropCleanupMaxZones();
        while (CLEANUP_ZONES.size() > maxZones) {
            CLEANUP_ZONES.remove(0);
        }

        // Run one immediate pass. In practice the block drop usually already exists when the
        // rollback detects air/replaced state and restores the block. Keeping the zone alive still
        // catches delayed drops or physics/merge movement in the next few ticks.
        try {
            cleanupZone(zone, Math.max(1, RaidsEnhancedCompatConfig.golemDropCleanupMaxItemsPerTick()));
        } catch (Throwable throwable) {
            warnOnce("immediate drop cleanup failed", throwable);
        }
    }

    private static void tickCleanupZones(Object level) throws ReflectiveOperationException {
        int maxItems = RaidsEnhancedCompatConfig.golemDropCleanupMaxItemsPerTick();
        int cleaned = 0;
        Iterator<CleanupZone> iterator = CLEANUP_ZONES.iterator();
        while (iterator.hasNext()) {
            CleanupZone zone = iterator.next();
            if (zone.level != level) {
                continue;
            }
            if (cleaned < maxItems) {
                cleaned += cleanupZone(zone, maxItems - cleaned);
            }
            zone.ticksRemaining--;
            if (zone.ticksRemaining <= 0) {
                iterator.remove();
            }
        }
    }

    private static int cleanupZone(CleanupZone zone, int remainingBudget) throws ReflectiveOperationException {
        if (remainingBudget <= 0) {
            return 0;
        }
        double radius = zone.radius;
        double radiusSq = radius * radius;
        int maxAge = RaidsEnhancedCompatConfig.golemDropCleanupMaxItemAgeTicks();
        int removed = 0;

        List<?> items = getItemEntities(zone.level,
                zone.x - radius, zone.y - radius, zone.z - radius,
                zone.x + radius, zone.y + radius, zone.z + radius);

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

    private static List<?> getItemEntities(Object level, double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
            throws ReflectiveOperationException {
        Object aabb = newAabb(minX, minY, minZ, maxX, maxY, maxZ);
        Class<?> itemClass = itemEntityClass();

        // Preferred vanilla/NeoForge path. On some 1.21.x runtime layouts this method is exposed
        // through inherited/public/interface methods, so findMethod(...) must search those too.
        try {
            Method method = findMethod(level.getClass(), "getEntitiesOfClass", new Object[]{itemClass, aabb});
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(level, itemClass, aabb);
                if (result instanceof List<?> list) {
                    return list;
                }
            }
        } catch (Throwable ignored) {
            // Try the three-argument overload and then the generic fallback below.
        }

        Predicate<Object> itemPredicate = object -> object != null && itemClass.isInstance(object);
        try {
            Method method = findMethod(level.getClass(), "getEntitiesOfClass", new Object[]{itemClass, aabb, itemPredicate});
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(level, itemClass, aabb, itemPredicate);
                if (result instanceof List<?> list) {
                    return list;
                }
            }
        } catch (Throwable ignored) {
            // Try generic getEntities fallback below.
        }

        // Generic fallback: Level#getEntities(Entity except, AABB area, Predicate). This catches
        // environments where getEntitiesOfClass cannot be reflected by descriptor, but the normal
        // entity query path is still available.
        Predicate<Object> predicate = object -> object != null && itemClass.isInstance(object);
        try {
            Method method = findMethod(level.getClass(), "getEntities", new Object[]{null, aabb, predicate});
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(level, null, aabb, predicate);
                if (result instanceof List<?> list) {
                    return list;
                }
            }
        } catch (Throwable ignored) {
            // Fall through to empty result.
        }

        return List.of();
    }

    private static void setBlock(Object level, Object pos, Object state) throws ReflectiveOperationException {
        try {
            Method method = level.getClass().getMethod("setBlock", pos.getClass(), state.getClass(), int.class);
            method.invoke(level, pos, state, 3);
            return;
        } catch (NoSuchMethodException ignored) {
            // Try the common superclass lookup below.
        }
        try {
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
            Method method = level.getClass().getMethod("setBlock", blockPosClass, blockStateClass, int.class);
            method.invoke(level, pos, state, 3);
            return;
        } catch (NoSuchMethodException ignored) {
            // Fall back to update method.
        }
        Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
        Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState");
        Method method = level.getClass().getMethod("setBlockAndUpdate", blockPosClass, blockStateClass);
        method.invoke(level, pos, state);
    }

    private static Object newBlockPos(int x, int y, int z) throws ReflectiveOperationException {
        if (blockPosConstructor == null) {
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);
        }
        return blockPosConstructor.newInstance(x, y, z);
    }

    private static Object newAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
            throws ReflectiveOperationException {
        if (aabbConstructor == null) {
            Class<?> aabbClass = Class.forName(AABB_CLASS);
            aabbConstructor = aabbClass.getConstructor(double.class, double.class, double.class, double.class, double.class, double.class);
        }
        return aabbConstructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Class<?> itemEntityClass() throws ReflectiveOperationException {
        if (itemEntityClass == null) {
            itemEntityClass = Class.forName(ITEM_ENTITY_CLASS);
        }
        return itemEntityClass;
    }

    private static boolean isAirBlockState(Object state) {
        try {
            Object value = invoke(state, "isAir");
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isClientSide(Object level) {
        try {
            Object value = level.getClass().getField("isClientSide").get(level);
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isGolemOfLastResort(Object entity) {
        return entity.getClass().getName().equals(GOLEM_CLASS);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, Object[] args) {
        if (type == null) {
            return null;
        }

        // Public search includes inherited methods and interface/default methods. This is important
        // for Level entity query methods in NeoForge/Minecraft 1.21.x.
        for (Method method : type.getMethods()) {
            if (methodMatches(method, methodName, args)) {
                return method;
            }
        }

        Method declared = findDeclaredMethod(type, methodName, args, new HashSet<>());
        if (declared != null) {
            return declared;
        }
        return null;
    }

    private static Method findDeclaredMethod(Class<?> type, String methodName, Object[] args, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (methodMatches(method, methodName, args)) {
                return method;
            }
        }
        for (Class<?> iface : type.getInterfaces()) {
            Method method = findDeclaredMethod(iface, methodName, args, visited);
            if (method != null) {
                return method;
            }
        }
        return findDeclaredMethod(type.getSuperclass(), methodName, args, visited);
    }

    private static boolean methodMatches(Method method, String methodName, Object[] args) {
        if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] != null && !wrap(parameterTypes[i]).isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static int floorDouble(Object value) {
        if (value instanceof Number number) {
            return (int) Math.floor(number.doubleValue());
        }
        return 0;
    }

    private static double entityCoordinate(Object entity, String methodName) {
        try {
            Object value = invoke(entity, methodName);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Throwable ignored) {
            // Fall back to origin; distance check will usually fail only for broken accessors.
        }
        return Double.NaN;
    }

    private static String entityIdentity(Object entity) {
        try {
            Object value = invoke(entity, "getUUID");
            if (value instanceof UUID uuid) {
                return uuid.toString();
            }
            if (value != null) {
                return value.toString();
            }
        } catch (Throwable ignored) {
            // UUID access failed; returning null means the entity is not protected as baseline.
        }
        return null;
    }

    private static int itemAge(Object itemEntity) {
        try {
            Object value = invoke(itemEntity, "getAge");
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
            // Field fallbacks below.
        }
        Integer directAge = intField(itemEntity, "age");
        if (directAge != null) {
            return directAge;
        }
        Integer tickCount = intField(itemEntity, "tickCount");
        return tickCount != null ? tickCount : -1;
    }

    private static Integer intField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
                // Try superclass.
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static void discardEntity(Object entity) {
        try {
            invoke(entity, "discard");
            return;
        } catch (Throwable ignored) {
            // Try remove(RemovalReason.DISCARDED) below.
        }
        try {
            Class<?> reasonClass = Class.forName("net.minecraft.world.entity.Entity$RemovalReason");
            Object discarded = null;
            for (Object constant : reasonClass.getEnumConstants()) {
                if (constant.toString().contains("DISCARD")) {
                    discarded = constant;
                    break;
                }
            }
            if (discarded != null) {
                invoke(entity, "remove", discarded);
                return;
            }
        } catch (Throwable ignored) {
            // Try kill below.
        }
        try {
            invoke(entity, "kill");
        } catch (Throwable throwable) {
            warnOnce("item discard failed", throwable);
        }
    }

    private static void warnOnce(String message, Throwable throwable) {
        if (!warned || RaidsEnhancedCompatConfig.debugLogsEnabled()) {
            warned = true;
            System.out.println("[Raid Enhancement Patch] Golem rollback/drop cleanup guard " + message + ": " + throwable);
        }
    }

    private static final class Snapshot {
        final Object level;
        final List<Entry> entries = new ArrayList<>();
        final Set<String> baselineItemIds = new HashSet<>();
        int ticksRemaining;

        Snapshot(Object level, int ticksRemaining) {
            this.level = level;
            this.ticksRemaining = ticksRemaining;
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

        CleanupZone(Object level, double x, double y, double z, double radius, int ticksRemaining, Set<String> baselineItemIds) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ticksRemaining = ticksRemaining;
            this.baselineItemIds = new HashSet<>(baselineItemIds);
        }
    }
}
