package com.noah.raidenhancement.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Configuration for the village-favor long-term relation and profession gift system. */
public final class VillageFavorConfig {
    public static final String CONFIG_STAGE = "0.9.0.0-village-favor-v2-profession-gifts-alpha";

    public static boolean ENABLED = true;
    public static int VILLAGE_RADIUS = 96;
    public static boolean ENABLE_GREETING = true;
    public static int GREETING_COOLDOWN_TICKS = 24000;
    public static boolean ENABLE_GIFT = true;
    public static int GIFT_COOLDOWN_TICKS = 24000;
    public static int MAX_GIFT_PER_VILLAGE_PER_PLAYER = 0;
    public static boolean ENABLE_PARTICLES = true;
    public static boolean ENABLE_SOUND = true;
    public static boolean ENABLE_DEBUG_LOG = false;
    public static boolean MIGRATE_LEGACY_FAVOR_FILE = true;

    public static boolean ENABLE_PROFESSION_GIFT = true;
    public static boolean ENABLE_FAVOR_LEVEL = true;
    public static boolean ENABLE_VILLAGER_CAREER_LEVEL_SCALING = true;
    public static boolean FALLBACK_TO_GENERIC_GIFT_POOL = true;
    public static boolean ENABLE_MASTER_VILLAGER_RARE_GIFT = true;
    public static boolean ENABLE_GIFT_DEBUG_LOG = false;

    public static boolean ALLOW_MULTIPLE_VICTORIES_INCREASE_FAVOR = true;
    public static int MAX_FAVOR_LEVEL = 5;
    public static int MAX_GIFT_TIER = 5;
    public static int GIFT_CLAIM_PERIOD_TICKS = 24000;
    public static int MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY = 1;
    public static int MAX_EMERALD_PER_GIFT = 8;
    public static int MAX_EMERALD_BONUS_BY_FAVOR_LEVEL = 4;
    public static int MAX_EMERALD_BONUS_BY_VILLAGER_LEVEL = 4;
    public static int BASE_RAID_WIN_SCORE = 1;
    public static int OMEN_LEVEL_SCORE_BONUS = 1;
    public static int HARD_DIFFICULTY_SCORE_BONUS = 1;
    public static int EXTRA_WAVE_SCORE_BONUS = 1;
    public static boolean SAME_VILLAGE_REPEATED_RAID_DIMINISHING = false;
    public static double RARE_GIFT_CHANCE_MULTIPLIER = 1.0D;
    public static int[] FAVOR_LEVEL_THRESHOLDS = new int[]{1, 3, 6, 10, 15};

    private VillageFavorConfig() {
    }

    public static Path configDir() {
        return Path.of("config", "raid_enhancement_patch");
    }

    public static Path configFile() {
        return configDir().resolve("village_favor_system.properties");
    }

    public static Path fallbackDataFile() {
        return configDir().resolve("village_favor_relations.properties");
    }

    public static Path rewardDir() {
        return configDir().resolve("settlement_rewards");
    }

    public static Path giftRootDir() {
        return rewardDir().resolve("village_favor");
    }

    /** Legacy V1 gift file kept for old configs and migration friendliness. */
    public static Path giftLootFile() {
        return rewardDir().resolve("village_favor_gift.json");
    }

    public static Path giftLootFile(String group, String tier) {
        String safeGroup = safeSegment(group, "generic");
        String safeTier = safeSegment(tier, "basic");
        return giftRootDir().resolve(safeGroup).resolve(safeTier + ".json");
    }

