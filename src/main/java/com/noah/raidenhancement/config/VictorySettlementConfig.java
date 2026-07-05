package com.noah.raidenhancement.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Config and generated JSON defaults for the 0.8.9.8 victory settlement alpha. */
public final class VictorySettlementConfig {
    private VictorySettlementConfig() {
    }

    public static final String CONFIG_STAGE = "0.8.9.8.0-victory-settlement-sweep-favor-alpha";

    public static boolean ENABLED = true;
    public static boolean SHOW_CHAT_MESSAGES = true;
    public static int SETTLEMENT_RADIUS = 128;
    public static boolean GIVE_REWARDS_TO_INVENTORY = true;
    public static boolean DROP_REWARDS_IF_INVENTORY_FULL = true;
    public static boolean EQUAL_XP_PER_ELIGIBLE_PLAYER = true;

    public static int COSTLY_MIN_DEFENSE_FAILURES = 2;
    public static int COSTLY_MIN_TIMEOUT_PENALTIES = 2;
    public static int COSTLY_MIN_DAMAGED_VILLAGER_EVENTS = 6;

    public static boolean BATTLEFIELD_SWEEP_ENABLED = true;
    public static int BATTLEFIELD_SWEEP_RADIUS = 96;
    public static int BATTLEFIELD_SWEEP_VERTICAL_RADIUS = 32;
    public static int BATTLEFIELD_SWEEP_DELAY_TICKS = 60;
    public static int BATTLEFIELD_SWEEP_MAX_ITEMS = 512;
    public static int BATTLEFIELD_SWEEP_MAX_ITEMS_PER_TICK = 128;
    public static boolean BATTLEFIELD_SWEEP_CONVERT_TO_EMERALDS = true;
    public static boolean BATTLEFIELD_SWEEP_CONVERT_TO_XP = true;

    public static boolean FAVOR_ENABLED = true;
    public static boolean FAVOR_ENTRY_MESSAGES_ENABLED = true;
    public static int FAVOR_ENTRY_RADIUS = 96;
    public static int FAVOR_ENTRY_SCAN_INTERVAL_TICKS = 100;
    public static int FAVOR_ENTRY_MESSAGE_COOLDOWN_TICKS = 6000;
    public static int FAVOR_COSTLY_POINTS = 10;
    public static int FAVOR_VICTORY_POINTS = 20;
    public static int FAVOR_PERFECT_POINTS = 35;
    public static int FAVOR_HARD_BONUS = 10;
    public static int FAVOR_OMEN5_BONUS = 15;
    public static int FAVOR_EXTRA_WAVE_BONUS = 20;
    public static boolean FAVOR_XP_BONUS_ENABLED = true;

    public static Path configDir() {
        return Path.of("config", "raid_enhancement_patch");
    }

    public static Path rewardDir() {
        return configDir().resolve("settlement_rewards");
    }

    public static Path settlementHistoryFile() {
        return configDir().resolve("victory_settlement_history.properties");
    }

    public static Path favorFile() {
        return configDir().resolve("village_favor.properties");
    }

