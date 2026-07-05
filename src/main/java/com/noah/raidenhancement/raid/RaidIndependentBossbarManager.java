package com.noah.raidenhancement.raid;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.8.9.9.8: independent raid bossbar presenter with dynamic per-wave progress.
 *
 * The previous UI path fought vanilla Raid's own ServerBossEvent#setName calls.
 * That reduced but could not eliminate title flicker. This manager creates one
 * mod-owned ServerBossEvent per managed encounter, hides the vanilla raid bar,
 * and only syncs players/title/progress on throttled intervals. It is entirely
 * presentation-only and intentionally uses reflection so a UI failure cannot
 * crash raid logic in NeoForge 1.21.1 modpacks.
 */
public final class RaidIndependentBossbarManager {
    private static final Map<String, ManagedBossbar> BARS = new LinkedHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> NO_ARG_METHOD_CACHE = new IdentityHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> ONE_ARG_METHOD_CACHE = new IdentityHashMap<>();
    private static final Set<String> PLAYERS_SNAPSHOT = new HashSet<>();

    private static Field statesField;
    private static boolean statesFieldResolved;
    private static boolean warnedCreateFailure;
    private static boolean warnedTickFailure;
    private static boolean warnedVanillaHideFailure;
    private static boolean announced;

    private static final long PLAYER_SYNC_INTERVAL_TICKS = 20L;
    private static final long CLEANUP_GRACE_TICKS = 80L;
    private static final double PLAYER_RADIUS_SQ = 128.0D * 128.0D;

    private RaidIndependentBossbarManager() {
    }

    public static void tick(Object serverLevel) {
        if (serverLevel == null) {
            return;
        }
        try {
            long gameTime = gameTime(serverLevel);
            if (!announced) {
                announced = true;
                System.out.println("[Raid Enhancement Patch] 0.8.9.9.6 independent raid BossBar presenter is active. Vanilla raid BossBar is hidden only after the mod-owned BossBar is created; player sync is throttled for performance.");
            }
            List<RaidEncounterSnapshot> snapshots = RaidEncounterAuthority.snapshots();
            Set<String> activeKeys = new HashSet<>();
            for (RaidEncounterSnapshot snapshot : snapshots) {
                if (snapshot == null || snapshot.key() == null || snapshot.key().isBlank()) {
                    continue;
                }
                if (!sameDimension(serverLevel, snapshot.dimensionId())) {
                    continue;
                }
                activeKeys.add(snapshot.key());
                ManagedBossbar bar = BARS.computeIfAbsent(snapshot.key(), key -> new ManagedBossbar());
                if (bar.bossEvent == null) {
                    bar.bossEvent = createBossEvent(snapshot, gameTime);
                    bar.createdGameTime = gameTime;
                    if (bar.bossEvent != null) {
                        RaidKeyDiagnostics.logBossbar("created", serverLevel, snapshot, gameTime, bar.lastWave,
                                bar.waveBaselineRaiders, bar.lastAliveRaiders, bar.lastProgress);
                    }
                }
                if (bar.bossEvent == null) {
                    // Do not hide vanilla if the replacement bar could not be built.
                    continue;
                }
                updateBar(serverLevel, bar, snapshot, gameTime);
                if (bar.lastPlayerSyncGameTime <= 0L
                        || gameTime - bar.lastPlayerSyncGameTime >= PLAYER_SYNC_INTERVAL_TICKS) {
                    syncPlayers(serverLevel, bar, snapshot, gameTime);
                }
                hideVanillaBossbar(snapshot.key());
                bar.lastSeenGameTime = gameTime;
            }
            cleanupInactive(activeKeys, gameTime);
        } catch (Throwable throwable) {
            if (!warnedTickFailure) {
                warnedTickFailure = true;
                System.out.println("[Raid Enhancement Patch] Independent raid BossBar manager failed once and was suppressed: " + throwable);
            }
        }
    }

