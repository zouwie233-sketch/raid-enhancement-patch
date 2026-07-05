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
 * Runtime config for 0.8.9.7 security support items.
 *
 * This file is intentionally separate from village_security.properties so the
 * stable Step 8 village-security baseline can remain untouched. Existing user
 * configs will not be rewritten except for a generated default reference file.
 */
public final class BattleSupportConfig {
    private BattleSupportConfig() {
    }

    public static final String CONFIG_STAGE = "0.8.9.7.11-control-tokens-cooldown-isolation";

    public static boolean ENABLED = true;
    public static boolean REQUIRE_ACTIVE_RAID = true;
    public static boolean SHOW_CHAT_MESSAGES = true;
    public static boolean CONSUME_ITEM_ON_SUCCESS = true;
    public static int USE_RADIUS = 128;
    public static int DEFAULT_DURATION_TICKS = 6000;
    public static int DEFAULT_COOLDOWN_TICKS = 1200;
    public static boolean AFFECT_MERCENARY_GOLEMS = false;

    public static boolean STRENGTH_ENABLED = true;
    public static int STRENGTH_DURATION_TICKS = 6000;
    public static int STRENGTH_COOLDOWN_TICKS = 1200;

    public static boolean SHIELD_ENABLED = true;
    public static int SHIELD_DURATION_TICKS = 6000;
    public static int SHIELD_COOLDOWN_TICKS = 1200;
    public static float BASIC_SHIELD_TEMP_HEALTH = 30.0F;
    public static float ADVANCED_SHIELD_TEMP_HEALTH = 100.0F;

    public static boolean SWIFTNESS_ENABLED = true;
    public static int SWIFTNESS_DURATION_TICKS = 6000;
    public static int SWIFTNESS_COOLDOWN_TICKS = 1200;

    public static boolean HUNTER_ENABLED = true;
    public static int HUNTER_DURATION_TICKS = 6000;
    public static int HUNTER_COOLDOWN_TICKS = 1200;

    public static boolean FIRE_ENABLED = true;
    public static int FIRE_DURATION_TICKS = 6000;
    public static int FIRE_COOLDOWN_TICKS = 1200;
    public static boolean ADVANCED_FIRE_ADDS_REGENERATION = true;

    public static boolean INSIGHT_ENABLED = true;
    public static int INSIGHT_BASIC_WAVE_COUNT = 3;
    public static int INSIGHT_GLOW_DURATION_TICKS = 6000;
    public static int INSIGHT_COOLDOWN_TICKS = 1200;

    public static boolean RALLY_ENABLED = true;
    public static int RALLY_BASIC_COUNT = 5;
    public static int RALLY_ADVANCED_COUNT = 10;
    public static int RALLY_BASIC_COOLDOWN_TICKS = 1800;
    public static int RALLY_ADVANCED_COOLDOWN_TICKS = 2400;
    public static boolean RALLY_BASIC_CONSUMES_ITEM = true;
    public static boolean RALLY_ADVANCED_CONSUMES_ITEM = false;
    public static int RALLY_EXISTING_NEAR_PLAYER_SKIP_DISTANCE = 8;
    public static int RALLY_TELEPORT_MIN_DISTANCE = 6;
    public static int RALLY_TELEPORT_MAX_DISTANCE = 12;
    public static String RALLY_BLACKLIST_ENTITY_ID_CONTAINS = "blimp,ender_dragon,wither,warden";

    public static boolean INTERNAL_TOKEN_COOLDOWNS_ENABLED = true;

    public static boolean MERCENARY_ENABLED = true;
    public static boolean MERCENARY_REQUIRE_ACTIVE_RAID = false;
    public static int MERCENARY_DURATION_TICKS = 36000;
    public static int MERCENARY_COOLDOWN_TICKS = 1200;
    public static int MERCENARY_SUMMON_COUNT = 2;
    public static int MERCENARY_MAX_ACTIVE_PER_PLAYER = 4;
    public static int MERCENARY_FOLLOW_START_DISTANCE = 12;
    public static int MERCENARY_TELEPORT_DISTANCE = 32;
    public static int MERCENARY_TICK_INTERVAL_TICKS = 20;
    public static double MERCENARY_FOLLOW_SPEED = 1.15D;
    public static boolean MERCENARY_DISCARD_ON_RAID_END = false;