    public static void loadOrCreate() {
        Path file = configFile();
        try {
            Files.createDirectories(configDir());
            Files.createDirectories(rewardDir());
            Files.createDirectories(giftRootDir());
            if (!Files.exists(file)) {
                writeDefaultProperties(file);
                System.out.println("[Raid Enhancement Patch] Created default village favor V2 config: " + file.toAbsolutePath());
            }
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            ENABLED = readBool(properties, "enableVillageFavorSystem", ENABLED);
            VILLAGE_RADIUS = readInt(properties, "favorVillageRadius", VILLAGE_RADIUS, 16, 256);
            ENABLE_GREETING = readBool(properties, "enableGreeting", ENABLE_GREETING);
            GREETING_COOLDOWN_TICKS = readInt(properties, "greetingCooldownTicks", GREETING_COOLDOWN_TICKS, 200, 1728000);
            ENABLE_GIFT = readBool(properties, "enableFavorGift", ENABLE_GIFT);
            GIFT_COOLDOWN_TICKS = readInt(properties, "giftCooldownTicks", GIFT_COOLDOWN_TICKS, 200, 1728000);
            MAX_GIFT_PER_VILLAGE_PER_PLAYER = readInt(properties, "maxGiftPerVillagePerPlayer", MAX_GIFT_PER_VILLAGE_PER_PLAYER, 0, 1000000);
            ENABLE_PARTICLES = readBool(properties, "enableFavorParticles", ENABLE_PARTICLES);
            ENABLE_SOUND = readBool(properties, "enableFavorSound", ENABLE_SOUND);
            ENABLE_DEBUG_LOG = readBool(properties, "enableDebugLog", ENABLE_DEBUG_LOG);
            MIGRATE_LEGACY_FAVOR_FILE = readBool(properties, "migrateLegacyFavorFile", MIGRATE_LEGACY_FAVOR_FILE);

            ENABLE_PROFESSION_GIFT = readBool(properties, "enableProfessionGift", ENABLE_PROFESSION_GIFT);
            ENABLE_FAVOR_LEVEL = readBool(properties, "enableFavorLevel", ENABLE_FAVOR_LEVEL);
            ENABLE_VILLAGER_CAREER_LEVEL_SCALING = readBool(properties, "enableVillagerCareerLevelScaling", ENABLE_VILLAGER_CAREER_LEVEL_SCALING);
            FALLBACK_TO_GENERIC_GIFT_POOL = readBool(properties, "fallbackToGenericGiftPool", FALLBACK_TO_GENERIC_GIFT_POOL);
            ENABLE_MASTER_VILLAGER_RARE_GIFT = readBool(properties, "enableMasterVillagerRareGift", ENABLE_MASTER_VILLAGER_RARE_GIFT);
            ENABLE_GIFT_DEBUG_LOG = readBool(properties, "enableGiftDebugLog", ENABLE_GIFT_DEBUG_LOG);

            ALLOW_MULTIPLE_VICTORIES_INCREASE_FAVOR = readBool(properties, "allowMultipleVictoriesIncreaseFavor", ALLOW_MULTIPLE_VICTORIES_INCREASE_FAVOR);
            MAX_FAVOR_LEVEL = readInt(properties, "maxFavorLevel", MAX_FAVOR_LEVEL, 1, 10);
            MAX_GIFT_TIER = readInt(properties, "maxGiftTier", MAX_GIFT_TIER, 1, 5);
            GIFT_CLAIM_PERIOD_TICKS = readInt(properties, "giftClaimPeriodTicks", GIFT_CLAIM_PERIOD_TICKS, 1200, 1728000);
            MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY = readInt(properties, "maxGiftClaimsPerVillagePerDay", MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY, 0, 1000);
            MAX_EMERALD_PER_GIFT = readInt(properties, "maxEmeraldPerGift", MAX_EMERALD_PER_GIFT, 0, 64);
            MAX_EMERALD_BONUS_BY_FAVOR_LEVEL = readInt(properties, "maxEmeraldBonusByFavorLevel", MAX_EMERALD_BONUS_BY_FAVOR_LEVEL, 0, 64);
            MAX_EMERALD_BONUS_BY_VILLAGER_LEVEL = readInt(properties, "maxEmeraldBonusByVillagerLevel", MAX_EMERALD_BONUS_BY_VILLAGER_LEVEL, 0, 64);
            BASE_RAID_WIN_SCORE = readInt(properties, "baseRaidWinScore", BASE_RAID_WIN_SCORE, 0, 1000);
            OMEN_LEVEL_SCORE_BONUS = readInt(properties, "omenLevelScoreBonus", OMEN_LEVEL_SCORE_BONUS, 0, 1000);
            HARD_DIFFICULTY_SCORE_BONUS = readInt(properties, "hardDifficultyScoreBonus", HARD_DIFFICULTY_SCORE_BONUS, 0, 1000);
            EXTRA_WAVE_SCORE_BONUS = readInt(properties, "extraWaveScoreBonus", EXTRA_WAVE_SCORE_BONUS, 0, 1000);
            SAME_VILLAGE_REPEATED_RAID_DIMINISHING = readBool(properties, "sameVillageRepeatedRaidDiminishing", SAME_VILLAGE_REPEATED_RAID_DIMINISHING);
            RARE_GIFT_CHANCE_MULTIPLIER = readDouble(properties, "rareGiftChanceMultiplier", RARE_GIFT_CHANCE_MULTIPLIER, 0.0D, 10.0D);
            FAVOR_LEVEL_THRESHOLDS = readIntArray(properties, "favorLevelThresholds", FAVOR_LEVEL_THRESHOLDS);

            writeDefaultProperties(configDir().resolve("village_favor_system.default.properties"));
            writeDefaultGiftJsons();
            System.out.println("[Raid Enhancement Patch] Loaded village favor config " + CONFIG_STAGE
                    + ": enabled=" + ENABLED
                    + ", professionGift=" + ENABLE_PROFESSION_GIFT
                    + ", maxLevel=" + MAX_FAVOR_LEVEL
                    + ", maxGiftTier=" + MAX_GIFT_TIER + ".");
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Village favor config load failed; using built-in defaults: " + throwable);
            try {
                writeDefaultGiftJsons();
            } catch (Throwable ignored) {
                // Do not let config bootstrap affect gameplay.
            }
        }
    }

