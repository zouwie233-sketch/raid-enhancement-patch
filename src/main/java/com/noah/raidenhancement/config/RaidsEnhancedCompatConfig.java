package com.noah.raidenhancement.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Compatibility switches for optional Raids Enhanced behavior.
 *
 * 0.8.9.7.20: keeps the successful block rollback guard and hardens drop cleanup.
 * The cleanup query now uses stronger runtime entity lookup and a slightly wider short-lived
 * restored-block cleanup area to catch block drops that spawn or bounce outside the block center.
 */
public final class RaidsEnhancedCompatConfig {
    private static final Path CONFIG_DIR = Path.of("config", "raid_enhancement_patch");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("raids_enhanced_compat.properties");
    private static volatile boolean loaded = false;

    private RaidsEnhancedCompatConfig() {
    }

    public static boolean GOLEM_OF_LAST_RESORT_BLOCK_BREAKING_ENABLED = false;
    public static boolean GOLEM_OF_LAST_RESORT_RESET_PENDING_BREAK_TIMER = true;
    public static boolean DEBUG_LOGS_ENABLED = false;

    public static boolean GOLEM_ROLLBACK_GUARD_ENABLED = true;
    public static int GOLEM_ROLLBACK_WINDOW_TICKS = 30;
    public static int GOLEM_ROLLBACK_HORIZONTAL_RADIUS = 3;
    public static int GOLEM_ROLLBACK_DOWN_RADIUS = 1;
    public static int GOLEM_ROLLBACK_UP_RADIUS = 3;
    public static int GOLEM_ROLLBACK_MAX_BLOCKS_PER_SNAPSHOT = 256;

    public static boolean GOLEM_DROP_CLEANUP_ENABLED = true;
    public static int GOLEM_DROP_CLEANUP_WINDOW_TICKS = 60;
    public static double GOLEM_DROP_CLEANUP_RADIUS = 2.5D;
    public static int GOLEM_DROP_CLEANUP_MAX_ITEM_AGE_TICKS = 120;
    public static int GOLEM_DROP_CLEANUP_MAX_ZONES = 512;
    public static int GOLEM_DROP_CLEANUP_MAX_ITEMS_PER_TICK = 256;
    public static int GOLEM_DROP_CLEANUP_BASELINE_EXTRA_RADIUS = 2;

    public static boolean golemOfLastResortBlockBreakingEnabled() {
        ensureLoaded();
        return GOLEM_OF_LAST_RESORT_BLOCK_BREAKING_ENABLED;
    }

    public static boolean resetPendingBreakTimerWhenBlocked() {
        ensureLoaded();
        return GOLEM_OF_LAST_RESORT_RESET_PENDING_BREAK_TIMER;
    }

    public static boolean debugLogsEnabled() {
        ensureLoaded();
        return DEBUG_LOGS_ENABLED;
    }

    public static boolean golemRollbackGuardEnabled() {
        ensureLoaded();
        return GOLEM_ROLLBACK_GUARD_ENABLED;
    }

    public static int golemRollbackWindowTicks() {
        ensureLoaded();
        return Math.max(1, GOLEM_ROLLBACK_WINDOW_TICKS);
    }

    public static int golemRollbackHorizontalRadius() {
        ensureLoaded();
        return Math.max(1, GOLEM_ROLLBACK_HORIZONTAL_RADIUS);
    }

    public static int golemRollbackDownRadius() {
        ensureLoaded();
        return Math.max(0, GOLEM_ROLLBACK_DOWN_RADIUS);
    }

    public static int golemRollbackUpRadius() {
        ensureLoaded();
        return Math.max(1, GOLEM_ROLLBACK_UP_RADIUS);
    }

    public static int golemRollbackMaxBlocksPerSnapshot() {
        ensureLoaded();
        return Math.max(16, GOLEM_ROLLBACK_MAX_BLOCKS_PER_SNAPSHOT);
    }

    public static boolean golemDropCleanupEnabled() {
        ensureLoaded();
        return GOLEM_DROP_CLEANUP_ENABLED;
    }

    public static int golemDropCleanupWindowTicks() {
        ensureLoaded();
        return Math.max(1, GOLEM_DROP_CLEANUP_WINDOW_TICKS);
    }

    public static double golemDropCleanupRadius() {
        ensureLoaded();
        return Math.max(0.25D, GOLEM_DROP_CLEANUP_RADIUS);
    }

    public static int golemDropCleanupMaxItemAgeTicks() {
        ensureLoaded();
        return Math.max(0, GOLEM_DROP_CLEANUP_MAX_ITEM_AGE_TICKS);
    }

    public static int golemDropCleanupMaxZones() {
        ensureLoaded();
        return Math.max(16, GOLEM_DROP_CLEANUP_MAX_ZONES);
    }

    public static int golemDropCleanupMaxItemsPerTick() {
        ensureLoaded();
        return Math.max(1, GOLEM_DROP_CLEANUP_MAX_ITEMS_PER_TICK);
    }

    public static int golemDropCleanupBaselineExtraRadius() {
        ensureLoaded();
        return Math.max(0, GOLEM_DROP_CLEANUP_BASELINE_EXTRA_RADIUS);
    }

    public static void ensureLoaded() {
        if (!loaded) {
            synchronized (RaidsEnhancedCompatConfig.class) {
                if (!loaded) {
                    loadOrCreate();
                    loaded = true;
                }
            }
        }
    }