    public static boolean MERCENARY_EFFECTS_ENABLED = true;
    public static boolean MERCENARY_EFFECTS_APPLY_ON_SPAWN_ONLY = true;
    public static boolean MERCENARY_EFFECTS_REPAIR_MISSING = true;
    public static int MERCENARY_EFFECTS_REPAIR_INTERVAL_TICKS = 600;

    public static boolean MERCENARY_GLOWING_ENABLED = true;
    public static String MERCENARY_GLOWING_TEAM_NAME = "rep_merc_gold";
    public static String MERCENARY_GLOWING_TEAM_COLOR = "GOLD";
    public static boolean MERCENARY_GLOWING_PER_PLAYER_COLOR = true;
    public static boolean MERCENARY_GLOWING_EXCLUSIVE_PLAYER_COLORS = true;
    public static boolean MERCENARY_GLOWING_RELEASE_COLOR_WHEN_NO_ACTIVE_MERCENARIES = false;
    public static String MERCENARY_GLOWING_COLOR_EXHAUSTED_POLICY = "DENY";
    public static String MERCENARY_GLOWING_COLOR_PALETTE = "GOLD,YELLOW,GREEN,AQUA,BLUE,LIGHT_PURPLE,RED,DARK_PURPLE,DARK_RED,DARK_AQUA,WHITE,GRAY";
    public static String MERCENARY_GLOWING_TEAM_PREFIX = "rep_m_";

    public static boolean MERCENARY_PERSISTENCE_ENABLED = true;
    public static int MERCENARY_PERSISTENCE_SCAN_INTERVAL_TICKS = 100;
    public static int MERCENARY_PERSISTENCE_SCAN_RADIUS = 384;

    public static boolean MERCENARY_DAMAGE_PROTECTION_PVP_AWARE = true;
    public static boolean MERCENARY_DAMAGE_PROTECTION_OWNER_CANNOT_DAMAGE = true;
    public static boolean MERCENARY_DAMAGE_PROTECTION_BLOCK_ALL_PLAYER_DAMAGE_WHEN_PVP_DISABLED = true;
    public static boolean MERCENARY_DAMAGE_PROTECTION_ALLOW_OTHER_PLAYERS_DAMAGE_WHEN_PVP_ENABLED = true;

    public static boolean DEBUG_LOGS_ENABLED = false;

    public static void loadOrCreate() {
        Path configDir = Path.of("config", "raid_enhancement_patch");
        Path configFile = configDir.resolve("battle_support_items.properties");
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(configFile)) {
                writeDefaultConfig(configFile);
                System.out.println("[Raid Enhancement Patch] Created default battle support item config: " + configFile.toAbsolutePath());
            }
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            ENABLED = readBool(properties, "enabled", ENABLED);
            REQUIRE_ACTIVE_RAID = readBool(properties, "requireActiveRaid", REQUIRE_ACTIVE_RAID);
            SHOW_CHAT_MESSAGES = readBool(properties, "showChatMessages", SHOW_CHAT_MESSAGES);
            CONSUME_ITEM_ON_SUCCESS = readBool(properties, "consumeItemOnSuccess", CONSUME_ITEM_ON_SUCCESS);
            USE_RADIUS = readInt(properties, "useRadius", USE_RADIUS, 16, 384);
            DEFAULT_DURATION_TICKS = readInt(properties, "defaultDurationTicks", DEFAULT_DURATION_TICKS, 20, 72000);
            DEFAULT_COOLDOWN_TICKS = readInt(properties, "defaultCooldownTicks", DEFAULT_COOLDOWN_TICKS, 0, 72000);
            AFFECT_MERCENARY_GOLEMS = readBool(properties, "affectMercenaryGolems", AFFECT_MERCENARY_GOLEMS);

