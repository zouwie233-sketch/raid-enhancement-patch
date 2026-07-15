package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.compat.MobEffectCompat;
import com.noah.raidenhancement.config.BattleSupportConfig;
import com.noah.raidenhancement.item.BattleSupportTokenItem;
import com.noah.raidenhancement.raid.runtime.VillageSecurityRuntimeView;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Applies item-driven battlefield support to the already-managed village-security golems.
 *
 * This class reads immutable VillageSecurityController runtime views without modifying
 * extra-wave, HUD or raid-state code paths. It only looks up active security golem UUIDs
 * and applies effects to those golems.
 */
public final class BattleSupportController {
    private static final String[] STRENGTH_NAMES = {"DAMAGE_BOOST", "STRENGTH"};
    private static final String[] SPEED_NAMES = {"MOVEMENT_SPEED", "SPEED"};
    private static final String[] FIRE_RESISTANCE_NAMES = {"FIRE_RESISTANCE"};
    private static final String[] REGENERATION_NAMES = {"REGENERATION"};
    private static final String[] ABSORPTION_NAMES = {"ABSORPTION"};
    private static final Map<UUID, ShieldRecord> SHIELD_RECORDS = new LinkedHashMap<>();
    private static final Map<String, InsightState> INSIGHT_STATES = new LinkedHashMap<>();
    private static boolean warnedAbsorptionAccess;

    private BattleSupportController() {
    }

    public static Result applyToken(ServerLevel level, Player player, BattleSupportTokenItem.Kind kind) {
        if (level == null || player == null || kind == null) {
            return Result.failure("[村民防卫同盟] 战备令无法送达：目标无效。");
        }
        if (!BattleSupportConfig.ENABLED) {
            return Result.failure("[村民防卫同盟] 战备令系统当前已关闭。");
        }
        if (!isKindEnabled(kind)) {
            return Result.failure("[村民防卫同盟] 此类战备令当前未被启用。");
        }
        ActiveSecuritySession session = findNearestSession(level, player, BattleSupportConfig.USE_RADIUS);
        if (session == null) {
            return Result.failure("[村民防卫同盟] 当前没有可响应的村庄战场。");
        }
        if (kind == BattleSupportTokenItem.Kind.BASIC_INSIGHT || kind == BattleSupportTokenItem.Kind.ADVANCED_INSIGHT) {
            return armInsightToken(level, session, kind);
        }
        if (kind == BattleSupportTokenItem.Kind.BASIC_RALLY_ENEMY || kind == BattleSupportTokenItem.Kind.ADVANCED_RALLY_ENEMY) {
            return useRallyEnemyToken(level, player, session, kind);
        }
        List<Entity> golems = session.aliveSecurityGolems(level);
        if (golems.isEmpty()) {
            return Result.failure("[村民防卫同盟] 战场内没有可接收支援的安防铁傀儡。");
        }
        int affected = 0;
        long now = level.getGameTime();
        for (Entity golem : golems) {
            if (golem == null || !golem.isAlive()) {
                continue;
            }
            applyKind(level, golem, kind, now);
            affected++;
        }
        if (affected <= 0) {
            return Result.failure("[村民防卫同盟] 战备令未找到有效安防铁傀儡目标。");
        }
        return Result.success(successMessage(kind, affected), affected);
    }

