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
 * Stage 0.9.0.2: opt-in key-chain diagnostics for raid/village/favor auditing.
 *
 * This configuration is diagnostic-only. It must never change raid wave logic,
 * settlement eligibility, BossBar progress, reward values, villager gifts, or
 * persisted gameplay state. It exists so repeated same-village raids can be
 * audited before the VillageKey/RaidInstanceKey split or BossBar refill fix is implemented.
 */
public final class KeyDiagnosticsConfig {
    private KeyDiagnosticsConfig() {
    }

    public static final String CONFIG_STAGE = "0.9.0.2-key-bossbar-diagnostics-logfile-alpha";

    public static boolean ENABLED = false;
    public static boolean LOG_RAID_DISCOVERY = true;
    public static boolean LOG_SETTLEMENT = true;
    public static boolean LOG_FAVOR = true;
    public static boolean LOG_BOSSBAR = true;
    public static boolean LOG_STORAGE_PATHS = true;
    public static int LOG_INTERVAL_TICKS = 100;
    public static int MAX_PLAYER_KEYS_PER_LINE = 3;

    public static Path configDir() {
        return Path.of("config", "raid_enhancement_patch");
    }

    public static void loadOrCreate() {
        Path dir = configDir();
        Path file = dir.resolve("key_diagnostics.properties");
        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                writeDefaultProperties(file);
                System.out.println("[Raid Enhancement Patch] Created default key diagnostics config: " + file.toAbsolutePath());
            }
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            ENABLED = readBool(properties, "enabled", ENABLED);
            LOG_RAID_DISCOVERY = readBool(properties, "log.raidDiscovery", LOG_RAID_DISCOVERY);
            LOG_SETTLEMENT = readBool(properties, "log.settlement", LOG_SETTLEMENT);
            LOG_FAVOR = readBool(properties, "log.favor", LOG_FAVOR);
            LOG_BOSSBAR = readBool(properties, "log.bossbar", LOG_BOSSBAR);
            LOG_STORAGE_PATHS = readBool(properties, "log.storagePaths", LOG_STORAGE_PATHS);
            LOG_INTERVAL_TICKS = readInt(properties, "log.intervalTicks", LOG_INTERVAL_TICKS, 20, 72000);
            MAX_PLAYER_KEYS_PER_LINE = readInt(properties, "log.maxPlayerKeysPerLine", MAX_PLAYER_KEYS_PER_LINE, 1, 12);
            writeDefaultProperties(dir.resolve("key_diagnostics.default.properties"));
            if (ENABLED) {
                System.out.println("[Raid Enhancement Patch] Loaded key diagnostics config " + CONFIG_STAGE
                        + ": raidDiscovery=" + LOG_RAID_DISCOVERY
                        + ", settlement=" + LOG_SETTLEMENT
                        + ", favor=" + LOG_FAVOR
                        + ", bossbar=" + LOG_BOSSBAR
                        + ", storagePaths=" + LOG_STORAGE_PATHS
                        + ", intervalTicks=" + LOG_INTERVAL_TICKS + ".");
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Key diagnostics config load failed; diagnostics stay disabled: " + throwable);
            ENABLED = false;
        }
    }

    private static boolean readBool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized) || "on".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized) || "off".equals(normalized)) {
            return false;
        }
        System.out.println("[Raid Enhancement Patch] Invalid key diagnostics boolean config value for " + key + "=" + value + "; using " + fallback + ".");
        return fallback;
    }

    private static int readInt(Properties properties, String key, int fallback, int min, int max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            System.out.println("[Raid Enhancement Patch] Invalid key diagnostics integer config value for " + key + "=" + value + "; using " + fallback + ".");
            return fallback;
        }
    }

    private static void writeDefaultProperties(Path file) throws IOException {
        List<String> lines = List.of(
                "# Raid Enhancement Patch - Key Diagnostics Config",
                "# Stage: " + CONFIG_STAGE,
                "# Diagnostic-only. Restart the game/server after editing.",
                "# This must not change raid waves, BossBar progress, rewards, villager gifts, Mixin behavior, or persisted gameplay state.",
                "",
                "# Master switch. Keep false during ordinary play; set true only while collecting key-chain/BossBar logs.",
                "enabled=false",
                "",
                "log.raidDiscovery=true",
                "log.settlement=true",
                "log.favor=true",
                "log.bossbar=true",
                "log.storagePaths=true",
                "",
                "# Repeated BossBar/discovery lines for the same key are throttled by this interval.",
                "log.intervalTicks=100",
                "",
                "# Avoid one very long settlement line when many players are eligible.",
                "log.maxPlayerKeysPerLine=3",
                ""
        );
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }
}
