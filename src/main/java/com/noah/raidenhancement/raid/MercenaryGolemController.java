package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.compat.MobEffectCompat;
import com.noah.raidenhancement.config.BattleSupportConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Temporary player-following iron golems summoned by the mercenary battle token.
 *
 * 0.8.9.7.7 notes:
 * - Mercenary effects are applied once on spawn and repaired only when missing.
 * - Records are persisted through entity scoreboard tags, so world reloads can recover remaining lifetime.
 * - Glow color can now be assigned per summoning player through vanilla scoreboard teams.
 * - Player damage protection is PvP-aware and owner-safe.
 */
public final class MercenaryGolemController {
    private static final String IRON_GOLEM_ID = "minecraft:iron_golem";
    private static final String[] RESISTANCE_NAMES = {"DAMAGE_RESISTANCE", "RESISTANCE"};
    private static final String[] STRENGTH_NAMES = {"DAMAGE_BOOST", "STRENGTH"};
    private static final String[] SPEED_NAMES = {"MOVEMENT_SPEED", "SPEED"};

    private static final String TAG_MARKER = "rep_mercenary";
    private static final String TAG_OWNER_PREFIX = "rep_merc_owner:";
    private static final String TAG_EXPIRE_PREFIX = "rep_merc_expire:";
    private static final String TAG_DIM_PREFIX = "rep_merc_dim:";
    private static final String TAG_COLOR_PREFIX = "rep_merc_color:";

    private static final Map<UUID, MercenaryRecord> MERCENARIES = new LinkedHashMap<>();
    private static boolean warnedSpawnFailure;
    private static boolean warnedTickFailure;
    private static boolean warnedRestoreFailure;
    private static boolean warnedGlowFailure;

    private MercenaryGolemController() {
    }

    public static Result useToken(ServerLevel level, Player player) {
        if (level == null || player == null) {
            return Result.failure("[村民防卫同盟] 雇佣兵契约无法执行：目标无效。");
        }
        if (!BattleSupportConfig.ENABLED || !BattleSupportConfig.MERCENARY_ENABLED) {
            return Result.failure("[村民防卫同盟] 雇佣兵战斗令牌当前未启用。");
        }
        if (BattleSupportConfig.MERCENARY_REQUIRE_ACTIVE_RAID
                && !BattleSupportController.hasNearbyActiveSession(level, player, BattleSupportConfig.USE_RADIUS)) {
            return Result.failure("[村民防卫同盟] 当前没有可响应的村庄战场，雇佣兵契约无法签发。");
        }
        long now = level.getGameTime();
        restoreLoadedMercenaries(level, now);
        String ownerColor = MercenaryColorManager.colorForOwner(level, player.getUUID());
        if (BattleSupportConfig.MERCENARY_GLOWING_ENABLED
                && BattleSupportConfig.MERCENARY_GLOWING_PER_PLAYER_COLOR
                && ownerColor == null) {
            return Result.failure("[村民防卫同盟] 雇佣兵识别色已分配完毕，暂时无法签发新的雇佣契约。");
        }
        int active = activeMercenaryCount(level, player.getUUID());
        if (active >= Math.max(1, BattleSupportConfig.MERCENARY_MAX_ACTIVE_PER_PLAYER)) {
            return Result.failure("[村民防卫同盟] 你的雇佣兵契约名额已满，无法继续征召。");
        }
        int allowed = Math.max(0, Math.max(1, BattleSupportConfig.MERCENARY_MAX_ACTIVE_PER_PLAYER) - active);
        int count = Math.min(Math.max(1, BattleSupportConfig.MERCENARY_SUMMON_COUNT), allowed);
        int summoned = 0;
        long expire = now + Math.max(20, BattleSupportConfig.MERCENARY_DURATION_TICKS);
        for (int i = 0; i < count; i++) {
            Entity golem = spawnMercenaryGolem(level, player, i, count, expire, ownerColor);
            if (golem == null) {
                continue;
            }
            MERCENARIES.put(golem.getUUID(), new MercenaryRecord(player.getUUID(), dimensionId(level), expire, now + Math.max(20, BattleSupportConfig.MERCENARY_EFFECTS_REPAIR_INTERVAL_TICKS), ownerColor));
            summoned++;
        }
        if (summoned <= 0) {
            return Result.failure("[村民防卫同盟] 雇佣兵战斗令牌响应失败，未能部署临时铁傀儡。");
        }
        return Result.success("[村民防卫同盟] 雇佣兵契约已签发，" + summoned + " 名临时铁傀儡进入作战序列。", summoned);
    }