    private static Object createBossEvent(RaidEncounterSnapshot snapshot, long gameTime) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Object title = component(componentClass, RaidBossbarTitleFormatter.format(snapshot, gameTime));
            Class<?> colorClass = Class.forName("net.minecraft.world.BossEvent$BossBarColor");
            Class<?> overlayClass = Class.forName("net.minecraft.world.BossEvent$BossBarOverlay");
            Object color = enumValue(colorClass, "RED");
            Object overlay = enumValue(overlayClass, "NOTCHED_10");
            Class<?> bossEventClass = Class.forName("net.minecraft.server.level.ServerBossEvent");
            Constructor<?> constructor = bossEventClass.getDeclaredConstructor(componentClass, colorClass, overlayClass);
            constructor.setAccessible(true);
            Object bossEvent = constructor.newInstance(title, color, overlay);
            invokeOneArg(bossEvent, "setVisible", boolean.class, true);
            invokeOneArg(bossEvent, "setProgress", float.class, 1.0F);
            return bossEvent;
        } catch (Throwable throwable) {
            if (!warnedCreateFailure) {
                warnedCreateFailure = true;
                System.out.println("[Raid Enhancement Patch] Could not create independent raid BossBar; vanilla bar will remain visible: " + throwable);
            }
            return null;
        }
    }

    private static void updateBar(Object serverLevel, ManagedBossbar bar, RaidEncounterSnapshot snapshot, long gameTime) {
        String title = RaidBossbarTitleFormatter.format(snapshot, gameTime);
        if (title != null && !title.equals(bar.lastTitle)) {
            Object component = component(title);
            if (component != null && invokeBestOneArg(bar.bossEvent, "setName", component)) {
                bar.lastTitle = title;
            }
        }
        float progress = progress(serverLevel, snapshot, snapshot.key(), bar, gameTime);
        boolean progressApplied = false;
        if (Math.abs(progress - bar.lastProgress) >= 0.005F) {
            if (invokeOneArg(bar.bossEvent, "setProgress", float.class, progress)) {
                bar.lastProgress = progress;
                progressApplied = true;
            }
        }
        invokeOneArg(bar.bossEvent, "setVisible", boolean.class, true);
        RaidKeyDiagnostics.logBossbar("update", serverLevel, snapshot, gameTime, bar.diagPreviousLastWave,
                bar.waveBaselineRaiders, bar.lastAliveRaiders, bar.lastProgress,
                bar.diagAlive, bar.diagNativeCount, bar.diagSessionCount, bar.diagNearbyCount,
                bar.diagCountSource, bar.diagWaveChange, bar.diagBaselineReset, bar.diagRefillAttempt,
                progressApplied, progress, bar.diagVanillaProgress, bar.diagDecision);
    }

    private static float progress(Object serverLevel, RaidEncounterSnapshot snapshot, String key, ManagedBossbar bar, long gameTime) {
        // 0.9.0.2: Diagnostics only. Keep the 0.8.9.9.8 dynamic progress behavior,
        // but record every decision needed to explain wave refill failures.
        if (snapshot == null) {
            return 1.0F;
        }
        if (bar == null) {
            Float vanilla = vanillaBossbarProgress(key);
            return vanilla == null ? 1.0F : clampProgress(vanilla);
        }

        resetBossbarDiagnostics(bar);
        int wave = Math.max(1, snapshot.currentWave());
        float previousDisplayedProgress = bar.lastProgress;
        bar.diagPreviousLastWave = bar.lastWave;
        if (bar.lastWave != wave) {
            bar.diagWaveChange = true;
            bar.diagBaselineReset = true;
            bar.lastWave = wave;
            bar.waveBaselineRaiders = 0;
            bar.lastAliveRaiders = -1;
            bar.waveEverSawRaiders = false;
            // Force the first update of a new wave to render.
            bar.lastProgress = -1.0F;
        }

        CountDiagnostics counts = effectiveLiveRaiderCountDetailed(serverLevel, snapshot, key, gameTime);
        bar.diagAlive = counts.alive;
        bar.diagNativeCount = counts.nativeCount;
        bar.diagSessionCount = counts.sessionCount;
        bar.diagNearbyCount = counts.nearbyCount;
        bar.diagCountSource = counts.source;

        if (counts.alive >= 0) {
            int alive = counts.alive;
            if (alive > 0) {
                bar.waveEverSawRaiders = true;
                if (alive > bar.waveBaselineRaiders) {
                    bar.waveBaselineRaiders = alive;
                }
            }
            bar.lastAliveRaiders = alive;

            if (bar.waveBaselineRaiders > 0) {
                float computed = clampProgress((float) alive / (float) Math.max(1, bar.waveBaselineRaiders));
                return finishProgressDiagnostic(bar, computed, previousDisplayedProgress,
                        "alive-over-baseline", Float.NaN);
            }

            // No raiders observed yet for this wave: this is normally the
            // preparation/spawn window. Show full until the first live raider is
            // observed, rather than showing an empty bar before the wave starts.
            return finishProgressDiagnostic(bar, 1.0F, previousDisplayedProgress,
                    "no-raiders-observed-yet-show-full", Float.NaN);
        }

        // Count was unavailable. Prefer the hidden vanilla bar only if it provides
        // a meaningful non-full value; otherwise preserve our previous dynamic
        // value. This avoids the 0.8.9.9.7 regression where fallback full progress
        // made the bar refill but never decrease.
        Float vanilla = vanillaBossbarProgress(key);
        float vanillaValue = vanilla == null ? Float.NaN : clampProgress(vanilla);
        bar.diagVanillaProgress = vanillaValue;
        if (vanilla != null && !vanilla.isNaN() && !vanilla.isInfinite()) {
            float v = clampProgress(vanilla);
            if (v < 0.995F || !bar.waveEverSawRaiders) {
                return finishProgressDiagnostic(bar, v, previousDisplayedProgress,
                        "vanilla-fallback", vanillaValue);
            }
        }
        if (bar.waveBaselineRaiders > 0 && bar.lastAliveRaiders >= 0) {
            float computed = clampProgress((float) bar.lastAliveRaiders / (float) Math.max(1, bar.waveBaselineRaiders));
            return finishProgressDiagnostic(bar, computed, previousDisplayedProgress,
                    "preserve-last-alive-over-baseline", vanillaValue);
        }
        if (bar.lastProgress >= 0.0F) {
            return finishProgressDiagnostic(bar, clampProgress(bar.lastProgress), previousDisplayedProgress,
                    "preserve-last-progress", vanillaValue);
        }
        return finishProgressDiagnostic(bar, 1.0F, previousDisplayedProgress,
                "default-full", vanillaValue);
    }

    private static float finishProgressDiagnostic(ManagedBossbar bar, float computed, float previousDisplayedProgress,
                                                  String decision, float vanillaProgress) {
        float progress = clampProgress(computed);
        bar.diagComputedProgress = progress;
        bar.diagVanillaProgress = vanillaProgress;
        bar.diagDecision = decision;
        bar.diagRefillAttempt = bar.diagWaveChange
                || previousDisplayedProgress < 0.0F
                || progress > previousDisplayedProgress + 0.005F;
        return progress;
    }

    private static void resetBossbarDiagnostics(ManagedBossbar bar) {
        bar.diagPreviousLastWave = bar.lastWave;
        bar.diagWaveChange = false;
        bar.diagBaselineReset = false;
        bar.diagRefillAttempt = false;
        bar.diagAlive = -1;
        bar.diagNativeCount = -1;
        bar.diagSessionCount = -1;
        bar.diagNearbyCount = -1;
        bar.diagCountSource = "uncomputed";
        bar.diagVanillaProgress = Float.NaN;
        bar.diagComputedProgress = Float.NaN;
        bar.diagDecision = "uncomputed";
    }

    private static int effectiveLiveRaiderCount(Object serverLevel, RaidEncounterSnapshot snapshot, String key, long gameTime) {
        return effectiveLiveRaiderCountDetailed(serverLevel, snapshot, key, gameTime).alive;
    }

    private static CountDiagnostics effectiveLiveRaiderCountDetailed(Object serverLevel, RaidEncounterSnapshot snapshot, String key, long gameTime) {
        Object state = null;
        Object nativeRaid = null;
        try {
            state = stateByKey(key);
            nativeRaid = state == null ? null : readObject(state, "nativeRaid");
        } catch (Throwable ignored) {
            // Fall through to the next source.
        }

        int nativeCount = countNativeRaiders(nativeRaid);
        if (nativeCount >= 0) {
            return new CountDiagnostics(nativeCount, "nativeRaid", nativeCount, -1, -1);
        }

        int sessionCount = countSessionRaiders(serverLevel, key, Math.max(1, snapshot.currentWave()), gameTime);
        if (sessionCount >= 0) {
            return new CountDiagnostics(sessionCount, "raidSession", nativeCount, sessionCount, -1);
        }

        // Last-resort low-frequency local scan. This is intentionally throttled
        // per bar, not a full-world scan, and only used when the Raid object and
        // runtime session cannot provide a count.
        if (state != null) {
            Integer scanned = throttledNearbyRaiderCount(serverLevel, key, snapshot, gameTime);
            if (scanned != null) {
                return new CountDiagnostics(scanned, "nearbyScan", nativeCount, sessionCount, scanned);
            }
        }
        return new CountDiagnostics(-1, state == null ? "unavailable:no-state" : "unavailable:no-count-source", nativeCount, sessionCount, -1);
    }

    private static int countNativeRaiders(Object nativeRaid) {
        if (nativeRaid == null) {
            return -1;
        }
        Object totalAlive = invokeNoArgValue(nativeRaid, "getTotalRaidersAlive");
        if (totalAlive instanceof Number number) {
            return Math.max(0, number.intValue());
        }

        Object allRaiders = invokeNoArgValue(nativeRaid, "getAllRaiders");
        if (allRaiders instanceof Iterable<?> iterable) {
            int count = 0;
            for (Object raider : iterable) {
                if (isEntityAlive(raider)) {
                    count++;
                }
            }
            return count;
        }

        // Fallback for mappings where public methods are unavailable: scan fields
        // that look like collections/maps of raiders on the Raid object only.
        int reflected = countRaidersFromRaidFields(nativeRaid);
        return reflected >= 0 ? reflected : -1;
    }

    private static int countRaidersFromRaidFields(Object nativeRaid) {
        if (nativeRaid == null) {
            return -1;
        }
        int count = 0;
        boolean foundAnyContainer = false;
        Class<?> current = nativeRaid.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(nativeRaid);
                    if (value instanceof Map<?, ?> map) {
                        for (Object entryValue : map.values()) {
                            CountResult result = countPossibleRaiderContainer(entryValue);
                            if (result.found()) {
                                foundAnyContainer = true;
                                count += result.count();
                            }
                        }
                    } else {
                        CountResult result = countPossibleRaiderContainer(value);
                        if (result.found()) {
                            foundAnyContainer = true;
                            count += result.count();
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try next field.
                }
            }
            current = current.getSuperclass();
        }
        return foundAnyContainer ? Math.max(0, count) : -1;
    }

    private static CountResult countPossibleRaiderContainer(Object value) {
        if (value == null) {
            return CountResult.none();
        }
        if (isRaiderLike(value)) {
            return new CountResult(true, isEntityAlive(value) ? 1 : 0);
        }
        if (value instanceof Iterable<?> iterable) {
            int count = 0;
            boolean found = false;
            for (Object element : iterable) {
                if (isRaiderLike(element)) {
                    found = true;
                    if (isEntityAlive(element)) {
                        count++;
                    }
                }
            }
            return new CountResult(found, count);
        }
        return CountResult.none();
    }

    private static boolean isRaiderLike(Object entity) {
        if (entity == null) {
            return false;
        }
        Class<?> current = entity.getClass();
        while (current != null && current != Object.class) {
            String name = current.getName();
            if ("net.minecraft.world.entity.raid.Raider".equals(name) || name.endsWith(".Raider")) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static boolean isEntityAlive(Object entity) {
        if (entity == null) {
            return false;
        }
        Object alive = invokeNoArgValue(entity, "isAlive");
        if (alive instanceof Boolean b) {
            return b;
        }
        Object removed = invokeNoArgValue(entity, "isRemoved");
        if (removed instanceof Boolean b) {
            return !b;
        }
        return true;
    }

    private static int countSessionRaiders(Object serverLevel, String key, int wave, long gameTime) {
        if (key == null || key.isBlank() || serverLevel == null) {
            return -1;
        }
        try {
            java.util.Optional<RaidSession> sessionOptional = RaidSessionManager.get(key);
            if (sessionOptional.isEmpty()) {
                return -1;
            }
            RaidSession session = sessionOptional.get();
            int count = 0;
            boolean found = false;
            for (RaiderRecord record : session.trackedRaiders()) {
                if (record == null || record.entityUuid() == null || record.waveIndex() != wave) {
                    continue;
                }
                Object entity = entityByUuid(serverLevel, record.entityUuid());
                if (entity == null) {
                    continue;
                }
                found = true;
                if (isEntityAlive(entity)) {
                    count++;
                }
            }
            return found ? Math.max(0, count) : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static Object entityByUuid(Object serverLevel, UUID uuid) {
        if (serverLevel == null || uuid == null) {
            return null;
        }
        Object value = invokeBestOneArgValue(serverLevel, "getEntity", uuid);
        if (value != null) {
            return value;
        }
        value = invokeBestOneArgValue(serverLevel, "getEntityOrPart", uuid);
        return value;
    }

    private static Integer throttledNearbyRaiderCount(Object serverLevel, String key, RaidEncounterSnapshot snapshot, long gameTime) {
        ManagedBossbar bar = BARS.get(key);
        if (bar == null || serverLevel == null || snapshot == null) {
            return null;
        }
        long interval = 10L;
        if (bar.lastNearbyScanGameTime > 0L && gameTime - bar.lastNearbyScanGameTime < interval) {
            return bar.lastNearbyScanCount < 0 ? null : bar.lastNearbyScanCount;
        }
        bar.lastNearbyScanGameTime = gameTime;
        int count = nearbyRaiderCount(serverLevel, snapshot.centerX(), snapshot.centerY(), snapshot.centerZ(), 96);
        bar.lastNearbyScanCount = count;
        return count < 0 ? null : count;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int nearbyRaiderCount(Object serverLevel, int x, int y, int z, int radius) {
        try {
            Class<?> raiderClass = Class.forName("net.minecraft.world.entity.raid.Raider");
            Class<?> aabbClass = Class.forName("net.minecraft.world.phys.AABB");
            Constructor<?> constructor = aabbClass.getConstructor(double.class, double.class, double.class,
                    double.class, double.class, double.class);
            int r = Math.max(1, radius);
            Object box = constructor.newInstance(x - r, y - r, z - r, x + r, y + r, z + r);
            Method getEntities = serverLevel.getClass().getMethod("getEntitiesOfClass", Class.class, aabbClass);
            Object result = getEntities.invoke(serverLevel, (Class) raiderClass, box);
            if (!(result instanceof List<?> list)) {
                return -1;
            }
            int count = 0;
            for (Object candidate : list) {
                if (isEntityAlive(candidate)) {
                    count++;
                }
            }
            return count;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static Float vanillaBossbarProgress(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            Object state = stateByKey(key);
            if (state == null) {
                return null;
            }
            Object nativeRaid = readObject(state, "nativeRaid");
            Object bossEvent = findServerBossEvent(nativeRaid);
            return readBossEventProgress(bossEvent);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Float readBossEventProgress(Object bossEvent) {
        if (bossEvent == null) {
            return null;
        }
        Object value = invokeNoArgValue(bossEvent, "getProgress");
        if (value instanceof Number number) {
            return number.floatValue();
        }

        // Fallback for mappings/modpacks where getProgress is not directly
        // available through reflection. Prefer explicitly named progress fields,
        // then fall back to the first float-like BossEvent field in [0, 1].
        Float named = readFloatField(bossEvent, true);
        if (named != null) {
            return named;
        }
        return readFloatField(bossEvent, false);
    }

    private static Float readFloatField(Object target, boolean requireProgressName) {
        if (target == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    Class<?> type = field.getType();
                    if (type != float.class && type != Float.class && type != double.class && type != Double.class) {
                        continue;
                    }
                    String name = field.getName() == null ? "" : field.getName().toLowerCase(java.util.Locale.ROOT);
                    if (requireProgressName && !name.contains("progress")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof Number number) {
                        float f = number.floatValue();
                        if (!Float.isNaN(f) && !Float.isInfinite(f) && f >= 0.0F && f <= 1.0F) {
                            return f;
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try next field.
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static float clampProgress(float progress) {
        if (progress <= 0.0F) {
            return 0.0F;
        }
        if (progress >= 1.0F) {
            return 1.0F;
        }
        return progress;
    }

    private static void syncPlayers(Object serverLevel, ManagedBossbar bar, RaidEncounterSnapshot snapshot, long gameTime) {
        List<?> players = players(serverLevel);
        if (players == null) {
            return;
        }
        PLAYERS_SNAPSHOT.clear();
        for (Object player : players) {
            if (player == null) {
                continue;
            }
            if (distanceSq(player, snapshot.centerX(), snapshot.centerY(), snapshot.centerZ()) > PLAYER_RADIUS_SQ) {
                continue;
            }
            String id = playerIdentity(player);
            if (id == null || id.isBlank()) {
                continue;
            }
            PLAYERS_SNAPSHOT.add(id);
            if (!bar.players.containsKey(id)) {
                if (invokeBestOneArg(bar.bossEvent, "addPlayer", player)) {
                    bar.players.put(id, player);
                }
            }
        }
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Object> entry : bar.players.entrySet()) {
            if (!PLAYERS_SNAPSHOT.contains(entry.getKey())) {
                invokeBestOneArg(bar.bossEvent, "removePlayer", entry.getValue());
                toRemove.add(entry.getKey());
            }
        }
        for (String id : toRemove) {
            bar.players.remove(id);
        }
        bar.lastPlayerSyncGameTime = gameTime;
    }

    private static void cleanupInactive(Set<String> activeKeys, long gameTime) {
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, ManagedBossbar> entry : BARS.entrySet()) {
            String key = entry.getKey();
            ManagedBossbar bar = entry.getValue();
            if (activeKeys.contains(key)) {
                continue;
            }
            if (bar == null || gameTime - bar.lastSeenGameTime >= CLEANUP_GRACE_TICKS) {
                remove.add(key);
            }
        }
        for (String key : remove) {
            ManagedBossbar bar = BARS.remove(key);
            if (bar != null) {
                removeAllPlayers(bar.bossEvent, bar.players);
            }
        }
    }

    private static void hideVanillaBossbar(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Object state = stateByKey(key);
            if (state == null) {
                return;
            }
            Object nativeRaid = readObject(state, "nativeRaid");
            Object bossEvent = findServerBossEvent(nativeRaid);
            if (bossEvent == null) {
                return;
            }
            boolean hidden = invokeOneArg(bossEvent, "setVisible", boolean.class, false);
            if (!hidden) {
                invokeNoArg(bossEvent, "removeAllPlayers");
            }
        } catch (Throwable throwable) {
            if (!warnedVanillaHideFailure) {
                warnedVanillaHideFailure = true;
                System.out.println("[Raid Enhancement Patch] Could not hide vanilla raid BossBar once; independent bar remains active: " + throwable);
            }
        }
    }

    private static void removeAllPlayers(Object bossEvent, Map<String, Object> players) {
        if (bossEvent == null) {
            return;
        }
        if (invokeNoArg(bossEvent, "removeAllPlayers")) {
            players.clear();
            invokeOneArg(bossEvent, "setVisible", boolean.class, false);
            return;
        }
        for (Object player : new ArrayList<>(players.values())) {
            invokeBestOneArg(bossEvent, "removePlayer", player);
        }
        players.clear();
        invokeOneArg(bossEvent, "setVisible", boolean.class, false);
    }

    private static Object stateByKey(String key) throws ReflectiveOperationException {
        Map<?, ?> states = states();
        return states == null ? null : states.get(key);
    }

    private static Map<?, ?> states() throws ReflectiveOperationException {
        if (!statesFieldResolved) {
            statesFieldResolved = true;
            Field field = RaidExtraWaveController.class.getDeclaredField("STATES");
            field.setAccessible(true);
            statesField = field;
        }
        if (statesField == null) {
            return null;
        }
        Object value = statesField.get(null);
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return null;
    }

    private static Object findServerBossEvent(Object raid) {
        if (raid == null) {
            return null;
        }
        Class<?> current = raid.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(raid);
                    if (value == null) {
                        continue;
                    }
                    String className = value.getClass().getName();
                    if ("net.minecraft.server.level.ServerBossEvent".equals(className)
                            || className.endsWith("ServerBossEvent")) {
                        return value;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try next field.
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean sameDimension(Object level, String snapshotDimension) {
        if (snapshotDimension == null || snapshotDimension.isBlank()) {
            return true;
        }
        try {
            Object dimension = invokeNoArgValue(level, "dimension");
            Object location = dimension == null ? null : invokeNoArgValue(dimension, "location");
            String dimensionText = dimension == null ? "" : String.valueOf(dimension);
            String locationText = location == null ? "" : String.valueOf(location);
            return snapshotDimension.equals(locationText)
                    || snapshotDimension.equals(dimensionText)
                    || (!dimensionText.isBlank() && dimensionText.contains(snapshotDimension));
        } catch (Throwable ignored) {
            // Dimension matching is only a presentation filter. Fail open so the
            // custom BossBar still appears rather than silently disappearing.
            return true;
        }
    }

    private static long gameTime(Object level) {
        Object value = invokeNoArgValue(level, "getGameTime");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static List<?> players(Object level) {
        Object value = invokeNoArgValue(level, "players");
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private static double distanceSq(Object entity, double x, double y, double z) {
        double dx = readDouble(entity, "getX", x) - x;
        double dy = readDouble(entity, "getY", y) - y;
        double dz = readDouble(entity, "getZ", z) - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double readDouble(Object target, String methodName, double fallback) {
        Object value = invokeNoArgValue(target, methodName);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    private static String playerIdentity(Object player) {
        Object uuid = invokeNoArgValue(player, "getUUID");
        if (uuid != null) {
            return String.valueOf(uuid);
        }
        return player.getClass().getName() + "@" + System.identityHashCode(player);
    }

    private static Object readObject(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
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

    private static Object component(String text) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            return component(componentClass, text);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object component(Class<?> componentClass, String text) throws ReflectiveOperationException {
        Method literal = componentClass.getMethod("literal", String.class);
        return literal.invoke(null, text == null ? "" : text);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }

    private static Object invokeNoArgValue(Object target, String name) {
        try {
            Method method = findNoArgMethod(target.getClass(), name);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeNoArg(Object target, String name) {
        return invokeNoArgValue(target, name) != null || hasNoArgVoidMethod(target, name);
    }

    private static boolean hasNoArgVoidMethod(Object target, String name) {
        try {
            Method method = findNoArgMethod(target.getClass(), name);
            if (method == null) {
                return false;
            }
            method.setAccessible(true);
            method.invoke(target);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean invokeOneArg(Object target, String name, Class<?> primitiveType, Object argument) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(name, primitiveType);
            method.setAccessible(true);
            method.invoke(target, argument);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeBestOneArgValue(Object target, String name, Object argument) {
        if (target == null || argument == null) {
            return null;
        }
        try {
            Method method = findOneArgMethod(target.getClass(), name, argument.getClass());
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(target, argument);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeBestOneArg(Object target, String name, Object argument) {
        if (target == null || argument == null) {
            return false;
        }
        try {
            Method method = findOneArgMethod(target.getClass(), name, argument.getClass());
            if (method == null) {
                return false;
            }
            method.setAccessible(true);
            method.invoke(target, argument);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        synchronized (NO_ARG_METHOD_CACHE) {
            Map<String, Method> byName = NO_ARG_METHOD_CACHE.computeIfAbsent(type, ignored -> new HashMap<>());
            if (byName.containsKey(name)) {
                return byName.get(name);
            }
            Method found = null;
            Class<?> current = type;
            while (current != null && current != Object.class && found == null) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name) && method.getParameterCount() == 0) {
                        found = method;
                        break;
                    }
                }
                current = current.getSuperclass();
            }
            byName.put(name, found);
            return found;
        }
    }

    private static Method findOneArgMethod(Class<?> type, String name, Class<?> argumentType) {
        String key = name + "#" + argumentType.getName();
        synchronized (ONE_ARG_METHOD_CACHE) {
            Map<String, Method> byName = ONE_ARG_METHOD_CACHE.computeIfAbsent(type, ignored -> new HashMap<>());
            if (byName.containsKey(key)) {
                return byName.get(key);
            }
            Method found = null;
            Class<?> current = type;
            while (current != null && current != Object.class && found == null) {
                for (Method method : current.getDeclaredMethods()) {
                    if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                        continue;
                    }
                    Class<?> parameter = method.getParameterTypes()[0];
                    if (parameter.isAssignableFrom(argumentType) || argumentType.isAssignableFrom(parameter)) {
                        found = method;
                        break;
                    }
                }
                current = current.getSuperclass();
            }
            byName.put(key, found);
            return found;
        }
    }

    private static final class ManagedBossbar {
        Object bossEvent;
        String lastTitle = "";
        float lastProgress = -1.0F;
        long createdGameTime;
        long lastSeenGameTime;
        long lastPlayerSyncGameTime;
        int lastWave = -1;
        int waveBaselineRaiders;
        int lastAliveRaiders = -1;
        boolean waveEverSawRaiders;
        long lastNearbyScanGameTime;
        int lastNearbyScanCount = -1;
        int diagPreviousLastWave = -1;
        boolean diagWaveChange;
        boolean diagBaselineReset;
        boolean diagRefillAttempt;
        int diagAlive = -1;
        int diagNativeCount = -1;
        int diagSessionCount = -1;
        int diagNearbyCount = -1;
        String diagCountSource = "uncomputed";
        float diagVanillaProgress = Float.NaN;
        float diagComputedProgress = Float.NaN;
        String diagDecision = "uncomputed";
        final Map<String, Object> players = new LinkedHashMap<>();
    }

    private record CountDiagnostics(int alive, String source, int nativeCount, int sessionCount, int nearbyCount) {
    }

    private record CountResult(boolean found, int count) {
        static CountResult none() {
            return new CountResult(false, 0);
        }
    }
}