    public static int favorLevelFromScore(int score) {
        if (!ENABLE_FAVOR_LEVEL) {
            return 1;
        }
        int safeScore = Math.max(0, score);
        int level = 0;
        for (int threshold : FAVOR_LEVEL_THRESHOLDS) {
            if (safeScore >= threshold) {
                level++;
            }
        }
        return Math.max(1, Math.min(Math.max(1, MAX_FAVOR_LEVEL), level));
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
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static double readDouble(Properties properties, String key, double fallback, double min, double max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int[] readIntArray(Properties properties, String key, int[] fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.replace('[', ' ').replace(']', ' ').trim();
        String[] parts = cleaned.split(",");
        int[] parsed = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            try {
                parsed[count++] = Math.max(0, Integer.parseInt(part.trim()));
            } catch (Throwable ignored) {
            }
        }
        if (count <= 0) {
            return fallback;
        }
        return java.util.Arrays.copyOf(parsed, count);
    }

    private static void writeDefaultProperties(Path file) throws IOException {
        List<String> lines = List.of(
                "# Raid Enhancement Patch - Village Favor System Config",
                "# Stage: " + CONFIG_STAGE,
                "# V2 is interaction-driven: no every-tick villager scan, no permanent trade mutation.",
                "",
                "enableVillageFavorSystem=true",
                "favorVillageRadius=96",
                "enableGreeting=true",
                "greetingCooldownTicks=24000",
                "enableFavorGift=true",
                "giftCooldownTicks=24000",
                "maxGiftPerVillagePerPlayer=0",
                "enableFavorParticles=true",
                "enableFavorSound=true",
                "enableDebugLog=false",
                "migrateLegacyFavorFile=true",
                "",
                "# V2 profession gift layer",
                "enableProfessionGift=true",
                "enableFavorLevel=true",
                "enableVillagerCareerLevelScaling=true",
                "fallbackToGenericGiftPool=true",
                "enableMasterVillagerRareGift=true",
                "enableGiftDebugLog=false",
                "allowMultipleVictoriesIncreaseFavor=true",
                "maxFavorLevel=5",
                "maxGiftTier=5",
                "giftClaimPeriodTicks=24000",
                "maxGiftClaimsPerVillagePerDay=1",
                "maxEmeraldPerGift=8",
                "maxEmeraldBonusByFavorLevel=4",
                "maxEmeraldBonusByVillagerLevel=4",
                "favorLevelThresholds=[1,3,6,10,15]",
                "baseRaidWinScore=1",
                "omenLevelScoreBonus=1",
                "hardDifficultyScoreBonus=1",
                "extraWaveScoreBonus=1",
                "sameVillageRepeatedRaidDiminishing=false",
                "rareGiftChanceMultiplier=1.0"
        );
        Files.createDirectories(file.getParent());
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }

    private static void writeDefaultGiftJsons() throws IOException {
        writeLegacyGiftJson();
        // Generic fallback pools.
        writeGift("generic", "basic", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:bread", "min": 1, "max": 3, "chance": 0.75},
                  {"item": "minecraft:apple", "min": 1, "max": 2, "chance": 0.45},
                  {"item": "minecraft:emerald", "min": 1, "max": 1, "chance": 0.20}
                ]}
                """);
        writeGift("generic", "good", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:bread", "min": 2, "max": 5, "chance": 0.75},
                  {"item": "minecraft:apple", "min": 1, "max": 3, "chance": 0.55},
                  {"item": "minecraft:iron_ingot", "min": 1, "max": 2, "chance": 0.20},
                  {"item": "minecraft:emerald", "min": 1, "max": 2, "chance": 0.35}
                ]}
                """);
        writeGift("generic", "great", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:emerald", "min": 1, "max": 3, "chance": 0.45},
                  {"item": "minecraft:iron_ingot", "min": 1, "max": 3, "chance": 0.30},
                  {"item": "minecraft:cookie", "min": 2, "max": 6, "chance": 0.45},
                  {"item": "minecraft:bell", "min": 1, "max": 1, "chance": 0.01}
                ]}
                """);
        writeGift("generic", "master", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:emerald", "min": 2, "max": 4, "chance": 0.55},
                  {"item": "minecraft:iron_ingot", "min": 2, "max": 4, "chance": 0.35},
                  {"item": "minecraft:experience_bottle", "min": 1, "max": 1, "chance": 0.08},
                  {"item": "minecraft:bell", "min": 1, "max": 1, "chance": 0.02}
                ]}
                """);

        writeGift("farmer", "basic", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:bread", "min": 2, "max": 5, "chance": 0.85},
                  {"item": "minecraft:carrot", "min": 2, "max": 6, "chance": 0.65},
                  {"item": "minecraft:potato", "min": 2, "max": 6, "chance": 0.65}
                ]}
                """);
        writeGift("farmer", "good", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:pumpkin_pie", "min": 1, "max": 3, "chance": 0.60},
                  {"item": "minecraft:cookie", "min": 2, "max": 6, "chance": 0.55},
                  {"item": "minecraft:emerald", "min": 1, "max": 2, "chance": 0.30}
                ]}
                """);
        writeGift("farmer", "great", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:pumpkin_pie", "min": 2, "max": 4, "chance": 0.55},
                  {"item": "minecraft:emerald", "min": 1, "max": 3, "chance": 0.40},
                  {"item": "minecraft:golden_carrot", "min": 1, "max": 1, "chance": 0.06}
                ]}
                """);
        writeGift("farmer", "master", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:emerald", "min": 2, "max": 4, "chance": 0.45},
                  {"item": "minecraft:golden_carrot", "min": 1, "max": 2, "chance": 0.12},
                  {"item": "minecraft:golden_apple", "min": 1, "max": 1, "chance": 0.01}
                ]}
                """);

        writeGift("librarian", "basic", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:paper", "min": 3, "max": 8, "chance": 0.75},
                  {"item": "minecraft:book", "min": 1, "max": 2, "chance": 0.45}
                ]}
                """);
        writeGift("librarian", "good", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:book", "min": 1, "max": 3, "chance": 0.55},
                  {"item": "minecraft:bookshelf", "min": 1, "max": 1, "chance": 0.18},
                  {"item": "minecraft:emerald", "min": 1, "max": 2, "chance": 0.30}
                ]}
                """);
        writeGift("librarian", "great", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:book", "min": 2, "max": 4, "chance": 0.50},
                  {"item": "minecraft:experience_bottle", "min": 1, "max": 2, "chance": 0.12},
                  {"item": "minecraft:emerald", "min": 1, "max": 3, "chance": 0.35}
                ]}
                """);
        writeGift("librarian", "master", """
                {"enabled": true, "entries": [
                  {"item": "minecraft:experience_bottle", "min": 1, "max": 3, "chance": 0.18},
                  {"item": "minecraft:emerald", "min": 2, "max": 4, "chance": 0.45},
                  {"item": "minecraft:enchanted_book", "min": 1, "max": 1, "chance": 0.015}
                ]}
                """);

        writeSimpleGroup("shepherd", "minecraft:white_wool", "minecraft:blue_dye", "minecraft:painting");
        writeSimpleGroup("fisherman", "minecraft:cod", "minecraft:cooked_cod", "minecraft:fishing_rod");
        writeSimpleGroup("fletcher", "minecraft:arrow", "minecraft:flint", "minecraft:bow");
        writeSimpleGroup("cleric", "minecraft:glowstone_dust", "minecraft:redstone", "minecraft:experience_bottle");
        writeSimpleGroup("mason", "minecraft:brick", "minecraft:stone", "minecraft:quartz");
        writeSimpleGroup("toolsmith", "minecraft:iron_ingot", "minecraft:coal", "minecraft:iron_pickaxe");
        writeSimpleGroup("weaponsmith", "minecraft:iron_ingot", "minecraft:coal", "minecraft:iron_axe");
        writeSimpleGroup("armorer", "minecraft:iron_ingot", "minecraft:coal", "minecraft:iron_chestplate");
        writeSimpleGroup("leatherworker", "minecraft:leather", "minecraft:rabbit_hide", "minecraft:leather_chestplate");
        writeSimpleGroup("cartographer", "minecraft:paper", "minecraft:map", "minecraft:compass");
        writeSimpleGroup("butcher", "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:smoker");
    }

    private static void writeLegacyGiftJson() throws IOException {
        Path file = giftLootFile();
        if (Files.exists(file)) {
            return;
        }
        Files.createDirectories(file.getParent());
        String content = """
                {"enabled": true, "entries": [
                  {"item": "minecraft:bread", "min": 2, "max": 5, "chance": 0.85},
                  {"item": "minecraft:apple", "min": 1, "max": 3, "chance": 0.50},
                  {"item": "minecraft:emerald", "min": 1, "max": 2, "chance": 0.35},
                  {"item": "minecraft:iron_ingot", "min": 1, "max": 3, "chance": 0.25},
                  {"item": "minecraft:bell", "min": 1, "max": 1, "chance": 0.02}
                ]}
                """;
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content.strip());
            writer.write(System.lineSeparator());
        }
    }

    private static void writeSimpleGroup(String group, String basicItem, String goodItem, String masterItem) throws IOException {
        writeGift(group, "basic", "{\"enabled\": true, \"entries\": ["
                + "{\"item\": \"" + basicItem + "\", \"min\": 1, \"max\": 4, \"chance\": 0.75},"
                + "{\"item\": \"minecraft:emerald\", \"min\": 1, \"max\": 1, \"chance\": 0.15}]}\n");
        writeGift(group, "good", "{\"enabled\": true, \"entries\": ["
                + "{\"item\": \"" + basicItem + "\", \"min\": 2, \"max\": 6, \"chance\": 0.65},"
                + "{\"item\": \"" + goodItem + "\", \"min\": 1, \"max\": 2, \"chance\": 0.35},"
                + "{\"item\": \"minecraft:emerald\", \"min\": 1, \"max\": 2, \"chance\": 0.25}]}\n");
        writeGift(group, "great", "{\"enabled\": true, \"entries\": ["
                + "{\"item\": \"" + goodItem + "\", \"min\": 1, \"max\": 3, \"chance\": 0.45},"
                + "{\"item\": \"minecraft:emerald\", \"min\": 1, \"max\": 3, \"chance\": 0.35}]}\n");
        writeGift(group, "master", "{\"enabled\": true, \"entries\": ["
                + "{\"item\": \"" + goodItem + "\", \"min\": 2, \"max\": 4, \"chance\": 0.40},"
                + "{\"item\": \"" + masterItem + "\", \"min\": 1, \"max\": 1, \"chance\": 0.06},"
                + "{\"item\": \"minecraft:emerald\", \"min\": 2, \"max\": 4, \"chance\": 0.40}]}\n");
    }

    private static void writeGift(String group, String tier, String content) throws IOException {
        Path file = giftLootFile(group, tier);
        if (Files.exists(file)) {
            return;
        }
        Files.createDirectories(file.getParent());
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content.strip());
            writer.write(System.lineSeparator());
        }
    }

    private static String safeSegment(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String normalized = input.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        return normalized.isBlank() ? fallback : normalized;
    }
}