    public static void tick(ServerLevel level) {
        if (level == null) {
            return;
        }
        try {
            long now = level.getGameTime();
            int scanInterval = Math.max(20, BattleSupportConfig.MERCENARY_PERSISTENCE_SCAN_INTERVAL_TICKS);
            if (BattleSupportConfig.MERCENARY_PERSISTENCE_ENABLED && now % scanInterval == 0L) {
                restoreLoadedMercenaries(level, now);
            }
            if (MERCENARIES.isEmpty()) {
                return;
            }
            if (now % Math.max(5, BattleSupportConfig.MERCENARY_TICK_INTERVAL_TICKS) != 0L) {
                return;
            }
            String dimension = dimensionId(level);
            Iterator<Map.Entry<UUID, MercenaryRecord>> iterator = MERCENARIES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, MercenaryRecord> entry = iterator.next();
                MercenaryRecord record = entry.getValue();
                if (record == null || !dimension.equals(record.dimensionId)) {
                    continue;
                }
                Entity golem = level.getEntity(entry.getKey());
                if (golem == null || !golem.isAlive()) {
                    if (golem != null) {
                        removeFromGlowTeam(level, golem);
                    }
                    UUID owner = record.ownerId;
                    iterator.remove();
                    maybeReleaseOwnerColor(owner);
                    continue;
                }
                if (now >= record.expireGameTime) {
                    UUID owner = record.ownerId;
                    discard(level, golem);
                    iterator.remove();
                    maybeReleaseOwnerColor(owner);
                    continue;
                }
                ensureMercenaryTags(golem, record.ownerId, record.expireGameTime, record.dimensionId, record.glowColor);
                ensureGlowing(level, golem, record.glowColor);
                if (BattleSupportConfig.MERCENARY_EFFECTS_ENABLED
                        && BattleSupportConfig.MERCENARY_EFFECTS_REPAIR_MISSING
                        && now >= record.nextEffectRepairGameTime) {
                    int remaining = remainingTicks(now, record.expireGameTime);
                    repairMissingMercenaryEffects(golem, remaining);
                    entry.setValue(record.withNextEffectRepair(now + Math.max(20, BattleSupportConfig.MERCENARY_EFFECTS_REPAIR_INTERVAL_TICKS)));
                }
                Player owner = findPlayer(level, record.ownerId);
                if (owner != null && owner.isAlive()) {
                    maintainFollow(level, golem, owner);
                }
            }
        } catch (Throwable throwable) {
            if (!warnedTickFailure) {
                warnedTickFailure = true;
                System.out.println("[Raid Enhancement Patch] Mercenary golem tick failed once and was suppressed: " + throwable);
            }
        }
    }

    public static boolean shouldCancelPlayerDamageToMercenaryGolem(Entity entity, DamageSource source) {
        if (entity == null || source == null || !isMercenaryEntity(entity)) {
            return false;
        }
        Player attacker = damagePlayer(source);
        if (attacker == null) {
            return false;
        }
        UUID owner = ownerFromRecordOrTags(entity);
        if (owner != null && owner.equals(attacker.getUUID()) && BattleSupportConfig.MERCENARY_DAMAGE_PROTECTION_OWNER_CANNOT_DAMAGE) {
            return true;
        }
        if (!BattleSupportConfig.MERCENARY_DAMAGE_PROTECTION_PVP_AWARE) {
            return true;
        }
        boolean pvpAllowed = isPvpAllowed(attacker);
        if (!pvpAllowed && BattleSupportConfig.MERCENARY_DAMAGE_PROTECTION_BLOCK_ALL_PLAYER_DAMAGE_WHEN_PVP_DISABLED) {
            return true;
        }
        return pvpAllowed && !BattleSupportConfig.MERCENARY_DAMAGE_PROTECTION_ALLOW_OTHER_PLAYERS_DAMAGE_WHEN_PVP_ENABLED;
    }

    /** Let the damage event decide owner/PvP-aware cancellation. */
    public static boolean shouldCancelPlayerAttackToMercenaryGolem(Entity entity) {
        return false;
    }

    private static int activeMercenaryCount(ServerLevel level, UUID ownerId) {
        if (level == null || ownerId == null) {
            return 0;
        }
        restoreLoadedMercenaries(level, level.getGameTime());
        int count = 0;
        String dimension = dimensionId(level);
        Iterator<Map.Entry<UUID, MercenaryRecord>> iterator = MERCENARIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MercenaryRecord> entry = iterator.next();
            MercenaryRecord record = entry.getValue();
            if (record == null) {
                iterator.remove();
                continue;
            }
            if (!ownerId.equals(record.ownerId)) {
                continue;
            }
            if (dimension.equals(record.dimensionId)) {
                Entity entity = level.getEntity(entry.getKey());
                if (entity != null && entity.isAlive()) {
                    count++;
                }
            }
        }
        return count;
    }


    private static void maybeReleaseOwnerColor(UUID ownerId) {
        if (ownerId == null || !BattleSupportConfig.MERCENARY_GLOWING_RELEASE_COLOR_WHEN_NO_ACTIVE_MERCENARIES) {
            return;
        }
        boolean stillActive = false;
        for (MercenaryRecord record : MERCENARIES.values()) {
            if (record != null && ownerId.equals(record.ownerId)) {
                stillActive = true;
                break;
            }
        }
        MercenaryColorManager.releaseOwnerColorIfConfigured(ownerId, stillActive);
    }

    private static Entity spawnMercenaryGolem(ServerLevel level, Player player, int index, int count, long expireGameTime, String ownerColor) {
        try {
            Object entity = createEntityFromId(IRON_GOLEM_ID, level);
            if (!(entity instanceof Entity golem)) {
                return null;
            }
            double angle = (Math.PI * 2.0D * index) / Math.max(1, count);
            double radius = 2.0D + (index % 2);
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY();
            double z = player.getZ() + Math.sin(angle) * radius;
            moveEntity(entity, x, y, z);
            invokeOptionalNoArg(entity, "setPersistenceRequired");
            repairLivingToFull(entity);
            boolean added = addFreshEntity(level, entity);
            if (!added) {
                return null;
            }
            repairLivingToFull(entity);
            ensureMercenaryTags(golem, player.getUUID(), expireGameTime, dimensionId(level), ownerColor);
            ensureGlowing(level, golem, ownerColor);
            if (BattleSupportConfig.MERCENARY_EFFECTS_ENABLED) {
                applyMercenaryEffects(golem, remainingTicks(level.getGameTime(), expireGameTime));
            }
            return golem;
        } catch (Throwable throwable) {
            if (!warnedSpawnFailure) {
                warnedSpawnFailure = true;
                System.out.println("[Raid Enhancement Patch] Mercenary iron golem spawn failed once: " + throwable);
            }
            return null;
        }
    }

    private static void maintainFollow(ServerLevel level, Entity golem, Player owner) {
        double distanceSquared = golem.distanceToSqr(owner);
        int teleportDistance = Math.max(8, BattleSupportConfig.MERCENARY_TELEPORT_DISTANCE);
        if (distanceSquared >= (double) teleportDistance * (double) teleportDistance) {
            tryTeleportNearOwner(level, golem, owner);
            return;
        }
        int followStart = Math.max(4, BattleSupportConfig.MERCENARY_FOLLOW_START_DISTANCE);
        if (distanceSquared >= (double) followStart * (double) followStart) {
            navigateTo(golem, owner.getX(), owner.getY(), owner.getZ(), BattleSupportConfig.MERCENARY_FOLLOW_SPEED);
        }
    }

    private static void tryTeleportNearOwner(ServerLevel level, Entity golem, Player owner) {
        double[][] offsets = {
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 1}, {-3, 1}, {3, -1}, {-3, -1}, {1, 3}, {-1, 3}, {1, -3}, {-1, -3}
        };
        for (double[] offset : offsets) {
            double x = owner.getX() + offset[0];
            double y = owner.getY();
            double z = owner.getZ() + offset[1];
            try {
                moveEntity(golem, x, y, z);
                return;
            } catch (Throwable ignored) {
                // Try next offset.
            }
        }
    }

    private static void navigateTo(Entity entity, double x, double y, double z, double speed) {
        if (entity == null) {
            return;
        }
        try {
            Method getNavigation = entity.getClass().getMethod("getNavigation");
            Object navigation = getNavigation.invoke(entity);
            if (navigation == null) {
                return;
            }
            for (Method method : navigation.getClass().getMethods()) {
                if (!method.getName().equals("moveTo") || method.getParameterCount() != 4) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if ((types[0] == double.class || types[0] == Double.TYPE)
                        && (types[1] == double.class || types[1] == Double.TYPE)
                        && (types[2] == double.class || types[2] == Double.TYPE)
                        && (types[3] == double.class || types[3] == Double.TYPE)) {
                    method.invoke(navigation, x, y, z, Math.max(0.1D, speed));
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Follow support is optional; teleport fallback handles large separations.
        }
    }

    private static void applyMercenaryEffects(Object entity, int durationTicks) {
        if (!BattleSupportConfig.MERCENARY_EFFECTS_ENABLED) {
            return;
        }
        int duration = Math.max(20, durationTicks);
        MobEffectCompat.addVisibleEffect(entity, RESISTANCE_NAMES, duration, 0);
        MobEffectCompat.addVisibleEffect(entity, STRENGTH_NAMES, duration, 0);
        MobEffectCompat.addVisibleEffect(entity, SPEED_NAMES, duration, 0);
    }

    private static void repairMissingMercenaryEffects(Object entity, int remainingTicks) {
        if (!BattleSupportConfig.MERCENARY_EFFECTS_ENABLED || !BattleSupportConfig.MERCENARY_EFFECTS_REPAIR_MISSING) {
            return;
        }
        int duration = Math.max(20, remainingTicks);
        if (!MobEffectCompat.hasEffect(entity, RESISTANCE_NAMES)) {
            MobEffectCompat.addVisibleEffect(entity, RESISTANCE_NAMES, duration, 0);
        }
        if (!MobEffectCompat.hasEffect(entity, STRENGTH_NAMES)) {
            MobEffectCompat.addVisibleEffect(entity, STRENGTH_NAMES, duration, 0);
        }
        if (!MobEffectCompat.hasEffect(entity, SPEED_NAMES)) {
            MobEffectCompat.addVisibleEffect(entity, SPEED_NAMES, duration, 0);
        }
    }

    private static int remainingTicks(long now, long expireGameTime) {
        long remaining = Math.max(20L, expireGameTime - now);
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    private static void ensureMercenaryTags(Entity entity, UUID ownerId, long expireGameTime, String dimensionId, String glowColor) {
        if (entity == null || !BattleSupportConfig.MERCENARY_PERSISTENCE_ENABLED) {
            return;
        }
        String normalizedDimension = safeTagValue(dimensionId);
        UUID currentOwner = readUuidTag(entity, TAG_OWNER_PREFIX);
        long currentExpire = readLongTag(entity, TAG_EXPIRE_PREFIX, Long.MIN_VALUE);
        String currentDimension = readStringTag(entity, TAG_DIM_PREFIX, null);
        String normalizedColor = MercenaryColorManager.colorForRestoredEntity(null, ownerId, glowColor);
        String currentColor = readStringTag(entity, TAG_COLOR_PREFIX, null);
        if (hasScoreboardTag(entity, TAG_MARKER)
                && ownerId != null
                && ownerId.equals(currentOwner)
                && currentExpire == expireGameTime
                && normalizedDimension.equals(currentDimension)
                && normalizedColor.equals(MercenaryColorManager.normalizeColor(currentColor, "GOLD"))) {
            return;
        }
        addScoreboardTag(entity, TAG_MARKER);
        removeTagsWithPrefix(entity, TAG_OWNER_PREFIX);
        removeTagsWithPrefix(entity, TAG_EXPIRE_PREFIX);
        removeTagsWithPrefix(entity, TAG_DIM_PREFIX);
        removeTagsWithPrefix(entity, TAG_COLOR_PREFIX);
        if (ownerId != null) {
            addScoreboardTag(entity, TAG_OWNER_PREFIX + ownerId);
        }
        addScoreboardTag(entity, TAG_EXPIRE_PREFIX + expireGameTime);
        addScoreboardTag(entity, TAG_DIM_PREFIX + normalizedDimension);
        addScoreboardTag(entity, TAG_COLOR_PREFIX + normalizedColor);
    }

    private static String safeTagValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static boolean isMercenaryEntity(Entity entity) {
        return entity != null && (MERCENARIES.containsKey(entity.getUUID()) || hasScoreboardTag(entity, TAG_MARKER));
    }

    private static UUID ownerFromRecordOrTags(Entity entity) {
        if (entity == null) {
            return null;
        }
        MercenaryRecord record = MERCENARIES.get(entity.getUUID());
        if (record != null) {
            return record.ownerId;
        }
        return readUuidTag(entity, TAG_OWNER_PREFIX);
    }

    private static void restoreLoadedMercenaries(ServerLevel level, long now) {
        if (level == null || !BattleSupportConfig.MERCENARY_PERSISTENCE_ENABLED) {
            return;
        }
        try {
            String dimension = dimensionId(level);
            for (Entity entity : findLoadedMercenaryEntities(level)) {
                if (entity == null || !entity.isAlive() || !hasScoreboardTag(entity, TAG_MARKER)) {
                    continue;
                }
                UUID ownerId = readUuidTag(entity, TAG_OWNER_PREFIX);
                long expire = readLongTag(entity, TAG_EXPIRE_PREFIX, now + Math.max(20, BattleSupportConfig.MERCENARY_DURATION_TICKS));
                String savedDimension = readStringTag(entity, TAG_DIM_PREFIX, dimension);
                String savedColor = readStringTag(entity, TAG_COLOR_PREFIX, null);
                String restoredColor = MercenaryColorManager.colorForRestoredEntity(level, ownerId, savedColor);
                if (ownerId == null) {
                    continue;
                }
                if (now >= expire) {
                    discard(level, entity);
                    MERCENARIES.remove(entity.getUUID());
                    continue;
                }
                MERCENARIES.putIfAbsent(entity.getUUID(), new MercenaryRecord(ownerId, savedDimension, expire, now + Math.max(20, BattleSupportConfig.MERCENARY_EFFECTS_REPAIR_INTERVAL_TICKS), restoredColor));
                ensureMercenaryTags(entity, ownerId, expire, savedDimension, restoredColor);
                ensureGlowing(level, entity, restoredColor);
            }
        } catch (Throwable throwable) {
            if (!warnedRestoreFailure) {
                warnedRestoreFailure = true;
                System.out.println("[Raid Enhancement Patch] Mercenary persistence restore failed once and was suppressed: " + throwable);
            }
        }
    }

    private static List<Entity> findLoadedMercenaryEntities(ServerLevel level) {
        List<Entity> result = new ArrayList<>();
        collectAllEntitiesIfAvailable(level, result);
        if (!result.isEmpty()) {
            return result;
        }
        collectEntitiesAroundPlayers(level, result);
        return result;
    }

    private static void collectAllEntitiesIfAvailable(ServerLevel level, List<Entity> result) {
        try {
            for (Method method : level.getClass().getMethods()) {
                if (!method.getName().equals("getAllEntities") || method.getParameterCount() != 0) {
                    continue;
                }
                Object all = method.invoke(level);
                if (all instanceof Iterable<?> iterable) {
                    for (Object object : iterable) {
                        if (object instanceof Entity entity && hasScoreboardTag(entity, TAG_MARKER)) {
                            result.add(entity);
                        }
                    }
                }
                return;
            }
        } catch (Throwable ignored) {
            // Fall back to player-radius scanning.
        }
    }

    private static void collectEntitiesAroundPlayers(ServerLevel level, List<Entity> result) {
        List<Player> players = players(level);
        if (players.isEmpty()) {
            return;
        }
        int radius = Math.max(32, BattleSupportConfig.MERCENARY_PERSISTENCE_SCAN_RADIUS);
        for (Player player : players) {
            try {
                Object aabb = createAabb(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                        player.getX() + radius, player.getY() + radius, player.getZ() + radius);
                if (aabb == null) {
                    continue;
                }
                Predicate<Object> predicate = object -> object instanceof Entity entity && hasScoreboardTag(entity, TAG_MARKER);
                for (Method method : level.getClass().getMethods()) {
                    if (!method.getName().equals("getEntitiesOfClass") || method.getParameterCount() != 3) {
                        continue;
                    }
                    Class<?>[] types = method.getParameterTypes();
                    if (types[0] != Class.class) {
                        continue;
                    }
                    Object entities = method.invoke(level, Entity.class, aabb, predicate);
                    if (entities instanceof Collection<?> collection) {
                        for (Object object : collection) {
                            if (object instanceof Entity entity && !result.contains(entity)) {
                                result.add(entity);
                            }
                        }
                    }
                    break;
                }
            } catch (Throwable ignored) {
                // Try next player.
            }
        }
    }

    private static Object createAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        try {
            Class<?> aabbClass = Class.forName("net.minecraft.world.phys.AABB");
            Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class, double.class, double.class, double.class);
            return constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Player> players(ServerLevel level) {
        List<Player> result = new ArrayList<>();
        if (level == null) {
            return result;
        }
        try {
            Method playersMethod = level.getClass().getMethod("players");
            Object value = playersMethod.invoke(level);
            if (value instanceof List<?> list) {
                for (Object object : list) {
                    if (object instanceof Player player) {
                        result.add(player);
                    }
                }
            }
        } catch (Throwable ignored) {
            // No players visible.
        }
        return result;
    }

    private static void ensureGlowing(ServerLevel level, Entity entity, String glowColor) {
        if (level == null || entity == null || !BattleSupportConfig.MERCENARY_GLOWING_ENABLED) {
            return;
        }
        try {
            setGlowingTag(entity, true);
            Object scoreboard = invokeNoArg(level, "getScoreboard");
            if (scoreboard == null) {
                return;
            }
            String normalizedColor = MercenaryColorManager.colorForRestoredEntity(level, ownerFromRecordOrTags(entity), glowColor);
            String teamName = BattleSupportConfig.MERCENARY_GLOWING_PER_PLAYER_COLOR
                    ? MercenaryColorManager.teamNameForColor(normalizedColor)
                    : safeTeamName(BattleSupportConfig.MERCENARY_GLOWING_TEAM_NAME);
            Object team = getOrCreatePlayerTeam(scoreboard, teamName);
            if (team == null) {
                return;
            }
            setTeamColor(team, normalizedColor);
            addEntityToTeam(scoreboard, entity, team);
        } catch (Throwable throwable) {
            if (!warnedGlowFailure) {
                warnedGlowFailure = true;
                System.out.println("[Raid Enhancement Patch] Mercenary orange/gold glow setup failed once and was suppressed: " + throwable);
            }
        }
    }

    private static String safeTeamName(String configured) {
        String name = configured == null || configured.isBlank() ? "rep_merc_gold" : configured.trim();
        if (name.length() > 16) {
            return name.substring(0, 16);
        }
        return name;
    }

    private static Object getOrCreatePlayerTeam(Object scoreboard, String teamName) {
        Object existing = null;
        try {
            Method getPlayerTeam = scoreboard.getClass().getMethod("getPlayerTeam", String.class);
            existing = getPlayerTeam.invoke(scoreboard, teamName);
        } catch (Throwable ignored) {
            // Try create below.
        }
        if (existing != null) {
            return existing;
        }
        try {
            Method addPlayerTeam = scoreboard.getClass().getMethod("addPlayerTeam", String.class);
            return addPlayerTeam.invoke(scoreboard, teamName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setTeamColor(Object team, String configuredColor) {
        if (team == null) {
            return;
        }
        try {
            Class<?> chatFormattingClass = Class.forName("net.minecraft.ChatFormatting");
            Object color = Enum.valueOf((Class<Enum>) chatFormattingClass.asSubclass(Enum.class), configuredColor == null ? "GOLD" : configuredColor.trim().toUpperCase(java.util.Locale.ROOT));
            Method setColor = team.getClass().getMethod("setColor", chatFormattingClass);
            setColor.invoke(team, color);
        } catch (Throwable ignored) {
            // Colorless glow still works; only orange/gold tint is optional.
        }
    }

    private static void addEntityToTeam(Object scoreboard, Entity entity, Object team) {
        try {
            String entry = entity.getStringUUID();
            for (Method method : scoreboard.getClass().getMethods()) {
                if (!method.getName().equals("addPlayerToTeam") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types[0] == String.class && types[1].isAssignableFrom(team.getClass())) {
                    method.invoke(scoreboard, entry, team);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Team color is optional.
        }
    }


    private static Object getPlayerTeam(Object scoreboard, String teamName) {
        if (scoreboard == null || teamName == null || teamName.isBlank()) {
            return null;
        }
        try {
            Method getPlayerTeam = scoreboard.getClass().getMethod("getPlayerTeam", String.class);
            return getPlayerTeam.invoke(scoreboard, teamName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void removeEntityFromTeam(Object scoreboard, String entry, Object team) {
        if (scoreboard == null || entry == null || team == null) {
            return;
        }
        try {
            for (Method method : scoreboard.getClass().getMethods()) {
                if (!method.getName().equals("removePlayerFromTeam") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types[0] == String.class && types[1].isAssignableFrom(team.getClass())) {
                    method.invoke(scoreboard, entry, team);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Optional cleanup.
        }
    }

    private static void removeEntityFromAnyTeam(Object scoreboard, String entry) {
        if (scoreboard == null || entry == null) {
            return;
        }
        try {
            for (Method method : scoreboard.getClass().getMethods()) {
                if (!method.getName().equals("removePlayerFromTeam") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types[0] == String.class) {
                    method.invoke(scoreboard, entry);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Optional cleanup.
        }
    }

    private static void removeFromGlowTeam(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return;
        }
        try {
            Object scoreboard = invokeNoArg(level, "getScoreboard");
            if (scoreboard == null) {
                return;
            }
            String entry = entity.getStringUUID();
            for (String teamName : MercenaryColorManager.allMercenaryTeamNames()) {
                Object team = getPlayerTeam(scoreboard, teamName);
                if (team != null) {
                    removeEntityFromTeam(scoreboard, entry, team);
                }
            }
            removeEntityFromAnyTeam(scoreboard, entry);
        } catch (Throwable ignored) {
            // Cleanup is optional.
        }
    }

    private static void setGlowingTag(Object entity, boolean glowing) {
        try {
            Method method = entity.getClass().getMethod("setGlowingTag", boolean.class);
            method.invoke(entity, glowing);
        } catch (Throwable ignored) {
            try {
                MobEffectCompat.addVisibleEffect(entity, MobEffectCompat.GLOWING_NAMES, 40, 0);
            } catch (Throwable ignoredAgain) {
                // Optional.
            }
        }
    }

    private static boolean addScoreboardTag(Entity entity, String tag) {
        try {
            Method method = entity.getClass().getMethod("addTag", String.class);
            Object result = method.invoke(entity, tag);
            return !(result instanceof Boolean bool) || bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean removeScoreboardTag(Entity entity, String tag) {
        try {
            Method method = entity.getClass().getMethod("removeTag", String.class);
            Object result = method.invoke(entity, tag);
            return !(result instanceof Boolean bool) || bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasScoreboardTag(Entity entity, String tag) {
        try {
            Object tags = invokeNoArg(entity, "getTags");
            return tags instanceof Set<?> set && set.contains(tag);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void removeTagsWithPrefix(Entity entity, String prefix) {
        try {
            Object tags = invokeNoArg(entity, "getTags");
            if (!(tags instanceof Set<?> set)) {
                return;
            }
            List<String> toRemove = new ArrayList<>();
            for (Object object : set) {
                if (object instanceof String tag && tag.startsWith(prefix)) {
                    toRemove.add(tag);
                }
            }
            for (String tag : toRemove) {
                removeScoreboardTag(entity, tag);
            }
        } catch (Throwable ignored) {
            // Optional.
        }
    }

    private static UUID readUuidTag(Entity entity, String prefix) {
        String value = readStringTag(entity, prefix, null);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static long readLongTag(Entity entity, String prefix, long fallback) {
        String value = readStringTag(entity, prefix, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String readStringTag(Entity entity, String prefix, String fallback) {
        try {
            Object tags = invokeNoArg(entity, "getTags");
            if (!(tags instanceof Set<?> set)) {
                return fallback;
            }
            for (Object object : set) {
                if (object instanceof String tag && tag.startsWith(prefix)) {
                    return tag.substring(prefix.length());
                }
            }
        } catch (Throwable ignored) {
            // Optional.
        }
        return fallback;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void repairLivingToFull(Object livingEntity) {
        try {
            float max = getFloatNoArg(livingEntity, "getMaxHealth", 20.0F);
            setHealth(livingEntity, Math.max(1.0F, max));
        } catch (Throwable ignored) {
            // Optional repair only.
        }
    }

    private static float getFloatNoArg(Object target, String methodName, float fallback) {
        if (target == null) {
            return fallback;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return fallback;
    }

    private static boolean setHealth(Object livingEntity, float health) {
        if (livingEntity == null) {
            return false;
        }
        for (Method method : livingEntity.getClass().getMethods()) {
            if (!method.getName().equals("setHealth") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> type = method.getParameterTypes()[0];
            try {
                method.setAccessible(true);
                if (type == float.class || type == Float.TYPE) {
                    method.invoke(livingEntity, health);
                    return true;
                }
                if (type == double.class || type == Double.TYPE) {
                    method.invoke(livingEntity, (double) health);
                    return true;
                }
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static Object createEntityFromId(String entityId, ServerLevel level) throws ReflectiveOperationException {
        Class<?> entityTypeClass = Class.forName("net.minecraft.world.entity.EntityType");
        Method byString = entityTypeClass.getMethod("byString", String.class);
        Object optional = byString.invoke(null, entityId);
        Object entityType = null;
        if (optional instanceof Optional<?> opt && opt.isPresent()) {
            entityType = opt.get();
        }
        return entityType == null ? null : createEntity(entityType, level);
    }

    private static Object createEntity(Object entityType, ServerLevel level) throws ReflectiveOperationException {
        for (Method method : entityType.getClass().getMethods()) {
            if (!method.getName().equals("create") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(level.getClass())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(entityType, level);
        }
        return null;
    }

    private static void moveEntity(Object entity, double x, double y, double z) throws ReflectiveOperationException {
        try {
            Method moveTo = entity.getClass().getMethod("moveTo", double.class, double.class, double.class, float.class, float.class);
            moveTo.setAccessible(true);
            moveTo.invoke(entity, x, y, z, 0.0F, 0.0F);
            return;
        } catch (NoSuchMethodException ignored) {
            // Try setPos below.
        }
        Method setPos = entity.getClass().getMethod("setPos", double.class, double.class, double.class);
        setPos.setAccessible(true);
        setPos.invoke(entity, x, y, z);
    }

    private static boolean addFreshEntity(ServerLevel level, Object entity) throws ReflectiveOperationException {
        for (Method method : level.getClass().getMethods()) {
            if (!method.getName().equals("addFreshEntity") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(entity.getClass())) {
                continue;
            }
            method.setAccessible(true);
            Object result = method.invoke(level, entity);
            return !(result instanceof Boolean bool) || bool;
        }
        return false;
    }

    private static void invokeOptionalNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional compatibility call.
        }
    }

    private static void discard(ServerLevel level, Object entity) {
        if (entity instanceof Entity golem) {
            removeFromGlowTeam(level, golem);
        }
        try {
            Method discard = entity.getClass().getMethod("discard");
            discard.setAccessible(true);
            discard.invoke(entity);
        } catch (Throwable ignored) {
            try {
                Method remove = entity.getClass().getMethod("remove", Class.forName("net.minecraft.world.entity.Entity$RemovalReason"));
                Object reason = Enum.valueOf((Class<Enum>) remove.getParameterTypes()[0].asSubclass(Enum.class), "DISCARDED");
                remove.invoke(entity, reason);
            } catch (Throwable ignoredAgain) {
                // Fail closed.
            }
        }
    }

    private static Player findPlayer(ServerLevel level, UUID ownerId) {
        if (level == null || ownerId == null) {
            return null;
        }
        for (Player player : players(level)) {
            if (ownerId.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    private static Player damagePlayer(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity causing = source.getEntity();
        if (causing instanceof Player player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player player) {
            return player;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static boolean isPvpAllowed(Player player) {
        if (player == null) {
            return false;
        }
        try {
            Object level = invokeNoArg(player, "level");
            if (level == null) {
                level = getField(player, "level");
            }
            Object server = invokeNoArg(level, "getServer");
            if (server == null) {
                return false;
            }
            Object result = invokeNoArg(server, "isPvpAllowed");
            return result instanceof Boolean bool && bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String dimensionId(ServerLevel level) {
        try {
            Method dimensionMethod = level.getClass().getMethod("dimension");
            Object dimension = dimensionMethod.invoke(level);
            Method location = dimension.getClass().getMethod("location");
            Object result = location.invoke(dimension);
            return String.valueOf(result);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public record Result(boolean success, String message, int summonedCount) {
        public static Result success(String message, int summonedCount) {
            return new Result(true, message, summonedCount);
        }

        public static Result failure(String message) {
            return new Result(false, message, 0);
        }
    }

    private record MercenaryRecord(UUID ownerId, String dimensionId, long expireGameTime, long nextEffectRepairGameTime, String glowColor) {
        private MercenaryRecord withNextEffectRepair(long next) {
            return new MercenaryRecord(ownerId, dimensionId, expireGameTime, next, glowColor);
        }
    }
}