    public static void loadOrCreate() {
        Path dir = configDir();
        Path file = dir.resolve("victory_settlement.properties");
        try {
            Files.createDirectories(dir);
            Files.createDirectories(rewardDir());
            if (!Files.exists(file)) {
                writeDefaultProperties(file);
                System.out.println("[Raid Enhancement Patch] Created default victory settlement config: " + file.toAbsolutePath());
            }
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            ENABLED = readBool(properties, "enabled", ENABLED);
            SHOW_CHAT_MESSAGES = readBool(properties, "showChatMessages", SHOW_CHAT_MESSAGES);
            SETTLEMENT_RADIUS = readInt(properties, "settlementRadius", SETTLEMENT_RADIUS, 16, 384);
            GIVE_REWARDS_TO_INVENTORY = readBool(properties, "giveRewardsToInventory", GIVE_REWARDS_TO_INVENTORY);
            DROP_REWARDS_IF_INVENTORY_FULL = readBool(properties, "dropRewardsIfInventoryFull", DROP_REWARDS_IF_INVENTORY_FULL);
            EQUAL_XP_PER_ELIGIBLE_PLAYER = readBool(properties, "equalXpPerEligiblePlayer", EQUAL_XP_PER_ELIGIBLE_PLAYER);

            COSTLY_MIN_DEFENSE_FAILURES = readInt(properties, "victoryGrade.costly.minDefenseFailures", COSTLY_MIN_DEFENSE_FAILURES, 1, 32);
            COSTLY_MIN_TIMEOUT_PENALTIES = readInt(properties, "victoryGrade.costly.minTimeoutPenalties", COSTLY_MIN_TIMEOUT_PENALTIES, 1, 32);
            COSTLY_MIN_DAMAGED_VILLAGER_EVENTS = readInt(properties, "victoryGrade.costly.minDamagedVillagerEvents", COSTLY_MIN_DAMAGED_VILLAGER_EVENTS, 1, 512);

            BATTLEFIELD_SWEEP_ENABLED = readBool(properties, "battlefieldSweep.enabled", BATTLEFIELD_SWEEP_ENABLED);
            BATTLEFIELD_SWEEP_RADIUS = readInt(properties, "battlefieldSweep.radius", BATTLEFIELD_SWEEP_RADIUS, 16, 256);
            BATTLEFIELD_SWEEP_VERTICAL_RADIUS = readInt(properties, "battlefieldSweep.verticalRadius", BATTLEFIELD_SWEEP_VERTICAL_RADIUS, 4, 128);
            BATTLEFIELD_SWEEP_DELAY_TICKS = readInt(properties, "battlefieldSweep.delayTicksAfterVictory", BATTLEFIELD_SWEEP_DELAY_TICKS, 0, 1200);
            BATTLEFIELD_SWEEP_MAX_ITEMS = readInt(properties, "battlefieldSweep.maxItemsPerSweep", BATTLEFIELD_SWEEP_MAX_ITEMS, 1, 4096);
            BATTLEFIELD_SWEEP_MAX_ITEMS_PER_TICK = readInt(properties, "battlefieldSweep.maxItemsPerTick", BATTLEFIELD_SWEEP_MAX_ITEMS_PER_TICK, 1, 1024);
            BATTLEFIELD_SWEEP_CONVERT_TO_EMERALDS = readBool(properties, "battlefieldSweep.convertToEmeralds", BATTLEFIELD_SWEEP_CONVERT_TO_EMERALDS);
            BATTLEFIELD_SWEEP_CONVERT_TO_XP = readBool(properties, "battlefieldSweep.convertToXp", BATTLEFIELD_SWEEP_CONVERT_TO_XP);

            FAVOR_ENABLED = readBool(properties, "favor.enabled", FAVOR_ENABLED);
            FAVOR_ENTRY_MESSAGES_ENABLED = readBool(properties, "favor.entryMessages", FAVOR_ENTRY_MESSAGES_ENABLED);
            FAVOR_ENTRY_RADIUS = readInt(properties, "favor.entryRadius", FAVOR_ENTRY_RADIUS, 16, 256);
            FAVOR_ENTRY_SCAN_INTERVAL_TICKS = readInt(properties, "favor.entryScanIntervalTicks", FAVOR_ENTRY_SCAN_INTERVAL_TICKS, 20, 2400);
            FAVOR_ENTRY_MESSAGE_COOLDOWN_TICKS = readInt(properties, "favor.entryMessageCooldownTicks", FAVOR_ENTRY_MESSAGE_COOLDOWN_TICKS, 200, 72000);
            FAVOR_COSTLY_POINTS = readInt(properties, "favor.points.costly", FAVOR_COSTLY_POINTS, 0, 1000);
            FAVOR_VICTORY_POINTS = readInt(properties, "favor.points.victory", FAVOR_VICTORY_POINTS, 0, 1000);
            FAVOR_PERFECT_POINTS = readInt(properties, "favor.points.perfect", FAVOR_PERFECT_POINTS, 0, 1000);
            FAVOR_HARD_BONUS = readInt(properties, "favor.bonus.hard", FAVOR_HARD_BONUS, 0, 1000);
            FAVOR_OMEN5_BONUS = readInt(properties, "favor.bonus.omen5", FAVOR_OMEN5_BONUS, 0, 1000);
            FAVOR_EXTRA_WAVE_BONUS = readInt(properties, "favor.bonus.extraWave", FAVOR_EXTRA_WAVE_BONUS, 0, 1000);
            FAVOR_XP_BONUS_ENABLED = readBool(properties, "favor.xpBonusEnabled", FAVOR_XP_BONUS_ENABLED);

            writeDefaultProperties(dir.resolve("victory_settlement.default.properties"));
            writeDefaultJsonFiles();
            System.out.println("[Raid Enhancement Patch] Loaded victory settlement config " + CONFIG_STAGE
                    + ": enabled=" + ENABLED
                    + ", settlementRadius=" + SETTLEMENT_RADIUS
                    + ", sweep=" + BATTLEFIELD_SWEEP_ENABLED
                    + ", favor=" + FAVOR_ENABLED + ".");
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Victory settlement config load failed; using built-in defaults: " + throwable);
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
        System.out.println("[Raid Enhancement Patch] Invalid boolean config value for " + key + "=" + value + "; using " + fallback + ".");
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
            System.out.println("[Raid Enhancement Patch] Invalid integer config value for " + key + "=" + value + "; using " + fallback + ".");
            return fallback;
        }
    }

