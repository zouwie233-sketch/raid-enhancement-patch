package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.KeyDiagnosticsConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collection;
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
 * 0.9.1.3: BossBar module boundary audit presenter.
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
    private static final Map<String, VictoryAttachGuard> VICTORY_ATTACH_GUARDS = new LinkedHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> NO_ARG_METHOD_CACHE = new IdentityHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> ONE_ARG_METHOD_CACHE = new IdentityHashMap<>();
    private static final Set<String> PLAYERS_SNAPSHOT = new HashSet<>();

    private static Field statesField;
    private static boolean statesFieldResolved;
    private static boolean warnedCreateFailure;
    private static boolean warnedTickFailure;
    private static boolean warnedVanillaHideFailure;
    private static boolean announced;
    private static boolean warnedAuthorityFileOutput;

    private static final long PLAYER_SYNC_INTERVAL_TICKS = 20L;
    private static final long CLEANUP_GRACE_TICKS = 420L;
    private static final long COMPLETED_VANILLA_SUPPRESS_TICKS = 420L;
    private static final long CLEANUP_AUDIT_INTERVAL_TICKS = 20L;
    private static final long VICTORY_SUPPRESS_MIN_TICKS = 120L;
    private static final long VICTORY_SUPPRESS_STABLE_ZERO_TICKS = 100L;
    private static final long VICTORY_ATTACH_GUARD_TICKS = 1200L;
    private static final long VICTORY_ATTACH_GUARD_MIN_TICKS = 200L;
    private static final long VICTORY_ATTACH_GUARD_STABLE_ZERO_TICKS = 300L;
    private static final long CLIENT_REATTACH_INTERVAL_TICKS = 20L;
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
                System.out.println("[Raid Enhancement Patch] 0.9.1.3 BossBar module boundary audit stage is active. The mod-owned [REP] BossBar path keeps the tested 0.9.1.0 behavior, 0.9.1.2 Key audit fields, dimension-safe cleanup, and same-dimension VictoryBarAttachGuard. This stage adds diagnostic-only BossBar boundary fields and does not change progress math, waveChange, baselineReset, settlement keys, key formats, rewards, raid waves or VillageFavor.");
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
                bar.lastSnapshot = snapshot;
                bar.inactiveSinceGameTime = -1L;
                bar.independentCleanupDone = false;
                bar.cleanupSuppressUntilGameTime = -1L;
                if (bar.bossEvent == null) {
                    bar.bossEvent = createBossEvent(snapshot, gameTime);
                    bar.createdGameTime = gameTime;
                    if (bar.bossEvent != null) {
                        RaidKeyDiagnostics.logBossbar("created", serverLevel, snapshot, gameTime, bar.lastWave,
                                bar.waveBaselineRaiders, bar.lastAliveRaiders, bar.lastProgress);
                        logAuthority("created", serverLevel, snapshot, bar, gameTime, true, "bossbar-created");
                    }
                }
                if (bar.bossEvent == null) {
                    // Do not hide vanilla if the replacement bar could not be built.
                    continue;
                }
                boolean visualRefreshRequired = updateBar(serverLevel, bar, snapshot, gameTime);
                if (visualRefreshRequired
                        || bar.lastPlayerSyncGameTime <= 0L
                        || gameTime - bar.lastPlayerSyncGameTime >= PLAYER_SYNC_INTERVAL_TICKS) {
                    syncPlayers(serverLevel, bar, snapshot, gameTime);
                }
                if (visualRefreshRequired) {
                    forceClientReattach(bar, gameTime);
                }
                if (bar.pendingPostWaveAuditGameTime > 0L
                        && gameTime >= bar.pendingPostWaveAuditGameTime
                        && snapshot.currentWave() == bar.pendingPostWaveAuditWave) {
                    logAuthority("post-wave-next-tick", serverLevel, snapshot, bar, gameTime, true,
                            "verify-progress-after-wave-change");
                    bar.pendingPostWaveAuditGameTime = -1L;
                    bar.pendingPostWaveAuditWave = -1;
                }
                hideVanillaBossbar(snapshot, bar, gameTime);
                bar.lastSeenGameTime = gameTime;
            }
            cleanupInactive(activeKeys, serverLevel, gameTime);
            tickVictoryAttachGuards(serverLevel, gameTime);
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
            Object title = component(componentClass, diagnosticTitle(RaidBossbarTitleFormatter.format(snapshot, gameTime)));
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

    private static boolean updateBar(Object serverLevel, ManagedBossbar bar, RaidEncounterSnapshot snapshot, long gameTime) {
        String title = diagnosticTitle(RaidBossbarTitleFormatter.format(snapshot, gameTime));
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
        if (bar.diagWaveChange) {
            bar.pendingPostWaveAuditGameTime = gameTime + 1L;
            bar.pendingPostWaveAuditWave = snapshot.currentWave();
        }
        logAuthority("update", serverLevel, snapshot, bar, gameTime,
                bar.diagWaveChange || bar.diagRefillAttempt || progressApplied,
                "waveChange=" + bar.diagWaveChange + ",refillAttempt=" + bar.diagRefillAttempt + ",progressApplied=" + progressApplied);
        return bar.diagWaveChange || bar.diagRefillAttempt || progressApplied;
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
        boolean sameWaveUpwardRefill = !bar.diagWaveChange
                && previousDisplayedProgress >= 0.0F
                && progress > previousDisplayedProgress + 0.005F;
        if (sameWaveUpwardRefill) {
            progress = clampProgress(previousDisplayedProgress);
            decision = decision + "+same-wave-refill-suppressed";
        }
        bar.diagComputedProgress = progress;
        bar.diagVanillaProgress = vanillaProgress;
        bar.diagDecision = decision;
        bar.diagRefillAttempt = bar.diagWaveChange
                || previousDisplayedProgress < 0.0F
                || (!sameWaveUpwardRefill && progress > previousDisplayedProgress + 0.005F);
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
            Object knownPlayer = bar.players.get(id);
            if (knownPlayer != null && knownPlayer != player) {
                boolean removedKnown = invokeBestOneArg(bar.bossEvent, "removePlayer", knownPlayer);
                bar.players.remove(id);
                logAuthorityPlayer("remove-known-replaced-player", bar, id, knownPlayer, removedKnown, gameTime);
            }
            if (!bar.players.containsKey(id)) {
                if (invokeBestOneArg(bar.bossEvent, "addPlayer", player)) {
                    bar.players.put(id, player);
                    logAuthorityPlayer("add-player", bar, id, player, true, gameTime);
                } else {
                    logAuthorityPlayer("add-player-failed", bar, id, player, false, gameTime);
                }
            }
        }
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Object> entry : bar.players.entrySet()) {
            if (!PLAYERS_SNAPSHOT.contains(entry.getKey())) {
                boolean removed = invokeBestOneArg(bar.bossEvent, "removePlayer", entry.getValue());
                logAuthorityPlayer("remove-out-of-range-player", bar, entry.getKey(), entry.getValue(), removed, gameTime);
                toRemove.add(entry.getKey());
            }
        }
        for (String id : toRemove) {
            bar.players.remove(id);
        }
        bar.lastPlayerSyncGameTime = gameTime;
    }

    /**
     * 0.9.0.4 display-layer hotfix: when progress refills or a new wave starts,
     * force a remove/add cycle for players already attached to the mod-owned
     * ServerBossEvent. Diagnostics from 0.9.0.3 showed setProgress succeeded on
     * the server object while the client-visible bar stayed stale, so this is a
     * presentation sync repair only; it does not change wave math, raider counts,
     * settlement keys, rewards or raid flow.
     */
    private static void forceClientReattach(ManagedBossbar bar, long gameTime) {
        if (bar == null || bar.bossEvent == null || bar.players.isEmpty()) {
            return;
        }
        if (bar.lastClientReattachGameTime > 0L
                && gameTime - bar.lastClientReattachGameTime < CLIENT_REATTACH_INTERVAL_TICKS
                && !bar.diagWaveChange) {
            return;
        }
        List<Object> players = new ArrayList<>(bar.players.values());
        for (Object player : players) {
            if (player == null) {
                continue;
            }
            boolean removed = invokeBestOneArg(bar.bossEvent, "removePlayer", player);
            boolean added = invokeBestOneArg(bar.bossEvent, "addPlayer", player);
            logAuthorityPlayer("client-reattach-remove-add", bar, playerIdentity(player), player, removed && added, gameTime);
        }
        invokeOneArg(bar.bossEvent, "setVisible", boolean.class, true);
        bar.lastClientReattachGameTime = gameTime;
    }


    private static String diagnosticTitle(String title) {
        String base = title == null ? "" : title;
        if (!KeyDiagnosticsConfig.ENABLE_TEMPORARY_REP_BOSSBAR_TITLE_MARKER) {
            return base;
        }
        return base.startsWith("[REP]") ? base : "[REP] " + base;
    }

    private static void logAuthority(String phase, Object serverLevel, RaidEncounterSnapshot snapshot,
                                     ManagedBossbar bar, long gameTime, boolean critical, String note) {
        if (!KeyDiagnosticsConfig.ENABLED
                || !KeyDiagnosticsConfig.LOG_BOSSBAR
                || !KeyDiagnosticsConfig.ENABLE_BOSSBAR_VISIBLE_AUTHORITY_AUDIT) {
            return;
        }
        if (!critical && bar != null && bar.lastAuthorityAuditGameTime > 0L
                && gameTime - bar.lastAuthorityAuditGameTime < Math.max(20L, KeyDiagnosticsConfig.BOSSBAR_VISIBLE_AUDIT_INTERVAL_TICKS)) {
            return;
        }
        if (bar != null) {
            bar.lastAuthorityAuditGameTime = gameTime;
        }
        Object independent = bar == null ? null : bar.bossEvent;
        Object vanilla = vanillaBossEvent(snapshot == null ? null : snapshot.key());
        String line = "[Raid Enhancement Patch][KeyDiag][BossBarAuthorityAudit] "
                + "phase=" + safeText(phase)
                + " version=0.9.1.3-bossbar-module-boundary-alpha"
                + BossBarAuditLogger.commonBoundaryFields("BossBarAuthorityAudit")
                + " dimensionId=" + safeText(snapshot == null ? null : snapshot.dimensionId())
                + " center=" + (snapshot == null ? "null" : snapshot.centerX() + "," + snapshot.centerY() + "," + snapshot.centerZ())
                + " snapshot.key=" + safeText(snapshot == null ? null : snapshot.key())
                + " wave=" + (snapshot == null ? -1 : snapshot.currentWave())
                + " totalWaves=" + (snapshot == null ? -1 : snapshot.totalWaves())
                + " markerEnabled=" + KeyDiagnosticsConfig.ENABLE_TEMPORARY_REP_BOSSBAR_TITLE_MARKER
                + " visibleAuthorityQuestion=does-player-see-[REP]"
                + " independentIdentity=" + bossEventIdentity(independent)
                + " independentTitle=" + safeText(bossEventTitle(independent))
                + " independentProgress=" + progressText(readBossEventProgressOrNaN(independent))
                + " independentVisible=" + bossEventVisible(independent)
                + " independentPlayerCount=" + bossEventPlayerCount(independent)
                + " independentTrackedPlayerCount=" + (bar == null ? -1 : bar.players.size())
                + " independentTrackedPlayers=" + trackedPlayerSummary(bar)
                + " vanillaIdentity=" + bossEventIdentity(vanilla)
                + " vanillaTitle=" + safeText(bossEventTitle(vanilla))
                + " vanillaProgress=" + progressText(readBossEventProgressOrNaN(vanilla))
                + " vanillaVisible=" + bossEventVisible(vanilla)
                + " vanillaPlayerCount=" + bossEventPlayerCount(vanilla)
                + " barLastWave=" + (bar == null ? -1 : bar.lastWave)
                + " barLastProgress=" + (bar == null ? "nan" : progressText(bar.lastProgress))
                + " diagWaveChange=" + (bar != null && bar.diagWaveChange)
                + " diagRefillAttempt=" + (bar != null && bar.diagRefillAttempt)
                + " gameTime=" + gameTime
                + " levelDimension=" + dimensionIdForAudit(serverLevel)
                + " note=" + safeText(note) + "; boundarySummary=" + BossBarAuditLogger.boundarySummary() + ".";
        System.out.println(line);
        appendAuthorityDiagnosticFile(line);
    }

    private static void logAuthorityPlayer(String phase, ManagedBossbar bar, String id, Object player, boolean success, long gameTime) {
        if (!KeyDiagnosticsConfig.ENABLED
                || !KeyDiagnosticsConfig.LOG_BOSSBAR
                || !KeyDiagnosticsConfig.ENABLE_BOSSBAR_VISIBLE_AUTHORITY_AUDIT) {
            return;
        }
        String line = "[Raid Enhancement Patch][KeyDiag][BossBarAuthorityAudit] "
                + "phase=" + safeText(phase)
                + " version=0.9.1.3-bossbar-module-boundary-alpha"
                + BossBarAuditLogger.commonBoundaryFields("BossBarPlayerBindingAudit")
                + " playerKey=" + safeText(id)
                + " player=" + safeText(playerDisplay(player))
                + " success=" + success
                + " independentIdentity=" + bossEventIdentity(bar == null ? null : bar.bossEvent)
                + " independentPlayerCount=" + bossEventPlayerCount(bar == null ? null : bar.bossEvent)
                + " independentTrackedPlayerCount=" + (bar == null ? -1 : bar.players.size())
                + " gameTime=" + gameTime
                + " note=player-binding-audit.";
        System.out.println(line);
        appendAuthorityDiagnosticFile(line);
    }

    private static void appendAuthorityDiagnosticFile(String line) {
        try {
            Path dir = KeyDiagnosticsConfig.configDir();
            Files.createDirectories(dir);
            Path file = dir.resolve("key_diagnostics.log");
            Files.writeString(file, Instant.now() + " " + line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (Throwable throwable) {
            if (!warnedAuthorityFileOutput) {
                warnedAuthorityFileOutput = true;
                System.out.println("[Raid Enhancement Patch] BossBar authority audit file output failed once: " + throwable);
            }
        }
    }

    private static Object vanillaBossEvent(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            Object state = stateByKey(key);
            Object nativeRaid = state == null ? null : readObject(state, "nativeRaid");
            return findServerBossEvent(nativeRaid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float readBossEventProgressOrNaN(Object bossEvent) {
        Float value = readBossEventProgress(bossEvent);
        return value == null ? Float.NaN : value;
    }

    private static String bossEventTitle(Object bossEvent) {
        if (bossEvent == null) {
            return "null";
        }
        Object value = invokeNoArgValue(bossEvent, "getName");
        if (value == null) {
            value = invokeNoArgValue(bossEvent, "getDisplayName");
        }
        return value == null ? "unknown" : String.valueOf(value);
    }

    private static boolean bossEventVisible(Object bossEvent) {
        if (bossEvent == null) {
            return false;
        }
        Object value = invokeNoArgValue(bossEvent, "isVisible");
        if (value instanceof Boolean b) {
            return b;
        }
        value = invokeNoArgValue(bossEvent, "getVisible");
        if (value instanceof Boolean b) {
            return b;
        }
        Boolean reflected = readBooleanField(bossEvent, "visible");
        return reflected != null && reflected;
    }

    private static Boolean readBooleanField(Object target, String expectedNamePart) {
        if (target == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    Class<?> type = field.getType();
                    if (type != boolean.class && type != Boolean.class) {
                        continue;
                    }
                    String name = field.getName() == null ? "" : field.getName().toLowerCase(java.util.Locale.ROOT);
                    if (expectedNamePart != null && !name.contains(expectedNamePart.toLowerCase(java.util.Locale.ROOT))) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof Boolean b) {
                        return b;
                    }
                } catch (Throwable ignored) {
                    // Try next field.
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static int bossEventPlayerCount(Object bossEvent) {
        if (bossEvent == null) {
            return -1;
        }
        Object value = invokeNoArgValue(bossEvent, "getPlayers");
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        Integer reflected = reflectedPlayerCollectionSize(bossEvent);
        return reflected == null ? -1 : reflected;
    }

    private static Integer reflectedPlayerCollectionSize(Object target) {
        if (target == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof Collection<?> collection && looksLikePlayerCollection(collection)) {
                        return collection.size();
                    }
                } catch (Throwable ignored) {
                    // Try next field.
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean looksLikePlayerCollection(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        int checked = 0;
        for (Object element : collection) {
            if (element == null) {
                continue;
            }
            String name = element.getClass().getName();
            if (name.contains("ServerPlayer") || name.contains("Player") || invokeNoArgValue(element, "getUUID") != null) {
                return true;
            }
            if (++checked >= 3) {
                break;
            }
        }
        return false;
    }

    private static String bossEventIdentity(Object bossEvent) {
        return bossEvent == null ? "null" : bossEvent.getClass().getName() + "@" + System.identityHashCode(bossEvent);
    }

    private static String trackedPlayerSummary(ManagedBossbar bar) {
        if (bar == null || bar.players.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        int count = 0;
        for (String key : bar.players.keySet()) {
            if (count > 0) {
                builder.append(',');
            }
            if (count >= 3) {
                builder.append("...+").append(bar.players.size() - count).append(" more");
                break;
            }
            builder.append(key);
            count++;
        }
        builder.append(']');
        return builder.toString();
    }

    private static String playerDisplay(Object player) {
        if (player == null) {
            return "null";
        }
        Object name = invokeNoArgValue(player, "getGameProfile");
        if (name != null) {
            return String.valueOf(name);
        }
        name = invokeNoArgValue(player, "getName");
        if (name != null) {
            return String.valueOf(name);
        }
        return player.getClass().getName() + "@" + System.identityHashCode(player);
    }

    private static String safeText(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace('\n', '_').replace('\r', '_');
    }


    private static String progressText(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "nan";
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private static String dimensionIdForAudit(Object level) {
        if (level == null) {
            return "null";
        }
        try {
            Object dimension = invokeNoArgValue(level, "dimension");
            Object location = dimension == null ? null : invokeNoArgValue(dimension, "location");
            return location == null ? String.valueOf(dimension) : String.valueOf(location);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static boolean sameDimensionForCleanup(Object serverLevel, RaidEncounterSnapshot snapshot) {
        if (serverLevel == null || snapshot == null) {
            return true;
        }
        String dimensionId = snapshot.dimensionId();
        if (dimensionId == null || dimensionId.isBlank()) {
            return true;
        }
        return sameDimension(serverLevel, dimensionId);
    }

    private static void cleanupInactive(Set<String> activeKeys, Object serverLevel, long gameTime) {
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, ManagedBossbar> entry : BARS.entrySet()) {
            String key = entry.getKey();
            ManagedBossbar bar = entry.getValue();
            if (activeKeys.contains(key)) {
                continue;
            }
            if (bar == null) {
                remove.add(key);
                continue;
            }
            RaidEncounterSnapshot snapshot = bar.lastSnapshot;
            if (!sameDimensionForCleanup(serverLevel, snapshot)) {
                logCleanup("skipped-different-dimension", serverLevel, key, snapshot, bar, gameTime, false,
                        "cleanupInactive skipped; bossbarDimension=" + safeText(snapshot == null ? null : snapshot.dimensionId())
                                + "; levelDimension=" + safeText(dimensionIdForAudit(serverLevel))
                                + "; no inactive cleanup and no vanilla suppress outside owning dimension");
                continue;
            }
            if (bar.inactiveSinceGameTime <= 0L) {
                bar.inactiveSinceGameTime = gameTime;
                bar.cleanupSuppressUntilGameTime = gameTime + COMPLETED_VANILLA_SUPPRESS_TICKS;
                bar.vanillaVictoryStableZeroSinceGameTime = -1L;
                bar.lastVanillaVictorySeenGameTime = -1L;
                bar.vanillaVictorySuppressCount = 0;
                bar.vanillaVictoryReappearedCount = 0;
                logCleanup("inactive-detected", serverLevel, key, snapshot, bar, gameTime, true,
                        "snapshot-removed-or-raid-completed;start-final-victory-suppress-window;windowTicks="
                                + COMPLETED_VANILLA_SUPPRESS_TICKS);
                logCleanup("final-victory-suppress-window-start", serverLevel, key, snapshot, bar, gameTime, true,
                        "same-dimension completion cleanup started; minTicks=" + VICTORY_SUPPRESS_MIN_TICKS
                                + "; stableZeroTicks=" + VICTORY_SUPPRESS_STABLE_ZERO_TICKS
                                + "; suppressUntil=" + bar.cleanupSuppressUntilGameTime);
                startVictoryAttachGuard(serverLevel, key, snapshot, gameTime,
                        "completion-window-start; guardTicks=" + VICTORY_ATTACH_GUARD_TICKS);
            }
            if (!bar.independentCleanupDone) {
                removeAllPlayers(bar.bossEvent, bar.players);
                bar.independentCleanupDone = true;
                logCleanup("remove-independent-completed", serverLevel, key, snapshot, bar, gameTime, true,
                        "removed-[REP]-bossbar-players-and-set-visible-false");
            }

            SuppressResult result = suppressVanillaForCleanup(serverLevel, key, snapshot, bar, gameTime,
                    "final-victory-bar-suppress");
            if (result.victoryBar()) {
                bar.vanillaVictorySuppressCount++;
                if (result.beforeVisible() || result.beforePlayers() > 0) {
                    bar.lastVanillaVictorySeenGameTime = gameTime;
                    bar.vanillaVictoryReappearedCount++;
                    bar.vanillaVictoryStableZeroSinceGameTime = -1L;
                    logCleanup("vanilla-victory-reappeared", serverLevel, key, snapshot, bar, gameTime, true,
                            "vanilla victory BossBar rebound during completion window; beforeVisible="
                                    + result.beforeVisible() + "; beforePlayers=" + result.beforePlayers()
                                    + "; suppressCount=" + bar.vanillaVictorySuppressCount
                                    + "; reappearedCount=" + bar.vanillaVictoryReappearedCount);
                } else if (!result.afterVisible() && result.afterPlayers() == 0) {
                    if (bar.vanillaVictoryStableZeroSinceGameTime <= 0L) {
                        bar.vanillaVictoryStableZeroSinceGameTime = gameTime;
                    }
                } else {
                    bar.vanillaVictoryStableZeroSinceGameTime = -1L;
                }
            }

            boolean minWindowElapsed = gameTime - bar.inactiveSinceGameTime >= VICTORY_SUPPRESS_MIN_TICKS;
            boolean stableZeroElapsed = bar.vanillaVictoryStableZeroSinceGameTime > 0L
                    && gameTime - bar.vanillaVictoryStableZeroSinceGameTime >= VICTORY_SUPPRESS_STABLE_ZERO_TICKS;
            boolean hardWindowElapsed = gameTime >= bar.cleanupSuppressUntilGameTime;
            if ((minWindowElapsed && stableZeroElapsed) || hardWindowElapsed) {
                SuppressResult finalResult = suppressVanillaForCleanup(serverLevel, key, snapshot, bar, gameTime,
                        hardWindowElapsed ? "final-victory-hard-window-end" : "cleanup-stable-zero-confirmed");
                logCleanup(stableZeroElapsed ? "cleanup-stable-zero-confirmed" : "cleanup-window-hard-ended",
                        serverLevel, key, snapshot, bar, gameTime, true,
                        "ending final victory suppress window; suppressCount=" + bar.vanillaVictorySuppressCount
                                + "; reappearedCount=" + bar.vanillaVictoryReappearedCount
                                + "; lastVictorySeen=" + bar.lastVanillaVictorySeenGameTime
                                + "; stableZeroSince=" + bar.vanillaVictoryStableZeroSinceGameTime
                                + "; finalBeforeVisible=" + finalResult.beforeVisible()
                                + "; finalBeforePlayers=" + finalResult.beforePlayers()
                                + "; finalAfterVisible=" + finalResult.afterVisible()
                                + "; finalAfterPlayers=" + finalResult.afterPlayers());
                logCleanup("final-victory-suppress-summary", serverLevel, key, snapshot, bar, gameTime, true,
                        "summary; suppressCount=" + bar.vanillaVictorySuppressCount
                                + "; reappearedCount=" + bar.vanillaVictoryReappearedCount
                                + "; windowTicks=" + (gameTime - bar.inactiveSinceGameTime)
                                + "; hardWindowElapsed=" + hardWindowElapsed
                                + "; stableZeroElapsed=" + stableZeroElapsed);
                startVictoryAttachGuard(serverLevel, key, snapshot, gameTime,
                        "managed-entry-finished; guard-continues-after-BARS-remove");
                remove.add(key);
            }
        }
        for (String key : remove) {
            BARS.remove(key);
        }
    }


    private static void startVictoryAttachGuard(Object serverLevel, String key, RaidEncounterSnapshot snapshot,
                                                long gameTime, String note) {
        if (key == null || key.isBlank() || snapshot == null) {
            return;
        }
        if (!sameDimensionForCleanup(serverLevel, snapshot)) {
            logVictoryAttachGuard("victory-attach-guard-skipped-different-dimension", serverLevel, key, snapshot,
                    VICTORY_ATTACH_GUARDS.get(key), gameTime, false,
                    "start skipped; bossbarDimension=" + safeText(snapshot.dimensionId())
                            + "; levelDimension=" + safeText(dimensionIdForAudit(serverLevel))
                            + "; note=" + safeText(note));
            return;
        }
        VictoryAttachGuard guard = VICTORY_ATTACH_GUARDS.computeIfAbsent(key, ignored -> new VictoryAttachGuard());
        if (guard.startedGameTime <= 0L) {
            guard.startedGameTime = gameTime;
        }
        guard.key = key;
        guard.snapshot = snapshot;
        guard.dimensionId = snapshot.dimensionId();
        guard.guardUntilGameTime = Math.max(guard.guardUntilGameTime, gameTime + VICTORY_ATTACH_GUARD_TICKS);
        Object latestBossEvent = vanillaBossEvent(key);
        if (latestBossEvent != null) {
            guard.vanillaBossEvent = latestBossEvent;
        }
        logVictoryAttachGuard("victory-attach-guard-start", serverLevel, key, snapshot, guard, gameTime, true,
                safeText(note) + "; guardUntil=" + guard.guardUntilGameTime
                        + "; guardMinTicks=" + VICTORY_ATTACH_GUARD_MIN_TICKS
                        + "; stableZeroTicks=" + VICTORY_ATTACH_GUARD_STABLE_ZERO_TICKS);
    }

    private static void tickVictoryAttachGuards(Object serverLevel, long gameTime) {
        if (VICTORY_ATTACH_GUARDS.isEmpty()) {
            return;
        }
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, VictoryAttachGuard> entry : VICTORY_ATTACH_GUARDS.entrySet()) {
            String key = entry.getKey();
            VictoryAttachGuard guard = entry.getValue();
            RaidEncounterSnapshot snapshot = guard == null ? null : guard.snapshot;
            if (guard == null || snapshot == null) {
                remove.add(key);
                continue;
            }
            if (!sameDimensionForCleanup(serverLevel, snapshot)) {
                logVictoryAttachGuard("victory-attach-guard-skipped-different-dimension", serverLevel, key, snapshot,
                        guard, gameTime, false,
                        "tick skipped; bossbarDimension=" + safeText(snapshot.dimensionId())
                                + "; levelDimension=" + safeText(dimensionIdForAudit(serverLevel))
                                + "; no attach guard outside owning dimension");
                continue;
            }
            Object latestBossEvent = vanillaBossEvent(key);
            if (latestBossEvent != null) {
                guard.vanillaBossEvent = latestBossEvent;
            }
            Object bossEvent = guard.vanillaBossEvent;
            String title = bossEventTitle(bossEvent);
            boolean victoryBar = isVanillaVictoryBossbar(title);
            boolean beforeVisible = bossEventVisible(bossEvent);
            int beforePlayers = bossEventPlayerCount(bossEvent);
            float beforeProgress = readBossEventProgressOrNaN(bossEvent);
            boolean hidden = false;
            boolean removedPlayers = false;
            if (bossEvent != null && victoryBar) {
                hidden = invokeOneArg(bossEvent, "setVisible", boolean.class, false);
                removedPlayers = invokeNoArg(bossEvent, "removeAllPlayers");
                if (!hidden && !removedPlayers) {
                    invokeOneArg(bossEvent, "setVisible", boolean.class, false);
                }
            }
            boolean afterVisible = bossEventVisible(bossEvent);
            int afterPlayers = bossEventPlayerCount(bossEvent);
            if (victoryBar) {
                guard.suppressCount++;
                if (beforeVisible || beforePlayers > 0) {
                    guard.reappearedCount++;
                    guard.lastReappearedGameTime = gameTime;
                    guard.stableZeroSinceGameTime = -1L;
                    logVictoryAttachGuard("victory-bar-attach-blocked", serverLevel, key, snapshot, guard, gameTime, true,
                            "vanilla victory BossBar attempted to reattach/show; beforeVisible=" + beforeVisible
                                    + "; beforePlayers=" + beforePlayers
                                    + "; beforeProgress=" + progressText(beforeProgress)
                                    + "; setVisibleFalse=" + hidden
                                    + "; removeAllPlayers=" + removedPlayers
                                    + "; afterVisible=" + afterVisible
                                    + "; afterPlayers=" + afterPlayers);
                } else if (!afterVisible && afterPlayers == 0) {
                    if (guard.stableZeroSinceGameTime <= 0L) {
                        guard.stableZeroSinceGameTime = gameTime;
                    }
                    if (guard.lastAuditGameTime <= 0L || gameTime - guard.lastAuditGameTime >= CLEANUP_AUDIT_INTERVAL_TICKS) {
                        logVictoryAttachGuard("victory-attach-guard-stable-zero", serverLevel, key, snapshot, guard, gameTime, false,
                                "victory bar still suppressed; suppressCount=" + guard.suppressCount
                                        + "; stableZeroSince=" + guard.stableZeroSinceGameTime
                                        + "; afterVisible=" + afterVisible
                                        + "; afterPlayers=" + afterPlayers);
                    }
                } else {
                    guard.stableZeroSinceGameTime = -1L;
                }
            }
            boolean minElapsed = gameTime - guard.startedGameTime >= VICTORY_ATTACH_GUARD_MIN_TICKS;
            boolean stableElapsed = guard.stableZeroSinceGameTime > 0L
                    && gameTime - guard.stableZeroSinceGameTime >= VICTORY_ATTACH_GUARD_STABLE_ZERO_TICKS;
            boolean hardElapsed = gameTime >= guard.guardUntilGameTime;
            if ((minElapsed && stableElapsed) || hardElapsed) {
                logVictoryAttachGuard("victory-attach-guard-summary", serverLevel, key, snapshot, guard, gameTime, true,
                        "ending attach guard; suppressCount=" + guard.suppressCount
                                + "; reappearedCount=" + guard.reappearedCount
                                + "; lastReappeared=" + guard.lastReappearedGameTime
                                + "; stableZeroSince=" + guard.stableZeroSinceGameTime
                                + "; guardTicks=" + (gameTime - guard.startedGameTime)
                                + "; hardElapsed=" + hardElapsed
                                + "; stableElapsed=" + stableElapsed
                                + "; finalVisible=" + afterVisible
                                + "; finalPlayers=" + afterPlayers);
                remove.add(key);
            }
        }
        for (String key : remove) {
            VICTORY_ATTACH_GUARDS.remove(key);
        }
    }

    private static void logVictoryAttachGuard(String phase, Object serverLevel, String key, RaidEncounterSnapshot snapshot,
                                             VictoryAttachGuard guard, long gameTime, boolean critical, String note) {
        if (!KeyDiagnosticsConfig.ENABLED
                || !KeyDiagnosticsConfig.LOG_BOSSBAR
                || !KeyDiagnosticsConfig.ENABLE_BOSSBAR_VISIBLE_AUTHORITY_AUDIT) {
            return;
        }
        if (!critical && guard != null && guard.lastAuditGameTime > 0L
                && gameTime - guard.lastAuditGameTime < CLEANUP_AUDIT_INTERVAL_TICKS) {
            return;
        }
        if (guard != null) {
            guard.lastAuditGameTime = gameTime;
        }
        Object vanilla = guard == null ? vanillaBossEvent(key) : (guard.vanillaBossEvent != null ? guard.vanillaBossEvent : vanillaBossEvent(key));
        String line = "[Raid Enhancement Patch][KeyDiag][VictoryBarAttachGuard] "
                + "phase=" + safeText(phase)
                + " version=0.9.1.3-bossbar-module-boundary-alpha"
                + BossBarAuditLogger.commonBoundaryFields("VictoryBarAttachGuard")
                + " dimensionId=" + safeText(snapshot == null ? null : snapshot.dimensionId())
                + " center=" + (snapshot == null ? "null" : snapshot.centerX() + "," + snapshot.centerY() + "," + snapshot.centerZ())
                + " snapshot.key=" + safeText(key)
                + " vanillaIdentity=" + bossEventIdentity(vanilla)
                + " vanillaTitle=" + safeText(bossEventTitle(vanilla))
                + " vanillaProgress=" + progressText(readBossEventProgressOrNaN(vanilla))
                + " vanillaVisible=" + bossEventVisible(vanilla)
                + " vanillaPlayerCount=" + bossEventPlayerCount(vanilla)
                + " started=" + (guard == null ? -1 : guard.startedGameTime)
                + " guardUntil=" + (guard == null ? -1 : guard.guardUntilGameTime)
                + " suppressCount=" + (guard == null ? -1 : guard.suppressCount)
                + " reappearedCount=" + (guard == null ? -1 : guard.reappearedCount)
                + " stableZeroSince=" + (guard == null ? -1 : guard.stableZeroSinceGameTime)
                + " gameTime=" + gameTime
                + " levelDimension=" + dimensionIdForAudit(serverLevel)
                + " note=" + safeText(note) + ".";
        System.out.println(line);
        appendAuthorityDiagnosticFile(line);
    }

    private static SuppressResult suppressVanillaForCleanup(Object serverLevel, String key, RaidEncounterSnapshot snapshot,
                                                           ManagedBossbar bar, long gameTime, String note) {
        if (!sameDimensionForCleanup(serverLevel, snapshot)) {
            logCleanup("skipped-different-dimension", serverLevel, key, snapshot, bar, gameTime, false,
                    "suppressVanillaForCleanup skipped; bossbarDimension=" + safeText(snapshot == null ? null : snapshot.dimensionId())
                            + "; levelDimension=" + safeText(dimensionIdForAudit(serverLevel))
                            + "; note=" + safeText(note));
            return SuppressResult.skipped();
        }
        Object bossEvent = vanillaBossEvent(key);
        int beforePlayers = bossEventPlayerCount(bossEvent);
        float beforeProgress = readBossEventProgressOrNaN(bossEvent);
        boolean beforeVisible = bossEventVisible(bossEvent);
        String title = bossEventTitle(bossEvent);
        boolean victoryBar = isVanillaVictoryBossbar(title);
        boolean hidden = false;
        boolean removed = false;
        if (bossEvent != null && victoryBar) {
            hidden = invokeOneArg(bossEvent, "setVisible", boolean.class, false);
            removed = invokeNoArg(bossEvent, "removeAllPlayers");
            if (!hidden && !removed) {
                invokeOneArg(bossEvent, "setVisible", boolean.class, false);
            }
        }
        boolean afterVisible = bossEventVisible(bossEvent);
        int afterPlayers = bossEventPlayerCount(bossEvent);
        boolean shouldLog = "cleanup-stable-zero-confirmed".equals(note)
                || "final-victory-hard-window-end".equals(note)
                || (victoryBar && (beforeVisible || beforePlayers > 0))
                || (bar != null && (bar.lastVanillaSuppressGameTime <= 0L
                        || gameTime - bar.lastVanillaSuppressGameTime >= CLEANUP_AUDIT_INTERVAL_TICKS));
        if (shouldLog) {
            logCleanup("final-victory-bar-suppress", serverLevel, key, snapshot, bar, gameTime, true,
                    "vanillaIdentity=" + bossEventIdentity(bossEvent)
                            + ",vanillaTitle=" + safeText(title)
                            + ",victoryBar=" + victoryBar
                            + ",beforeVisible=" + beforeVisible
                            + ",beforeProgress=" + progressText(beforeProgress)
                            + ",beforePlayers=" + beforePlayers
                            + ",setVisibleFalse=" + hidden
                            + ",removeAllPlayers=" + removed
                            + ",afterVisible=" + afterVisible
                            + ",afterPlayers=" + afterPlayers
                            + ",note=" + note);
            if (bar != null) {
                bar.lastVanillaSuppressGameTime = gameTime;
            }
        }
        return new SuppressResult(victoryBar, beforeVisible, beforePlayers, afterVisible, afterPlayers);
    }

    private static boolean isVanillaVictoryBossbar(String title) {
        if (title == null) {
            return false;
        }
        String normalized = title.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("event.minecraft.raid.victory")
                || normalized.contains("raid.victory")
                || normalized.contains("victory.full");
    }

    private static void logCleanup(String phase, Object serverLevel, String key, RaidEncounterSnapshot snapshot,
                                   ManagedBossbar bar, long gameTime, boolean critical, String note) {
        if (!KeyDiagnosticsConfig.ENABLED
                || !KeyDiagnosticsConfig.LOG_BOSSBAR
                || !KeyDiagnosticsConfig.ENABLE_BOSSBAR_VISIBLE_AUTHORITY_AUDIT) {
            return;
        }
        if (!critical && bar != null && bar.lastCleanupAuditGameTime > 0L
                && gameTime - bar.lastCleanupAuditGameTime < CLEANUP_AUDIT_INTERVAL_TICKS) {
            return;
        }
        if (bar != null) {
            bar.lastCleanupAuditGameTime = gameTime;
        }
        Object independent = bar == null ? null : bar.bossEvent;
        Object vanilla = vanillaBossEvent(key);
        String line = "[Raid Enhancement Patch][KeyDiag][BossBarCleanupAudit] "
                + "phase=" + safeText(phase)
                + " version=0.9.1.3-bossbar-module-boundary-alpha"
                + BossBarAuditLogger.commonBoundaryFields("BossBarCleanupAudit")
                + " dimensionId=" + safeText(snapshot == null ? null : snapshot.dimensionId())
                + " center=" + (snapshot == null ? "null" : snapshot.centerX() + "," + snapshot.centerY() + "," + snapshot.centerZ())
                + " snapshot.key=" + safeText(key)
                + " wave=" + (snapshot == null ? -1 : snapshot.currentWave())
                + " totalWaves=" + (snapshot == null ? -1 : snapshot.totalWaves())
                + " independentIdentity=" + bossEventIdentity(independent)
                + " independentTitle=" + safeText(bossEventTitle(independent))
                + " independentProgress=" + progressText(readBossEventProgressOrNaN(independent))
                + " independentVisible=" + bossEventVisible(independent)
                + " independentPlayerCount=" + bossEventPlayerCount(independent)
                + " independentTrackedPlayerCount=" + (bar == null ? -1 : bar.players.size())
                + " vanillaIdentity=" + bossEventIdentity(vanilla)
                + " vanillaTitle=" + safeText(bossEventTitle(vanilla))
                + " vanillaProgress=" + progressText(readBossEventProgressOrNaN(vanilla))
                + " vanillaVisible=" + bossEventVisible(vanilla)
                + " vanillaPlayerCount=" + bossEventPlayerCount(vanilla)
                + " inactiveSince=" + (bar == null ? -1 : bar.inactiveSinceGameTime)
                + " suppressUntil=" + (bar == null ? -1 : bar.cleanupSuppressUntilGameTime)
                + " gameTime=" + gameTime
                + " levelDimension=" + dimensionIdForAudit(serverLevel)
                + " note=" + safeText(note) + ".";
        System.out.println(line);
        appendAuthorityDiagnosticFile(line);
    }

    private static void hideVanillaBossbar(RaidEncounterSnapshot snapshot, ManagedBossbar bar, long gameTime) {
        String key = snapshot == null ? null : snapshot.key();
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
            int beforePlayers = bossEventPlayerCount(bossEvent);
            float beforeProgress = readBossEventProgress(bossEvent) == null ? Float.NaN : readBossEventProgress(bossEvent);
            boolean beforeVisible = bossEventVisible(bossEvent);
            boolean hidden = invokeOneArg(bossEvent, "setVisible", boolean.class, false);
            // 0.9.0.4 baseline behavior retained for diagnosis: do not leave clients bound
            // to the original Raid BossBar after it is hidden. 0.9.0.5 does not add a
            // second-layer reattach/sync fix; it records whether this object still has players.
            boolean removed = invokeNoArg(bossEvent, "removeAllPlayers");
            if (!hidden && !removed) {
                invokeOneArg(bossEvent, "setVisible", boolean.class, false);
            }
            logAuthority("hide-vanilla", null, snapshot, bar, gameTime, true,
                    "vanillaIdentity=" + bossEventIdentity(bossEvent)
                            + ",beforeVisible=" + beforeVisible
                            + ",beforeProgress=" + progressText(beforeProgress)
                            + ",beforePlayers=" + beforePlayers
                            + ",setVisibleFalse=" + hidden
                            + ",removeAllPlayers=" + removed
                            + ",afterVisible=" + bossEventVisible(bossEvent)
                            + ",afterPlayers=" + bossEventPlayerCount(bossEvent));
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


    private static final class VictoryAttachGuard {
        String key;
        String dimensionId;
        RaidEncounterSnapshot snapshot;
        Object vanillaBossEvent;
        long startedGameTime = -1L;
        long guardUntilGameTime = -1L;
        long stableZeroSinceGameTime = -1L;
        long lastReappearedGameTime = -1L;
        long lastAuditGameTime = -1L;
        int suppressCount;
        int reappearedCount;
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
        long lastClientReattachGameTime;
        long lastAuthorityAuditGameTime = -1L;
        long pendingPostWaveAuditGameTime = -1L;
        int pendingPostWaveAuditWave = -1;
        RaidEncounterSnapshot lastSnapshot;
        long inactiveSinceGameTime = -1L;
        long cleanupSuppressUntilGameTime = -1L;
        long lastVanillaSuppressGameTime = -1L;
        long lastCleanupAuditGameTime = -1L;
        boolean independentCleanupDone;
        long vanillaVictoryStableZeroSinceGameTime = -1L;
        long lastVanillaVictorySeenGameTime = -1L;
        int vanillaVictorySuppressCount;
        int vanillaVictoryReappearedCount;
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

    private record SuppressResult(boolean victoryBar, boolean beforeVisible, int beforePlayers, boolean afterVisible, int afterPlayers) {
        static SuppressResult skipped() {
            return new SuppressResult(false, false, -1, false, -1);
        }
    }

    private record CountDiagnostics(int alive, String source, int nativeCount, int sessionCount, int nearbyCount) {
    }

    private record CountResult(boolean found, int count) {
        static CountResult none() {
            return new CountResult(false, 0);
        }
    }
}
