package com.noah.raidenhancement.raid;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 0.8.9.8.8: stable raid bossbar title provider.
 *
 * The previous bossbar UI patch updated the vanilla raid bossbar from the
 * server tick after vanilla had already pushed its plain "Raid" title. In the
 * client this could appear as a visible title flicker. This helper is used by
 * a ServerBossEvent#setName mixin so every vanilla title write for a managed
 * raid is rewritten at the source.
 *
 * This class intentionally uses reflection only. It must never hard-bind to a
 * particular NeoForge/Minecraft descriptor, because this project has already
 * hit several 1.21.1 runtime signature mismatches.
 */
public final class RaidBossbarTitleOverride {
    private static Field statesField;
    private static boolean statesFieldResolved;
    private static final Map<Object, CachedTitle> TITLE_CACHE = new IdentityHashMap<>();
    private static final long CACHE_TTL_NANOS = 1_800_000_000_000L;

    private RaidBossbarTitleOverride() {
    }

    public static String titleForBossEvent(Object bossEvent) {
        if (bossEvent == null) {
            return null;
        }
        try {
            Map<?, ?> states = states();
            if (states != null && !states.isEmpty()) {
                for (Object state : states.values()) {
                    if (state == null || readBoolean(state, "completed", false)) {
                        continue;
                    }
                    Object nativeRaid = readObject(state, "nativeRaid");
                    if (nativeRaid == null) {
                        continue;
                    }
                    Object nativeBossEvent = findServerBossEvent(nativeRaid);
                    if (nativeBossEvent == bossEvent) {
                        String title = titleForState(state);
                        rememberTitle(bossEvent, title);
                        return title;
                    }
                }
            }
            return cachedTitle(bossEvent);
        } catch (Throwable ignored) {
            // Bossbar title is display-only. Fail open and let vanilla use its name.
            return cachedTitle(bossEvent);
        }
    }

    public static String titleForNativeRaid(Object raid) {
        if (raid == null) {
            return null;
        }
        try {
            Map<?, ?> states = states();
            if (states == null || states.isEmpty()) {
                return null;
            }
            for (Object state : states.values()) {
                if (state == null || readBoolean(state, "completed", false)) {
                    continue;
                }
                if (readObject(state, "nativeRaid") == raid) {
                    String title = titleForState(state);
                    Object bossEvent = findServerBossEvent(raid);
                    rememberTitle(bossEvent, title);
                    return title;
                }
            }
        } catch (Throwable ignored) {
            // Display-only.
        }
        return null;
    }

    private static void rememberTitle(Object bossEvent, String title) {
        if (bossEvent == null || title == null || title.isBlank()) {
            return;
        }
        synchronized (TITLE_CACHE) {
            TITLE_CACHE.put(bossEvent, new CachedTitle(title, System.nanoTime()));
        }
    }

    private static String cachedTitle(Object bossEvent) {
        if (bossEvent == null) {
            return null;
        }
        synchronized (TITLE_CACHE) {
            CachedTitle cached = TITLE_CACHE.get(bossEvent);
            if (cached == null) {
                return null;
            }
            long age = System.nanoTime() - cached.nanoTime();
            if (age > CACHE_TTL_NANOS) {
                TITLE_CACHE.remove(bossEvent);
                return null;
            }
            return cached.title();
        }
    }

    private static String titleForState(Object state) {
        int totalWaves = Math.max(1, readInt(state, "logicalTargetWaves", 1));
        int customWavesSpawned = Math.max(0, readInt(state, "customWavesSpawned", 0));
        int activeCustomLogicalWave = readInt(state, "activeCustomLogicalWave", -1);
        boolean currentWaveActive = readBoolean(state, "currentWaveActive", false);
        boolean bridgeHoldActive = readBoolean(state, "bridgeHoldActive", false);
        boolean armedForExtraWaves = readBoolean(state, "armedForExtraWaves", false);
        boolean nativeRaidFinishedObserved = readBoolean(state, "nativeRaidFinishedObserved", false);
        int maxObservedNativeGroupsSpawned = Math.max(0, readInt(state, "maxObservedNativeGroupsSpawned", 0));

        int currentWave;
        if (activeCustomLogicalWave > 0 && (currentWaveActive || customWavesSpawned > 0)) {
            currentWave = activeCustomLogicalWave;
        } else if (customWavesSpawned > 0) {
            currentWave = RaidWaveAuthority.NATIVE_WAVE_LIMIT + customWavesSpawned;
        } else if (bridgeHoldActive || armedForExtraWaves || nativeRaidFinishedObserved) {
            currentWave = Math.max(RaidWaveAuthority.nativeWaveLimitForTarget(totalWaves), maxObservedNativeGroupsSpawned);
        } else {
            currentWave = Math.max(1, maxObservedNativeGroupsSpawned);
        }
        currentWave = clamp(currentWave, 1, totalWaves);

        return RaidBossbarTitleFormatter.format(currentWave, totalWaves,
                RaidWaveAuthority.isCustomWave(currentWave, totalWaves), clearTimerTextForState(state));
    }

    private static String clearTimerTextForState(Object state) {
        int budgetSeconds = readInt(state, "lastWaveBudgetSeconds", 0);
        if (budgetSeconds <= 0) {
            return "";
        }
        if (readBoolean(state, "clearClockPaused", false)) {
            return "暂停";
        }
        long timeoutGameTime = readLong(state, "lastWaveTimeoutGameTime", 0L);
        long gameTime = Math.max(0L, readLong(state, "lastSeenGameTime", 0L));
        if (timeoutGameTime <= 0L || gameTime <= 0L) {
            return "";
        }
        long remainingTicks = timeoutGameTime - gameTime;
        int remainingSeconds = (int) Math.max(0L, (remainingTicks + 19L) / 20L);
        if (remainingSeconds <= 0) {
            return "超时";
        }
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String m = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
        String s = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
        return m + ":" + s;
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

    private static int readInt(Object target, String fieldName, int fallback) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return fallback;
            }
            field.setAccessible(true);
            return field.getInt(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static long readLong(Object target, String fieldName, long fallback) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return fallback;
            }
            field.setAccessible(true);
            return field.getLong(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean readBoolean(Object target, String fieldName, boolean fallback) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return fallback;
            }
            field.setAccessible(true);
            return field.getBoolean(target);
        } catch (Throwable ignored) {
            return fallback;
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

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private record CachedTitle(String title, long nanoTime) {
    }
}