            STRENGTH_ENABLED = readBool(properties, "strength.enabled", STRENGTH_ENABLED);
            STRENGTH_DURATION_TICKS = readInt(properties, "strength.durationTicks", STRENGTH_DURATION_TICKS, 20, 72000);
            STRENGTH_COOLDOWN_TICKS = readInt(properties, "strength.cooldownTicks", STRENGTH_COOLDOWN_TICKS, 0, 72000);

            SHIELD_ENABLED = readBool(properties, "shield.enabled", SHIELD_ENABLED);
            SHIELD_DURATION_TICKS = readInt(properties, "shield.durationTicks", SHIELD_DURATION_TICKS, 20, 72000);
            SHIELD_COOLDOWN_TICKS = readInt(properties, "shield.cooldownTicks", SHIELD_COOLDOWN_TICKS, 0, 72000);
            BASIC_SHIELD_TEMP_HEALTH = readFloat(properties, "shield.basicTempHealth", BASIC_SHIELD_TEMP_HEALTH, 0.0F, 500.0F);
            ADVANCED_SHIELD_TEMP_HEALTH = readFloat(properties, "shield.advancedTempHealth", ADVANCED_SHIELD_TEMP_HEALTH, 0.0F, 500.0F);
            // 0.8.9.7.9: automatically migrate the old shipped shield defaults so existing
            // configs do not silently keep the broken/underpowered 20/50 values. Custom
            // non-default values are respected.
            if (Float.compare(BASIC_SHIELD_TEMP_HEALTH, 20.0F) == 0) {
                BASIC_SHIELD_TEMP_HEALTH = 30.0F;
            }
            if (Float.compare(ADVANCED_SHIELD_TEMP_HEALTH, 50.0F) == 0) {
                ADVANCED_SHIELD_TEMP_HEALTH = 100.0F;
            }

            SWIFTNESS_ENABLED = readBool(properties, "swiftness.enabled", SWIFTNESS_ENABLED);
            SWIFTNESS_DURATION_TICKS = readInt(properties, "swiftness.durationTicks", SWIFTNESS_DURATION_TICKS, 20, 72000);
            SWIFTNESS_COOLDOWN_TICKS = readInt(properties, "swiftness.cooldownTicks", SWIFTNESS_COOLDOWN_TICKS, 0, 72000);

            HUNTER_ENABLED = readBool(properties, "hunter.enabled", HUNTER_ENABLED);
            HUNTER_DURATION_TICKS = readInt(properties, "hunter.durationTicks", HUNTER_DURATION_TICKS, 20, 72000);
            HUNTER_COOLDOWN_TICKS = readInt(properties, "hunter.cooldownTicks", HUNTER_COOLDOWN_TICKS, 0, 72000);

            FIRE_ENABLED = readBool(properties, "fire.enabled", FIRE_ENABLED);
            FIRE_DURATION_TICKS = readInt(properties, "fire.durationTicks", FIRE_DURATION_TICKS, 20, 72000);
            FIRE_COOLDOWN_TICKS = readInt(properties, "fire.cooldownTicks", FIRE_COOLDOWN_TICKS, 0, 72000);
            ADVANCED_FIRE_ADDS_REGENERATION = readBool(properties, "fire.advancedAddsRegeneration", ADVANCED_FIRE_ADDS_REGENERATION);

            INSIGHT_ENABLED = readBool(properties, "insight.enabled", INSIGHT_ENABLED);
            INSIGHT_BASIC_WAVE_COUNT = readInt(properties, "insight.basicWaveCount", INSIGHT_BASIC_WAVE_COUNT, 1, 16);
            INSIGHT_GLOW_DURATION_TICKS = readInt(properties, "insight.glowDurationTicks", INSIGHT_GLOW_DURATION_TICKS, 20, 72000);
            INSIGHT_COOLDOWN_TICKS = readInt(properties, "insight.cooldownTicks", INSIGHT_COOLDOWN_TICKS, 0, 72000);