    public static void loadOrCreate() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaultConfig(CONFIG_FILE);
            }
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            GOLEM_OF_LAST_RESORT_BLOCK_BREAKING_ENABLED = readBoolean(properties,
                    "raidsEnhanced.golemOfLastResort.blockBreaking.enabled",
                    GOLEM_OF_LAST_RESORT_BLOCK_BREAKING_ENABLED);
            GOLEM_OF_LAST_RESORT_RESET_PENDING_BREAK_TIMER = readBoolean(properties,
                    "raidsEnhanced.golemOfLastResort.blockBreaking.resetPendingTimerWhenBlocked",
                    GOLEM_OF_LAST_RESORT_RESET_PENDING_BREAK_TIMER);
            DEBUG_LOGS_ENABLED = readBoolean(properties,
                    "raidsEnhanced.compat.debugLogs.enabled",
                    DEBUG_LOGS_ENABLED);
            GOLEM_ROLLBACK_GUARD_ENABLED = readBoolean(properties,
                    "raidsEnhanced.golemOfLastResort.rollbackGuard.enabled",
                    GOLEM_ROLLBACK_GUARD_ENABLED);
            GOLEM_ROLLBACK_WINDOW_TICKS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.rollbackGuard.windowTicks",
                    GOLEM_ROLLBACK_WINDOW_TICKS);
            GOLEM_ROLLBACK_HORIZONTAL_RADIUS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.rollbackGuard.horizontalRadius",
                    GOLEM_ROLLBACK_HORIZONTAL_RADIUS);
            GOLEM_ROLLBACK_DOWN_RADIUS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.rollbackGuard.downRadius",
                    GOLEM_ROLLBACK_DOWN_RADIUS);
            GOLEM_ROLLBACK_UP_RADIUS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.rollbackGuard.upRadius",
                    GOLEM_ROLLBACK_UP_RADIUS);
            GOLEM_ROLLBACK_MAX_BLOCKS_PER_SNAPSHOT = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.rollbackGuard.maxBlocksPerSnapshot",
                    GOLEM_ROLLBACK_MAX_BLOCKS_PER_SNAPSHOT);

            GOLEM_DROP_CLEANUP_ENABLED = readBoolean(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.enabled",
                    GOLEM_DROP_CLEANUP_ENABLED);
            GOLEM_DROP_CLEANUP_WINDOW_TICKS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.windowTicks",
                    GOLEM_DROP_CLEANUP_WINDOW_TICKS);
            GOLEM_DROP_CLEANUP_RADIUS = readDouble(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.radius",
                    GOLEM_DROP_CLEANUP_RADIUS);
            GOLEM_DROP_CLEANUP_MAX_ITEM_AGE_TICKS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.maxItemAgeTicks",
                    GOLEM_DROP_CLEANUP_MAX_ITEM_AGE_TICKS);
            GOLEM_DROP_CLEANUP_MAX_ZONES = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.maxZones",
                    GOLEM_DROP_CLEANUP_MAX_ZONES);
            GOLEM_DROP_CLEANUP_MAX_ITEMS_PER_TICK = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.maxItemsPerTick",
                    GOLEM_DROP_CLEANUP_MAX_ITEMS_PER_TICK);
            GOLEM_DROP_CLEANUP_BASELINE_EXTRA_RADIUS = readInt(properties,
                    "raidsEnhanced.golemOfLastResort.dropCleanup.baselineExtraRadius",
                    GOLEM_DROP_CLEANUP_BASELINE_EXTRA_RADIUS);
        } catch (IOException exception) {
            System.out.println("[Raid Enhancement Patch] Failed to load raids_enhanced_compat.properties, using defaults: " + exception);
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static int readInt(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void writeDefaultConfig(Path configFile) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("raidsEnhanced.golemOfLastResort.blockBreaking.enabled", "false");
        properties.setProperty("raidsEnhanced.golemOfLastResort.blockBreaking.resetPendingTimerWhenBlocked", "true");
        properties.setProperty("raidsEnhanced.compat.debugLogs.enabled", "false");
        properties.setProperty("raidsEnhanced.golemOfLastResort.rollbackGuard.enabled", "true");
        properties.setProperty("raidsEnhanced.golemOfLastResort.rollbackGuard.windowTicks", "30");
        properties.setProperty("raidsEnhanced.golemOfLastResort.rollbackGuard.horizontalRadius", "3");
        properties.setProperty("raidsEnhanced.golemOfLastResort.rollbackGuard.downRadius", "1");
        properties.setProperty("raidsEnhanced.golemOfLastResort.rollbackGuard.upRadius", "3");
        properties.setProperty("raidsEnhanced.golemOfLastResort.rollbackGuard.maxBlocksPerSnapshot", "256");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.enabled", "true");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.windowTicks", "60");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.radius", "2.5");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.maxItemAgeTicks", "120");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.maxZones", "512");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.maxItemsPerTick", "256");
        properties.setProperty("raidsEnhanced.golemOfLastResort.dropCleanup.baselineExtraRadius", "2");

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(configFile), StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), List.of(
                    "# Raid Enhancement Patch - Raids Enhanced compatibility config",
                    "# Version: 0.8.9.7.20-golem-drop-cleanup-query-hotfix",
                    "#",
                    "# false = prevent Raids Enhanced's Golem of Last Resort from causing lasting real block damage.",
                    "# The rollback guard restores changed blocks for a short post-hurt window.",
                    "# The drop cleanup guard only creates tiny cleanup zones around blocks that were actually restored,",
                    "# and only removes new ItemEntity drops that were not already present when the golem was hurt.",
                    ""
            )));
            properties.store(writer, "Raids Enhanced compatibility defaults");
        }
    }
}