    public static void tick(ServerLevel level) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        SupportTokenCooldowns.cleanup(now);
        processInsightStates(level, now);
        if (SHIELD_RECORDS.isEmpty()) {
            return;
        }
        String dimension = dimensionId(level);
        Iterator<Map.Entry<UUID, ShieldRecord>> iterator = SHIELD_RECORDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ShieldRecord> entry = iterator.next();
            ShieldRecord record = entry.getValue();
            if (record == null || !dimension.equals(record.dimensionId)) {
                continue;
            }
            if (now < record.expireGameTime) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && entity.isAlive()) {
                removeSupportAbsorption(entity, record);
            }
            iterator.remove();
        }
    }

    public static boolean hasNearbyActiveSession(ServerLevel level, Player player, int radius) {
        return findNearestSession(level, player, radius) != null;
    }

    public static void debugLog(String message) {
        if (BattleSupportConfig.DEBUG_LOGS_ENABLED && message != null && !message.isBlank()) {
            System.out.println("[Raid Enhancement Patch] " + message);
        }
    }


    private static Result armInsightToken(ServerLevel level, ActiveSecuritySession activeSession, BattleSupportTokenItem.Kind kind) {
        RaidSession raidSession = raidSessionFor(activeSession);
        if (raidSession == null || raidSession.isClosed()) {
            return Result.failure("[村民防卫同盟] 当前没有可记录洞察状态的袭击战场。");
        }
        int currentWave = Math.max(0, raidSession.currentWave());
        int startWave = Math.max(1, currentWave + 1);
        boolean allRemaining = kind == BattleSupportTokenItem.Kind.ADVANCED_INSIGHT;
        int waveCount = allRemaining ? Integer.MAX_VALUE : Math.max(1, BattleSupportConfig.INSIGHT_BASIC_WAVE_COUNT);
        int lastWave = allRemaining ? Integer.MAX_VALUE : startWave + waveCount - 1;
        INSIGHT_STATES.put(activeSession.raidKey(), new InsightState(activeSession.dimensionId(), startWave, lastWave, allRemaining, new HashSet<>()));
        if (allRemaining) {
            return Result.success("[村民防卫同盟] 高级洞察战备令已响应：后续全部波次的新生袭击者将获得原版发光标记。", 1);
        }
        return Result.success("[村民防卫同盟] 洞察战备令已响应：接下来 " + waveCount + " 波新生袭击者将获得原版发光标记。", 1);
    }

    private static void processInsightStates(ServerLevel level, long gameTime) {
        if (level == null || INSIGHT_STATES.isEmpty()) {
            return;
        }
        String currentDimension = dimensionId(level);
        Iterator<Map.Entry<String, InsightState>> iterator = INSIGHT_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, InsightState> entry = iterator.next();
            RaidSession session = RaidSessionManager.get(entry.getKey()).orElse(null);
            if (session == null || session.isClosed()) {
                iterator.remove();
                continue;
            }
            InsightState state = entry.getValue();
            if (state == null || (!state.dimensionId().isBlank() && !"unknown".equals(currentDimension)
                    && !state.dimensionId().equals(currentDimension))) {
                continue;
            }
            int wave = Math.max(0, session.currentWave());
            if (wave <= 0 || wave < state.startWave()) {
                continue;
            }
            if (!state.allRemaining() && wave > state.lastWave()) {
                iterator.remove();
                continue;
            }
            int duration = Math.max(20, BattleSupportConfig.INSIGHT_GLOW_DURATION_TICKS);
            for (RaiderRecord record : session.trackedRaiders()) {
                if (record == null || !record.aliveWhenLastSeen()) {
                    continue;
                }
                int recordWave = Math.max(1, record.waveIndex());
                if (recordWave < state.startWave() || (!state.allRemaining() && recordWave > state.lastWave())) {
                    continue;
                }
                UUID uuid = record.entityUuid();
                if (uuid == null || state.appliedRaiders().contains(uuid)) {
                    continue;
                }
                Entity entity = level.getEntity(uuid);
                if (entity == null || !entity.isAlive()) {
                    continue;
                }
                MobEffectCompat.addEffectIfLonger(entity, MobEffectCompat.GLOWING_NAMES, duration, 0);
                state.appliedRaiders().add(uuid);
            }
        }
    }

    private static Result useRallyEnemyToken(ServerLevel level, Player player, ActiveSecuritySession activeSession, BattleSupportTokenItem.Kind kind) {
        RaidSession raidSession = raidSessionFor(activeSession);
        if (raidSession == null || raidSession.isClosed()) {
            return Result.failure("[村民防卫同盟] 当前没有可集结敌军的袭击战场。");
        }
        int targetCount = kind == BattleSupportTokenItem.Kind.ADVANCED_RALLY_ENEMY
                ? Math.max(1, BattleSupportConfig.RALLY_ADVANCED_COUNT)
                : Math.max(1, BattleSupportConfig.RALLY_BASIC_COUNT);
        List<Entity> candidates = new ArrayList<>();
        int skipDistance = Math.max(0, BattleSupportConfig.RALLY_EXISTING_NEAR_PLAYER_SKIP_DISTANCE);
        double skipDistanceSq = (double) skipDistance * (double) skipDistance;
        for (RaiderRecord record : raidSession.trackedRaiders()) {
            if (record == null || !record.aliveWhenLastSeen() || blacklistedRallyEntity(record.entityId())) {
                continue;
            }
            Entity entity = level.getEntity(record.entityUuid());
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            double dx = entity.getX() - player.getX();
            double dy = entity.getY() - player.getY();
            double dz = entity.getZ() - player.getZ();
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (skipDistance > 0 && distanceSq <= skipDistanceSq) {
                continue;
            }
            candidates.add(entity);
        }
        if (candidates.isEmpty()) {
            return Result.failure("[村民防卫同盟] 没有找到可被集敌令牵引的有效袭击者。");
        }
        candidates.sort(Comparator.comparingDouble(entity -> -distanceSquared(entity, player)));
        int moved = 0;
        for (Entity entity : candidates) {
            if (moved >= targetCount) {
                break;
            }
            BlockPos pos = findRallyLanding(level, player, moved);
            if (pos == null) {
                continue;
            }
            if (teleportEntity(entity, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)) {
                moved++;
            }
        }
        if (moved <= 0) {
            return Result.failure("[村民防卫同盟] 集敌令没有找到安全落点，未消耗令牌。");
        }
        if (kind == BattleSupportTokenItem.Kind.ADVANCED_RALLY_ENEMY) {
            return Result.success("[村民防卫同盟] 高级集敌战备令已响应，" + moved + " 名袭击者被牵引至你附近。该令牌不会被消耗。", moved);
        }
        return Result.success("[村民防卫同盟] 集敌战备令已响应，" + moved + " 名袭击者被牵引至你附近。", moved);
    }

    private static RaidSession raidSessionFor(ActiveSecuritySession activeSession) {
        if (activeSession == null || activeSession.raidKey() == null || activeSession.raidKey().isBlank()) {
            return null;
        }
        return RaidSessionManager.get(activeSession.raidKey()).orElse(null);
    }

    private static double distanceSquared(Entity entity, Player player) {
        double dx = entity.getX() - player.getX();
        double dy = entity.getY() - player.getY();
        double dz = entity.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean blacklistedRallyEntity(String entityId) {
        String blacklist = BattleSupportConfig.RALLY_BLACKLIST_ENTITY_ID_CONTAINS;
        if (entityId == null || blacklist == null || blacklist.isBlank()) {
            return false;
        }
        String id = entityId.toLowerCase(java.util.Locale.ROOT);
        for (String part : blacklist.split(",")) {
            String token = part == null ? "" : part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!token.isBlank() && id.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findRallyLanding(ServerLevel level, Player player, int offsetIndex) {
        int min = Math.max(1, BattleSupportConfig.RALLY_TELEPORT_MIN_DISTANCE);
        int max = Math.max(min, BattleSupportConfig.RALLY_TELEPORT_MAX_DISTANCE);
        int attempts = 24;
        double base = Math.atan2(player.getZ(), player.getX());
        for (int i = 0; i < attempts; i++) {
            double angle = base + ((offsetIndex * 5 + i) * Math.PI * 2.0D / attempts);
            int radius = min + (i % Math.max(1, max - min + 1));
            int x = (int) Math.round(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.round(player.getZ() + Math.sin(angle) * radius);
            int baseY = (int) Math.floor(player.getY());
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos pos = new BlockPos(x, baseY + dy, z);
                if (isSafeLanding(level, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static boolean isSafeLanding(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        try {
            if (!level.getWorldBorder().isWithinBounds(pos)) {
                return false;
            }
            return level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && !level.getBlockState(pos.below()).isAir();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean teleportEntity(Entity entity, double x, double y, double z) {
        if (entity == null) {
            return false;
        }
        try {
            Method method = entity.getClass().getMethod("teleportTo", double.class, double.class, double.class);
            method.invoke(entity, x, y, z);
            return true;
        } catch (Throwable ignored) {
            // Try another stable positional setter used by most Entity subclasses.
        }
        try {
            Method method = entity.getClass().getMethod("setPos", double.class, double.class, double.class);
            method.invoke(entity, x, y, z);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isKindEnabled(BattleSupportTokenItem.Kind kind) {
        return switch (kind) {
            case BASIC_STRENGTH, ADVANCED_STRENGTH -> BattleSupportConfig.STRENGTH_ENABLED;
            case BASIC_SHIELD, ADVANCED_SHIELD -> BattleSupportConfig.SHIELD_ENABLED;
            case BASIC_SWIFTNESS, ADVANCED_SWIFTNESS -> BattleSupportConfig.SWIFTNESS_ENABLED;
            case HUNTER -> BattleSupportConfig.HUNTER_ENABLED;
            case BASIC_FIRE, ADVANCED_FIRE -> BattleSupportConfig.FIRE_ENABLED;
            case BASIC_INSIGHT, ADVANCED_INSIGHT -> BattleSupportConfig.INSIGHT_ENABLED;
            case BASIC_RALLY_ENEMY, ADVANCED_RALLY_ENEMY -> BattleSupportConfig.RALLY_ENABLED;
        };
    }

    private static void applyKind(ServerLevel level, Entity golem, BattleSupportTokenItem.Kind kind, long now) {
        switch (kind) {
            case BASIC_STRENGTH -> MobEffectCompat.addVisibleEffect(golem, STRENGTH_NAMES,
                    Math.max(20, BattleSupportConfig.STRENGTH_DURATION_TICKS), 0);
            case ADVANCED_STRENGTH -> MobEffectCompat.addVisibleEffect(golem, STRENGTH_NAMES,
                    Math.max(20, BattleSupportConfig.STRENGTH_DURATION_TICKS), 1);
            case BASIC_SHIELD -> applyShield(level, golem, BattleSupportConfig.BASIC_SHIELD_TEMP_HEALTH,
                    Math.max(20, BattleSupportConfig.SHIELD_DURATION_TICKS), now);
            case ADVANCED_SHIELD -> applyShield(level, golem, BattleSupportConfig.ADVANCED_SHIELD_TEMP_HEALTH,
                    Math.max(20, BattleSupportConfig.SHIELD_DURATION_TICKS), now);
            case BASIC_SWIFTNESS -> MobEffectCompat.addVisibleEffect(golem, SPEED_NAMES,
                    Math.max(20, BattleSupportConfig.SWIFTNESS_DURATION_TICKS), 0);
            case ADVANCED_SWIFTNESS -> MobEffectCompat.addVisibleEffect(golem, SPEED_NAMES,
                    Math.max(20, BattleSupportConfig.SWIFTNESS_DURATION_TICKS), 1);
            case HUNTER -> {
                MobEffectCompat.addVisibleEffect(golem, SPEED_NAMES, Math.max(20, BattleSupportConfig.HUNTER_DURATION_TICKS), 1);
                MobEffectCompat.addVisibleEffect(golem, STRENGTH_NAMES, Math.max(20, BattleSupportConfig.HUNTER_DURATION_TICKS), 0);
            }
            case BASIC_FIRE -> MobEffectCompat.addVisibleEffect(golem, FIRE_RESISTANCE_NAMES,
                    Math.max(20, BattleSupportConfig.FIRE_DURATION_TICKS), 0);
            case ADVANCED_FIRE -> {
                MobEffectCompat.addVisibleEffect(golem, FIRE_RESISTANCE_NAMES, Math.max(20, BattleSupportConfig.FIRE_DURATION_TICKS), 0);
                if (BattleSupportConfig.ADVANCED_FIRE_ADDS_REGENERATION) {
                    MobEffectCompat.addVisibleEffect(golem, REGENERATION_NAMES, Math.max(20, BattleSupportConfig.FIRE_DURATION_TICKS), 0);
                }
            }
            case BASIC_INSIGHT, ADVANCED_INSIGHT, BASIC_RALLY_ENEMY, ADVANCED_RALLY_ENEMY -> {
                // Handled before golem-buff dispatch.
            }
        }
    }

    private static void applyShield(ServerLevel level, Entity golem, float amount, int durationTicks, long now) {
        if (golem == null || amount <= 0.0F) {
            return;
        }
        UUID uuid = golem.getUUID();
        int safeDuration = Math.max(20, durationTicks);
        ShieldRecord existing = SHIELD_RECORDS.get(uuid);
        float baseline = existing == null ? getAbsorptionAmount(golem) : existing.baselineAbsorption;
        float effectiveAmount = existing == null ? amount : Math.max(existing.supportAmount, amount);

        // Functional shield: keep an internal damage-absorbing pool. This fixes the case where
        // vanilla absorption on IronGolems is hidden by UI mods or cannot be reliably written by
        // the staged compatibility build. Damage interception below consumes this pool directly.
        SHIELD_RECORDS.put(uuid, new ShieldRecord(dimensionId(level), now + safeDuration, baseline, effectiveAmount));

        // Visual/vanilla backup: try to also expose the temporary HP through vanilla absorption.
        // The direct setter keeps the exact requested value (30 / 100), while the effect gives
        // clients and inspection mods a conventional status-effect indicator when supported.
        float current = getAbsorptionAmount(golem);
        setAbsorptionAmount(golem, Math.max(current, effectiveAmount));
        int amplifier = absorptionAmplifierFor(effectiveAmount);
        if (amplifier >= 0) {
            MobEffectCompat.addVisibleEffect(golem, ABSORPTION_NAMES, safeDuration, amplifier);
            setAbsorptionAmount(golem, Math.max(getAbsorptionAmount(golem), effectiveAmount));
        }
    }

    /**
     * Consumes shield-token temporary health before the IronGolem receives real damage.
     * Returns true when the incoming damage has been fully absorbed/cancelled.
     */
    public static boolean absorbShieldDamage(Object entity, Object damageEvent) {
        if (entity == null || damageEvent == null || SHIELD_RECORDS.isEmpty()) {
            return false;
        }
        UUID uuid = uuidOf(entity);
        if (uuid == null) {
            return false;
        }
        ShieldRecord record = SHIELD_RECORDS.get(uuid);
        if (record == null || record.supportAmount <= 0.0F) {
            return false;
        }
        float amount = eventAmount(damageEvent);
        if (amount <= 0.0F) {
            return false;
        }
        float absorbed = Math.min(amount, record.supportAmount);
        float remainingShield = Math.max(0.0F, record.supportAmount - absorbed);
        if (remainingShield <= 0.0F) {
            SHIELD_RECORDS.remove(uuid);
        } else {
            SHIELD_RECORDS.put(uuid, new ShieldRecord(record.dimensionId, record.expireGameTime, record.baselineAbsorption, remainingShield));
        }
        if (entity instanceof Entity golem) {
            setAbsorptionAmount(golem, Math.max(record.baselineAbsorption, remainingShield));
        } else {
            setAbsorptionAmount(entity, Math.max(record.baselineAbsorption, remainingShield));
        }
        float remainingDamage = Math.max(0.0F, amount - absorbed);
        if (remainingDamage <= 0.0F) {
            cancelEvent(damageEvent);
            return true;
        }
        setEventAmount(damageEvent, remainingDamage);
        return false;
    }

    private static void removeSupportAbsorption(Entity golem, ShieldRecord record) {
        float current = getAbsorptionAmount(golem);
        float next;
        if (current <= record.supportAmount + 0.1F) {
            next = Math.max(0.0F, record.baselineAbsorption);
        } else {
            next = Math.max(record.baselineAbsorption, current - record.supportAmount);
        }
        setAbsorptionAmount(golem, next);
    }

    private static int absorptionAmplifierFor(float temporaryHealth) {
        if (temporaryHealth <= 0.0F) {
            return -1;
        }
        return Math.max(0, (int) Math.ceil(temporaryHealth / 4.0F) - 1);
    }

    private static float getAbsorptionAmount(Object livingEntity) {
        if (livingEntity == null) {
            return 0.0F;
        }
        try {
            Method method = livingEntity.getClass().getMethod("getAbsorptionAmount");
            Object result = method.invoke(livingEntity);
            if (result instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable throwable) {
            warnAbsorptionOnce("Unable to read absorption amount reflectively: " + throwable);
        }
        return 0.0F;
    }

    private static void setAbsorptionAmount(Object livingEntity, float amount) {
        if (livingEntity == null) {
            return;
        }
        try {
            Method method = livingEntity.getClass().getMethod("setAbsorptionAmount", float.class);
            method.invoke(livingEntity, Math.max(0.0F, amount));
            return;
        } catch (Throwable ignored) {
            // Try declared methods below.
        }
        try {
            for (Method method : livingEntity.getClass().getMethods()) {
                if (!method.getName().equals("setAbsorptionAmount") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> type = method.getParameterTypes()[0];
                if (type == float.class || type == Float.TYPE) {
                    method.invoke(livingEntity, Math.max(0.0F, amount));
                    return;
                }
                if (type == double.class || type == Double.TYPE) {
                    method.invoke(livingEntity, (double) Math.max(0.0F, amount));
                    return;
                }
            }
        } catch (Throwable throwable) {
            warnAbsorptionOnce("Unable to set absorption amount reflectively: " + throwable);
        }
    }

    private static UUID uuidOf(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            Method method = entity.getClass().getMethod("getUUID");
            Object value = method.invoke(entity);
            return value instanceof UUID uuid ? uuid : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float eventAmount(Object event) {
        if (event == null) {
            return 0.0F;
        }
        try {
            Method method = event.getClass().getMethod("getAmount");
            Object value = method.invoke(event);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (Throwable ignored) {
            // Fall through.
        }
        return 0.0F;
    }

    private static void setEventAmount(Object event, float amount) {
        if (event == null) {
            return;
        }
        try {
            Method method = event.getClass().getMethod("setAmount", float.class);
            method.invoke(event, Math.max(0.0F, amount));
            return;
        } catch (Throwable ignored) {
            // Try compatible numeric descriptors below.
        }
        try {
            for (Method method : event.getClass().getMethods()) {
                if (!"setAmount".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> type = method.getParameterTypes()[0];
                if (type == float.class || type == Float.TYPE) {
                    method.invoke(event, Math.max(0.0F, amount));
                    return;
                }
                if (type == double.class || type == Double.TYPE) {
                    method.invoke(event, (double) Math.max(0.0F, amount));
                    return;
                }
            }
        } catch (Throwable ignored) {
            // No-op.
        }
    }

    private static void cancelEvent(Object event) {
        if (event == null) {
            return;
        }
        try {
            Method method = event.getClass().getMethod("setCanceled", boolean.class);
            method.invoke(event, true);
        } catch (Throwable ignored) {
            // Some event implementations may not be cancellable in a staged environment.
            setEventAmount(event, 0.0F);
        }
    }

    private static void warnAbsorptionOnce(String message) {
        if (!warnedAbsorptionAccess) {
            warnedAbsorptionAccess = true;
            System.out.println("[Raid Enhancement Patch] " + message);
        }
    }

    private static ActiveSecuritySession findNearestSession(ServerLevel level, Player player, int radius) {
        if (level == null || player == null) {
            return null;
        }
        double bestDistance = Double.MAX_VALUE;
        ActiveSecuritySession best = null;
        String currentDimension = dimensionId(level);
        int safeRadius = Math.max(1, radius);
        double radiusSquared = (double) safeRadius * (double) safeRadius;
        for (VillageSecurityRuntimeView session : VillageSecurityController.runtimeViews()) {
            if (session == null) {
                continue;
            }
            String sessionDimension = session.dimensionId() == null ? "" : session.dimensionId();
            if (!sessionDimension.isBlank() && !"unknown".equals(currentDimension) && !sessionDimension.equals(currentDimension)) {
                continue;
            }
            int centerX = session.centerX();
            int centerY = session.centerY();
            int centerZ = session.centerZ();
            double dx = player.getX() - (centerX + 0.5D);
            double dy = player.getY() - centerY;
            double dz = player.getZ() - (centerZ + 0.5D);
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance > radiusSquared || distance >= bestDistance) {
                continue;
            }
            bestDistance = distance;
            best = new ActiveSecuritySession(session.raidKey(), sessionDimension,
                    centerX, centerY, centerZ, session.securityGolemIds());
        }
        return best;
    }

    private static boolean isIronGolem(Object entity) {
        try {
            return Class.forName("net.minecraft.world.entity.animal.IronGolem").isInstance(entity);
        } catch (Throwable ignored) {
            return entity != null && entity.getClass().getName().endsWith("IronGolem");
        }
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

    private static String successMessage(BattleSupportTokenItem.Kind kind, int affected) {
        return switch (kind) {
            case BASIC_STRENGTH -> "[村民防卫同盟] 力量战备令已响应，" + affected + " 名安防铁傀儡获得力量 I。";
            case ADVANCED_STRENGTH -> "[村民防卫同盟] 高级力量战备令已响应，" + affected + " 名安防铁傀儡进入强攻姿态。";
            case BASIC_SHIELD -> "[村民防卫同盟] 圣盾战备令已响应，" + affected + " 名安防铁傀儡获得 30 点临时生命。";
            case ADVANCED_SHIELD -> "[村民防卫同盟] 高级圣盾战备令已响应，" + affected + " 名安防铁傀儡获得 100 点临时生命。";
            case BASIC_SWIFTNESS -> "[村民防卫同盟] 迅捷战备令已响应，" + affected + " 名安防铁傀儡加速接敌。";
            case ADVANCED_SWIFTNESS -> "[村民防卫同盟] 高级迅捷战备令已响应，" + affected + " 名安防铁傀儡进入快速拦截状态。";
            case HUNTER -> "[村民防卫同盟] 追猎协议已激活，" + affected + " 名安防铁傀儡正在加速清剿残敌。";
            case BASIC_FIRE -> "[村民防卫同盟] 炽火战备令已响应，" + affected + " 名安防铁傀儡获得火焰防护。";
            case ADVANCED_FIRE -> "[村民防卫同盟] 高级炽火战备令已响应，" + affected + " 名安防铁傀儡获得熔火防护。";
            case BASIC_INSIGHT -> "[村民防卫同盟] 洞察战备令已响应。";
            case ADVANCED_INSIGHT -> "[村民防卫同盟] 高级洞察战备令已响应。";
            case BASIC_RALLY_ENEMY -> "[村民防卫同盟] 集敌战备令已响应。";
            case ADVANCED_RALLY_ENEMY -> "[村民防卫同盟] 高级集敌战备令已响应。";
        };
    }

    public record Result(boolean success, String message, int affectedCount) {
        public static Result success(String message, int affectedCount) {
            return new Result(true, message, affectedCount);
        }

        public static Result failure(String message) {
            return new Result(false, message, 0);
        }
    }

    private record ShieldRecord(String dimensionId, long expireGameTime, float baselineAbsorption, float supportAmount) {
    }

    private record InsightState(String dimensionId, int startWave, int lastWave, boolean allRemaining, Set<UUID> appliedRaiders) {
    }

    private record ActiveSecuritySession(String raidKey, String dimensionId, int centerX, int centerY, int centerZ, List<UUID> securityGolemIds) {
        List<Entity> aliveSecurityGolems(ServerLevel level) {
            if (level == null || securityGolemIds == null || securityGolemIds.isEmpty()) {
                return List.of();
            }
            List<Entity> golems = new ArrayList<>();
            for (UUID uuid : securityGolemIds) {
                Entity entity = level.getEntity(uuid);
                if (entity != null && entity.isAlive() && isIronGolem(entity)) {
                    golems.add(entity);
                }
            }
            return golems;
        }
    }
}