            RALLY_ENABLED = readBool(properties, "rally.enabled", RALLY_ENABLED);
            RALLY_BASIC_COUNT = readInt(properties, "rally.basicCount", RALLY_BASIC_COUNT, 1, 32);
            RALLY_ADVANCED_COUNT = readInt(properties, "rally.advancedCount", RALLY_ADVANCED_COUNT, 1, 64);
            RALLY_BASIC_COOLDOWN_TICKS = readInt(properties, "rally.basicCooldownTicks", RALLY_BASIC_COOLDOWN_TICKS, 0, 72000);
            RALLY_ADVANCED_COOLDOWN_TICKS = readInt(properties, "rally.advancedCooldownTicks", RALLY_ADVANCED_COOLDOWN_TICKS, 0, 72000);
            RALLY_BASIC_CONSUMES_ITEM = readBool(properties, "rally.basicConsumesItem", RALLY_BASIC_CONSUMES_ITEM);
            RALLY_ADVANCED_CONSUMES_ITEM = readBool(properties, "rally.advancedConsumesItem", RALLY_ADVANCED_CONSUMES_ITEM);
            RALLY_EXISTING_NEAR_PLAYER_SKIP_DISTANCE = readInt(properties, "rally.skipRaidersAlreadyWithin", RALLY_EXISTING_NEAR_PLAYER_SKIP_DISTANCE, 0, 64);
            RALLY_TELEPORT_MIN_DISTANCE = readInt(properties, "rally.teleportMinDistance", RALLY_TELEPORT_MIN_DISTANCE, 1, 64);
            RALLY_TELEPORT_MAX_DISTANCE = readInt(properties, "rally.teleportMaxDistance", RALLY_TELEPORT_MAX_DISTANCE, Math.max(1, RALLY_TELEPORT_MIN_DISTANCE), 128);
            RALLY_BLACKLIST_ENTITY_ID_CONTAINS = readString(properties, "rally.blacklistEntityIdContains", RALLY_BLACKLIST_ENTITY_ID_CONTAINS, 0, 512);

            INTERNAL_TOKEN_COOLDOWNS_ENABLED = readBool(properties, "internalTokenCooldowns.enabled", INTERNAL_TOKEN_COOLDOWNS_ENABLED);

            boolean legacyMercenaryConfig = !properties.containsKey("mercenary.persistence.enabled")
                    && !properties.containsKey("mercenary.effects.applyOnSpawnOnly")
                    && !properties.containsKey("mercenary.glowing.enabled")
                    && !properties.containsKey("mercenary.damageProtection.pvpAware");

            MERCENARY_ENABLED = readBool(properties, "mercenary.enabled", MERCENARY_ENABLED);
            MERCENARY_REQUIRE_ACTIVE_RAID = readBool(properties, "mercenary.requireActiveRaid", MERCENARY_REQUIRE_ACTIVE_RAID);
            MERCENARY_DURATION_TICKS = readInt(properties, "mercenary.durationTicks", MERCENARY_DURATION_TICKS, 20, 72000);
            MERCENARY_COOLDOWN_TICKS = readInt(properties, "mercenary.cooldownTicks", MERCENARY_COOLDOWN_TICKS, 0, 72000);
            MERCENARY_SUMMON_COUNT = readInt(properties, "mercenary.summonCount", MERCENARY_SUMMON_COUNT, 1, 8);
            MERCENARY_MAX_ACTIVE_PER_PLAYER = readInt(properties, "mercenary.maxActivePerPlayer", MERCENARY_MAX_ACTIVE_PER_PLAYER, 1, 16);
            MERCENARY_FOLLOW_START_DISTANCE = readInt(properties, "mercenary.followStartDistance", MERCENARY_FOLLOW_START_DISTANCE, 4, 64);
            MERCENARY_TELEPORT_DISTANCE = readInt(properties, "mercenary.teleportDistance", MERCENARY_TELEPORT_DISTANCE, 8, 256);
            MERCENARY_TICK_INTERVAL_TICKS = readInt(properties, "mercenary.tickIntervalTicks", MERCENARY_TICK_INTERVAL_TICKS, 5, 200);
            MERCENARY_FOLLOW_SPEED = readDouble(properties, "mercenary.followSpeed", MERCENARY_FOLLOW_SPEED, 0.1D, 4.0D);
            MERCENARY_DISCARD_ON_RAID_END = readBool(properties, "mercenary.discardOnRaidEnd", MERCENARY_DISCARD_ON_RAID_END);