    private static void writeDefaultProperties(Path file) throws IOException {
        List<String> lines = List.of(
                "# Raid Enhancement Patch - Victory Settlement Config",
                "# Stage: " + CONFIG_STAGE,
                "# File created in config/raid_enhancement_patch/ . Restart the game/server after editing.",
                "# This layer adds three-grade settlement, JSON rewards, battlefield sweep conversion, XP rewards, and village favor.",
                "",
                "enabled=true",
                "showChatMessages=true",
                "settlementRadius=128",
                "giveRewardsToInventory=true",
                "dropRewardsIfInventoryFull=true",
                "equalXpPerEligiblePlayer=true",
                "",
                "# Three-grade victory: perfect / victory / costly. One small breach is now normal Victory, not immediate Costly Victory.",
                "victoryGrade.costly.minDefenseFailures=2",
                "victoryGrade.costly.minTimeoutPenalties=2",
                "victoryGrade.costly.minDamagedVillagerEvents=6",
                "",
                "# Battlefield sweep converts configured raid drops around the village after victory.",
                "battlefieldSweep.enabled=true",
                "battlefieldSweep.radius=96",
                "battlefieldSweep.verticalRadius=32",
                "battlefieldSweep.delayTicksAfterVictory=60",
                "battlefieldSweep.maxItemsPerSweep=512",
                "battlefieldSweep.maxItemsPerTick=128",
                "battlefieldSweep.convertToEmeralds=true",
                "battlefieldSweep.convertToXp=true",
                "",
                "# Village favor records player-specific gratitude for this village. First alpha does NOT modify security golem deployment.",
                "favor.enabled=true",
                "favor.entryMessages=true",
                "favor.entryRadius=96",
                "favor.entryScanIntervalTicks=100",
                "favor.entryMessageCooldownTicks=6000",
                "favor.points.costly=10",
                "favor.points.victory=20",
                "favor.points.perfect=35",
                "favor.bonus.hard=10",
                "favor.bonus.omen5=15",
                "favor.bonus.extraWave=20",
                "favor.xpBonusEnabled=true"
        );
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }

    private static void writeDefaultJsonFiles() throws IOException {
        writeIfMissing(rewardDir().resolve("perfect_victory.json"), """
                {
                  "enabled": true,
                  "entries": [
                    {"item": "minecraft:emerald", "min": 24, "max": 40, "chance": 1.0},
                    {"item": "minecraft:iron_block", "min": 1, "max": 3, "chance": 0.75},
                    {"item": "minecraft:amethyst_block", "min": 1, "max": 1, "chance": 0.60},
                    {"item": "raid_enhancement_patch:basic_shield_token", "min": 1, "max": 1, "chance": 0.35},
                    {"item": "raid_enhancement_patch:advanced_shield_token", "min": 1, "max": 1, "chance": 0.15}
                  ]
                }
                """);
        writeIfMissing(rewardDir().resolve("victory.json"), """
                {
                  "enabled": true,
                  "entries": [
                    {"item": "minecraft:emerald", "min": 12, "max": 24, "chance": 1.0},
                    {"item": "minecraft:iron_ingot", "min": 8, "max": 16, "chance": 0.85},
                    {"item": "minecraft:amethyst_shard", "min": 2, "max": 6, "chance": 0.65},
                    {"item": "raid_enhancement_patch:basic_strength_token", "min": 1, "max": 1, "chance": 0.20}
                  ]
                }
                """);
        writeIfMissing(rewardDir().resolve("costly_victory.json"), """
                {
                  "enabled": true,
                  "entries": [
                    {"item": "minecraft:emerald", "min": 4, "max": 12, "chance": 1.0},
                    {"item": "minecraft:iron_ingot", "min": 4, "max": 8, "chance": 0.75},
                    {"item": "minecraft:bread", "min": 4, "max": 12, "chance": 0.60}
                  ]
                }
                """);
        writeIfMissing(rewardDir().resolve("sweep_exchange.json"), """
                {
                  "enabled": true,
                  "entries": [
                    {"item": "minecraft:crossbow", "enabled": true, "emeraldValue": 2, "experienceValue": 4},
                    {"item": "minecraft:iron_axe", "enabled": true, "emeraldValue": 2, "experienceValue": 3},
                    {"item": "minecraft:saddle", "enabled": true, "emeraldValue": 4, "experienceValue": 8},
                    {"item": "minecraft:ominous_banner", "enabled": true, "emeraldValue": 1, "experienceValue": 1},
                    {"item": "minecraft:totem_of_undying", "enabled": false, "emeraldValue": 16, "experienceValue": 40}
                  ]
                }
                """);
        writeIfMissing(rewardDir().resolve("experience_rewards.json"), """
                {
                  "enabled": true,
                  "gradeMultiplier": {"perfect": 1.15, "victory": 1.0, "costly": 0.7},
                  "table": {
                    "EASY": [160, 220, 300, 380, 480],
                    "NORMAL": [360, 480, 620, 780, 960],
                    "HARD": [700, 900, 1100, 1300, 1507]
                  }
                }
                """);
        writeIfMissing(rewardDir().resolve("favor_rewards.json"), """
                {
                  "enabled": true,
                  "xpBonusByLevel": [0.0, 0.05, 0.08, 0.12, 0.16, 0.20],
                  "entryMessages": true
                }
                """);
    }

    private static void writeIfMissing(Path file, String content) throws IOException {
        if (Files.exists(file)) {
            return;
        }
        Files.createDirectories(file.getParent());
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content.strip());
            writer.write(System.lineSeparator());
        }
    }
}