            if (legacyMercenaryConfig) {
                if (MERCENARY_REQUIRE_ACTIVE_RAID) {
                    MERCENARY_REQUIRE_ACTIVE_RAID = false;
                }
                if (MERCENARY_DURATION_TICKS == 12000) {
                    MERCENARY_DURATION_TICKS = 36000;
                }
                if (MERCENARY_MAX_ACTIVE_PER_PLAYER == 2) {
                    MERCENARY_MAX_ACTIVE_PER_PLAYER = 4;
                }
            }

            MERCENARY_EFFECTS_ENABLED = readBool(properties, "mercenary.effects.enabled", MERCENARY_EFFECTS_ENABLED);
            MERCENARY_EFFECTS_APPLY_ON_SPAWN_ONLY = readBool(properties, "mercenary.effects.applyOnSpawnOnly", MERCENARY_EFFECTS_APPLY_ON_SPAWN_ONLY);
            MERCENARY_EFFECTS_REPAIR_MISSING = readBool(properties, "mercenary.effects.repairMissingEffects", MERCENARY_EFFECTS_REPAIR_MISSING);
            MERCENARY_EFFECTS_REPAIR_INTERVAL_TICKS = readInt(properties, "mercenary.effects.repairIntervalTicks", MERCENARY_EFFECTS_REPAIR_INTERVAL_TICKS, 20, 72000);

            MERCENARY_GLOWING_ENABLED = readBool(properties, "mercenary.glowing.enabled", MERCENARY_GLOWING_ENABLED);
            MERCENARY_GLOWING_TEAM_NAME = readString(properties, "mercenary.glowing.teamName", MERCENARY_GLOWING_TEAM_NAME, 1, 32);
            MERCENARY_GLOWING_TEAM_COLOR = readString(properties, "mercenary.glowing.teamColor", MERCENARY_GLOWING_TEAM_COLOR, 2, 32);
            MERCENARY_GLOWING_PER_PLAYER_COLOR = readBool(properties, "mercenary.glowing.perPlayerColor", MERCENARY_GLOWING_PER_PLAYER_COLOR);
            MERCENARY_GLOWING_EXCLUSIVE_PLAYER_COLORS = readBool(properties, "mercenary.glowing.exclusivePlayerColors", MERCENARY_GLOWING_EXCLUSIVE_PLAYER_COLORS);
            MERCENARY_GLOWING_RELEASE_COLOR_WHEN_NO_ACTIVE_MERCENARIES = readBool(properties, "mercenary.glowing.releaseColorWhenNoActiveMercenaries", MERCENARY_GLOWING_RELEASE_COLOR_WHEN_NO_ACTIVE_MERCENARIES);
            MERCENARY_GLOWING_COLOR_EXHAUSTED_POLICY = readString(properties, "mercenary.glowing.colorExhaustedPolicy", MERCENARY_GLOWING_COLOR_EXHAUSTED_POLICY, 3, 16);
            MERCENARY_GLOWING_COLOR_PALETTE = readString(properties, "mercenary.glowing.colorPalette", MERCENARY_GLOWING_COLOR_PALETTE, 2, 512);
            MERCENARY_GLOWING_TEAM_PREFIX = readString(properties, "mercenary.glowing.teamPrefix", MERCENARY_GLOWING_TEAM_PREFIX, 1, 16);

            MERCENARY_PERSISTENCE_ENABLED = readBool(properties, "mercenary.persistence.enabled", MERCENARY_PERSISTENCE_ENABLED);
            MERCENARY_PERSISTENCE_SCAN_INTERVAL_TICKS = readInt(properties, "mercenary.persistence.scanIntervalTicks", MERCENARY_PERSISTENCE_SCAN_INTERVAL_TICKS, 20, 1200);
            MERCENARY_PERSISTENCE_SCAN_RADIUS = readInt(properties, "mercenary.persistence.scanRadius", MERCENARY_PERSISTENCE_SCAN_RADIUS, 32, 1024);

            MERCENARY_DAMAGE_PROTECTION_PVP_AWARE = readBool(properties, "mercenary.damageProtection.pvpAware", MERCENARY_DAMAGE_PROTECTION_PVP_AWARE);
            MERCENARY_DAMAGE_PROTECTION_OWNER_CANNOT_DAMAGE = readBool(properties, "mercenary.damageProtection.ownerCannotDamage", MERCENARY_DAMAGE_PROTECTION_OWNER_CANNOT_DAMAGE);
            MERCENARY_DAMAGE_PROTECTION_BLOCK_ALL_PLAYER_DAMAGE_WHEN_PVP_DISABLED = readBool(properties, "mercenary.damageProtection.blockAllPlayerDamageWhenPvpDisabled", MERCENARY_DAMAGE_PROTECTION_BLOCK_ALL_PLAYER_DAMAGE_WHEN_PVP_DISABLED);
            MERCENARY_DAMAGE_PROTECTION_ALLOW_OTHER_PLAYERS_DAMAGE_WHEN_PVP_ENABLED = readBool(properties, "mercenary.damageProtection.allowOtherPlayersDamageWhenPvpEnabled", MERCENARY_DAMAGE_PROTECTION_ALLOW_OTHER_PLAYERS_DAMAGE_WHEN_PVP_ENABLED);

            DEBUG_LOGS_ENABLED = readBool(properties, "debug.enabled", DEBUG_LOGS_ENABLED);
            writeDefaultConfig(configDir.resolve("battle_support_items.default.properties"));
            System.out.println("[Raid Enhancement Patch] Loaded battle support item config " + CONFIG_STAGE
                    + ": enabled=" + ENABLED
                    + ", useRadius=" + USE_RADIUS
                    + ", defaultDurationTicks=" + DEFAULT_DURATION_TICKS
                    + ", mercenaryEnabled=" + MERCENARY_ENABLED
                    + ", mercenaryDurationTicks=" + MERCENARY_DURATION_TICKS + ".");
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Battle support item config load failed; using built-in defaults: " + throwable);
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

    private static float readFloat(Properties properties, String key, float fallback, float min, float max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            float parsed = Float.parseFloat(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            System.out.println("[Raid Enhancement Patch] Invalid float config value for " + key + "=" + value + "; using " + fallback + ".");
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
        } catch (NumberFormatException exception) {
            System.out.println("[Raid Enhancement Patch] Invalid double config value for " + key + "=" + value + "; using " + fallback + ".");
            return fallback;
        }
    }

    private static String readString(Properties properties, String key, String fallback, int minLength, int maxLength) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() < minLength || trimmed.length() > maxLength) {
            System.out.println("[Raid Enhancement Patch] Invalid string config value for " + key + "=" + value + "; using " + fallback + ".");
            return fallback;
        }
        return trimmed;
    }

    private static void writeDefaultConfig(Path file) throws IOException {
        List<String> lines = List.of(
                "# Raid Enhancement Patch - Battle Support Items Config",
                "# Stage: " + CONFIG_STAGE,
                "# File created in config/raid_enhancement_patch/ . Restart the game/server after editing.",
                "# This config controls only item-driven village battle support. It does not change HUD, extra waves, bridge logic, native raid victory/failure, or raider movement.",
                "",
                "enabled=true",
                "requireActiveRaid=true",
                "showChatMessages=true",
                "consumeItemOnSuccess=true",
                "useRadius=128",
                "defaultDurationTicks=6000",
                "defaultCooldownTicks=1200",
                "affectMercenaryGolems=false",
                "",
                "strength.enabled=true",
                "strength.durationTicks=6000",
                "strength.cooldownTicks=1200",
                "",
                "shield.enabled=true",
                "shield.durationTicks=6000",
                "shield.cooldownTicks=1200",
                "shield.basicTempHealth=30.0",
                "shield.advancedTempHealth=100.0",
                "",
                "swiftness.enabled=true",
                "swiftness.durationTicks=6000",
                "swiftness.cooldownTicks=1200",
                "",
                "hunter.enabled=true",
                "hunter.durationTicks=6000",
                "hunter.cooldownTicks=1200",
                "",
                "fire.enabled=true",
                "fire.durationTicks=6000",
                "fire.cooldownTicks=1200",
                "fire.advancedAddsRegeneration=true",
                "",
                "insight.enabled=true",
                "insight.basicWaveCount=3",
                "insight.glowDurationTicks=6000",
                "insight.cooldownTicks=1200",
                "",
                "rally.enabled=true",
                "rally.basicCount=5",
                "rally.advancedCount=10",
                "rally.basicCooldownTicks=1800",
                "rally.advancedCooldownTicks=2400",
                "rally.basicConsumesItem=true",
                "rally.advancedConsumesItem=false",
                "rally.skipRaidersAlreadyWithin=8",
                "rally.teleportMinDistance=6",
                "rally.teleportMaxDistance=12",
                "rally.blacklistEntityIdContains=blimp,ender_dragon,wither,warden",
                "",
                "# Internal cooldowns enforce gameplay cooldowns without vanilla item-cooldown overlays in the creative inventory.",
                "internalTokenCooldowns.enabled=true",
                "",
                "mercenary.enabled=true",
                "mercenary.requireActiveRaid=false",
                "mercenary.durationTicks=36000",
                "mercenary.cooldownTicks=1200",
                "mercenary.summonCount=2",
                "mercenary.maxActivePerPlayer=4",
                "mercenary.followStartDistance=12",
                "mercenary.teleportDistance=32",
                "mercenary.tickIntervalTicks=20",
                "mercenary.followSpeed=1.15",
                "mercenary.discardOnRaidEnd=false",
                "mercenary.effects.enabled=true",
                "mercenary.effects.applyOnSpawnOnly=true",
                "mercenary.effects.repairMissingEffects=true",
                "mercenary.effects.repairIntervalTicks=600",
                "mercenary.glowing.enabled=true",
                "mercenary.glowing.teamName=rep_merc_gold",
                "mercenary.glowing.teamColor=GOLD",
                "mercenary.glowing.perPlayerColor=true",
                "mercenary.glowing.exclusivePlayerColors=true",
                "mercenary.glowing.releaseColorWhenNoActiveMercenaries=false",
                "mercenary.glowing.colorExhaustedPolicy=DENY",
                "mercenary.glowing.colorPalette=GOLD,YELLOW,GREEN,AQUA,BLUE,LIGHT_PURPLE,RED,DARK_PURPLE,DARK_RED,DARK_AQUA,WHITE,GRAY",
                "mercenary.glowing.teamPrefix=rep_m_",
                "mercenary.persistence.enabled=true",
                "mercenary.persistence.scanIntervalTicks=100",
                "mercenary.persistence.scanRadius=384",
                "mercenary.damageProtection.pvpAware=true",
                "mercenary.damageProtection.ownerCannotDamage=true",
                "mercenary.damageProtection.blockAllPlayerDamageWhenPvpDisabled=true",
                "mercenary.damageProtection.allowOtherPlayersDamageWhenPvpEnabled=true",
                "",
                "debug.enabled=false"
        );
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }
}
