package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.config.VillageFavorConfig;
import com.noah.raidenhancement.config.VictorySettlementConfig;
import com.noah.raidenhancement.raid.RaidKeyDiagnostics;
import net.minecraft.server.level.ServerLevel;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/** Server-side single source of truth for player x rescued-village favor rows. */
public final class VillageFavorState {
    private static final Map<String, VillageFavorRecord> RECORDS = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean dirty;
    private static Path loadedDataFile;
    private static boolean warnedLoad;
    private static boolean warnedSave;

    private VillageFavorState() {
    }

    public static synchronized int favorLevelFor(ServerLevel level, String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid) {
        ensureLoaded(level);
        VillageFavorRecord record = RECORDS.get(recordKey(dimensionId, centerX, centerY, centerZ, playerUuid));
        return record == null ? 0 : Math.max(0, record.favorLevel);
    }

    public static synchronized VillageFavorRecord recordVictory(ServerLevel level, String dimensionId, int centerX, int centerY, int centerZ,
                                                                int radius, UUID playerUuid, long gameTime) {
        return recordVictory(level, dimensionId, centerX, centerY, centerZ, radius, playerUuid, gameTime,
                1, "NORMAL", false);
    }

    public static synchronized VillageFavorRecord recordVictory(ServerLevel level, String dimensionId, int centerX, int centerY, int centerZ,
                                                                int radius, UUID playerUuid, long gameTime,
                                                                int omenLevel, String difficultyName, boolean extraWaveCompleted) {
        if (playerUuid == null || dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        ensureLoaded(level);
        int safeRadius = Math.max(16, radius);
        String key = recordKey(dimensionId, centerX, centerY, centerZ, playerUuid);
        VillageFavorRecord record = RECORDS.get(key);
        int safeOmen = Math.max(1, Math.min(5, omenLevel));
        int scoreDelta = meritDelta(record, safeOmen, difficultyName, extraWaveCompleted);
        if (record == null) {
            record = new VillageFavorRecord(key, dimensionId, centerX, centerY, centerZ, safeRadius, playerUuid, gameTime);
            record.victoryCount = 1;
            record.raidMeritScore = Math.max(1, scoreDelta);
            record.highestOmenLevelWon = safeOmen;
            record.favorLevel = VillageFavorConfig.favorLevelFromScore(record.raidMeritScore);
            RECORDS.put(key, record);
        } else {
            record.victoryCount = Math.max(0, record.victoryCount) + 1;
            record.highestOmenLevelWon = Math.max(Math.max(1, record.highestOmenLevelWon), safeOmen);
            if (VillageFavorConfig.ALLOW_MULTIPLE_VICTORIES_INCREASE_FAVOR) {
                record.raidMeritScore = Math.max(0, record.raidMeritScore) + Math.max(0, scoreDelta);
            } else {
                record.raidMeritScore = Math.max(Math.max(0, record.raidMeritScore), Math.max(0, scoreDelta));
            }
            record.favorLevel = VillageFavorConfig.favorLevelFromScore(record.raidMeritScore);
            record.lastUpdatedTime = Math.max(0L, gameTime);
        }
        record.favorLevel = Math.max(1, Math.min(Math.max(1, VillageFavorConfig.MAX_FAVOR_LEVEL), record.favorLevel));
        record.lastUpdatedTime = Math.max(record.lastUpdatedTime, gameTime);
        dirty = true;
        save(level);
        return record;
    }

    private static int meritDelta(VillageFavorRecord existing, int omenLevel, String difficultyName, boolean extraWaveCompleted) {
        int delta = Math.max(0, VillageFavorConfig.BASE_RAID_WIN_SCORE)
                + Math.max(0, omenLevel - 1) * Math.max(0, VillageFavorConfig.OMEN_LEVEL_SCORE_BONUS);
        String difficulty = difficultyName == null ? "" : difficultyName.toUpperCase(java.util.Locale.ROOT);
        if (difficulty.contains("HARD")) {
            delta += Math.max(0, VillageFavorConfig.HARD_DIFFICULTY_SCORE_BONUS);
        }
        if (extraWaveCompleted) {
            delta += Math.max(0, VillageFavorConfig.EXTRA_WAVE_SCORE_BONUS);
        }
        if (VillageFavorConfig.SAME_VILLAGE_REPEATED_RAID_DIMINISHING && existing != null && existing.victoryCount > 0) {
            delta = Math.max(1, (int) Math.ceil(delta * 0.75D));
        }
        return Math.max(1, delta);
    }

    public static synchronized Optional<VillageFavorRecord> findForInteraction(ServerLevel level, String dimensionId, UUID playerUuid,
                                                                               double x, double y, double z) {
        if (playerUuid == null || dimensionId == null || dimensionId.isBlank()) {
            return Optional.empty();
        }
        ensureLoaded(level);
        return RECORDS.values().stream()
                .filter(record -> record != null
                        && playerUuid.equals(record.playerUuid)
                        && dimensionId.equals(record.dimensionId)
                        && record.contains(x, y, z))
                .min(Comparator.comparingDouble(record -> record.distanceSq(x, y, z)));
    }

    public static synchronized void markGreeting(ServerLevel level, VillageFavorRecord record, long gameTime) {
        if (record == null) {
            return;
        }
        record.lastGreetingTime = Math.max(0L, gameTime);
        record.lastUpdatedTime = Math.max(record.lastUpdatedTime, gameTime);
        dirty = true;
        save(level);
    }

    public static synchronized boolean canClaimGift(VillageFavorRecord record, long gameTime) {
        if (record == null) {
            return false;
        }
        resetGiftPeriodIfNeeded(record, gameTime);
        if (VillageFavorConfig.MAX_GIFT_PER_VILLAGE_PER_PLAYER > 0
                && record.totalClaimedGiftCount >= VillageFavorConfig.MAX_GIFT_PER_VILLAGE_PER_PLAYER) {
            return false;
        }
        if (VillageFavorConfig.MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY > 0
                && record.giftClaimsInCurrentPeriod >= VillageFavorConfig.MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY) {
            return false;
        }
        long cooldown = Math.max(200L, VillageFavorConfig.GIFT_COOLDOWN_TICKS);
        return record.lastGiftTime <= 0L || gameTime - record.lastGiftTime >= cooldown;
    }

    public static synchronized void markGift(ServerLevel level, VillageFavorRecord record, long gameTime) {
        if (record == null) {
            return;
        }
        resetGiftPeriodIfNeeded(record, gameTime);
        record.lastGiftTime = Math.max(0L, gameTime);
        record.totalClaimedGiftCount = Math.max(0, record.totalClaimedGiftCount) + 1;
        record.giftClaimsInCurrentPeriod = Math.max(0, record.giftClaimsInCurrentPeriod) + 1;
        record.lastUpdatedTime = Math.max(record.lastUpdatedTime, gameTime);
        dirty = true;
        save(level);
    }

    private static void resetGiftPeriodIfNeeded(VillageFavorRecord record, long gameTime) {
        long period = Math.max(1200L, VillageFavorConfig.GIFT_CLAIM_PERIOD_TICKS);
        if (record.giftPeriodStartTime <= 0L || gameTime - record.giftPeriodStartTime >= period || gameTime < record.giftPeriodStartTime) {
            record.giftPeriodStartTime = Math.max(0L, gameTime);
            record.giftClaimsInCurrentPeriod = 0;
        }
    }

    public static synchronized void ensureLoaded(ServerLevel level) {
        Path file = dataFile(level);
        RaidKeyDiagnostics.logFavorStoragePath(level, file, file != null && file.equals(VillageFavorConfig.fallbackDataFile()));
        if (loaded && file.equals(loadedDataFile)) {
            return;
        }
        if (loaded && dirty && loadedDataFile != null) {
            saveTo(loadedDataFile);
        }
        RECORDS.clear();
        loaded = true;
        loadedDataFile = file;
        dirty = false;
        load(file);
        if (RECORDS.isEmpty() && VillageFavorConfig.MIGRATE_LEGACY_FAVOR_FILE) {
            migrateLegacyFavor(level);
        }
    }

    public static synchronized void save(ServerLevel level) {
        if (!dirty) {
            return;
        }
        saveTo(dataFile(level));
    }

    private static void saveTo(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            properties.setProperty("dataVersion", Integer.toString(VillageFavorRecord.DATA_VERSION));
            int i = 0;
            for (VillageFavorRecord record : RECORDS.values()) {
                if (record == null || record.playerUuid == null) {
                    continue;
                }
                String prefix = "record" + i++;
                properties.setProperty(prefix + ".key", record.key);
                properties.setProperty(prefix + ".dimension", record.dimensionId);
                properties.setProperty(prefix + ".centerX", Integer.toString(record.centerX));
                properties.setProperty(prefix + ".centerY", Integer.toString(record.centerY));
                properties.setProperty(prefix + ".centerZ", Integer.toString(record.centerZ));
                properties.setProperty(prefix + ".radius", Integer.toString(record.radius));
                properties.setProperty(prefix + ".player", record.playerUuid.toString());
                properties.setProperty(prefix + ".favorLevel", Integer.toString(record.favorLevel));
                properties.setProperty(prefix + ".victoryCount", Integer.toString(record.victoryCount));
                properties.setProperty(prefix + ".highestOmenLevelWon", Integer.toString(record.highestOmenLevelWon));
                properties.setProperty(prefix + ".raidMeritScore", Integer.toString(record.raidMeritScore));
                properties.setProperty(prefix + ".createdTime", Long.toString(record.createdTime));
                properties.setProperty(prefix + ".lastUpdatedTime", Long.toString(record.lastUpdatedTime));
                properties.setProperty(prefix + ".lastGiftTime", Long.toString(record.lastGiftTime));
                properties.setProperty(prefix + ".lastGreetingTime", Long.toString(record.lastGreetingTime));
                properties.setProperty(prefix + ".totalClaimedGiftCount", Integer.toString(record.totalClaimedGiftCount));
                properties.setProperty(prefix + ".giftClaimsInCurrentPeriod", Integer.toString(record.giftClaimsInCurrentPeriod));
                properties.setProperty(prefix + ".giftPeriodStartTime", Long.toString(record.giftPeriodStartTime));
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                properties.store(writer, "Raid Enhancement Patch village favor relation data V2");
            }
            dirty = false;
        } catch (Throwable throwable) {
            if (!warnedSave) {
                warnedSave = true;
                System.out.println("[Raid Enhancement Patch] Failed to save village favor relation data: " + throwable);
            }
        }
    }

    private static void load(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            Map<String, Builder> builders = new HashMap<>();
            for (String name : properties.stringPropertyNames()) {
                int dot = name.indexOf('.');
                if (dot <= 0) {
                    continue;
                }
                String prefix = name.substring(0, dot);
                String field = name.substring(dot + 1);
                builders.computeIfAbsent(prefix, Builder::new).accept(field, properties.getProperty(name, ""));
            }
            for (Builder builder : builders.values()) {
                VillageFavorRecord record = builder.build();
                if (record != null) {
                    RECORDS.put(record.key, record);
                }
            }
        } catch (Throwable throwable) {
            if (!warnedLoad) {
                warnedLoad = true;
                System.out.println("[Raid Enhancement Patch] Failed to load village favor relation data; continuing with empty state: " + throwable);
            }
        }
    }

    private static void migrateLegacyFavor(ServerLevel level) {
        Path oldFile = VictorySettlementConfig.favorFile();
        if (oldFile == null || !Files.exists(oldFile)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(oldFile), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            Map<String, LegacyBuilder> builders = new HashMap<>();
            for (String name : properties.stringPropertyNames()) {
                int dot = name.indexOf('.');
                if (dot <= 0) {
                    continue;
                }
                String prefix = name.substring(0, dot);
                String field = name.substring(dot + 1);
                builders.computeIfAbsent(prefix, LegacyBuilder::new).accept(field, properties.getProperty(name, ""));
            }
            List<VillageFavorRecord> migrated = new ArrayList<>();
            for (LegacyBuilder builder : builders.values()) {
                VillageFavorRecord record = builder.build();
                if (record != null) {
                    migrated.add(record);
                }
            }
            for (VillageFavorRecord record : migrated) {
                RECORDS.putIfAbsent(record.key, record);
            }
            if (!migrated.isEmpty()) {
                dirty = true;
                save(level);
                if (VillageFavorConfig.ENABLE_DEBUG_LOG) {
                    System.out.println("[Raid Enhancement Patch] Migrated " + migrated.size() + " legacy village favor rows into V2 relation state.");
                }
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Legacy village favor migration failed and was skipped: " + throwable);
        }
    }

    private static Path dataFile(ServerLevel level) {
        Path worldData = worldDataDir(level);
        return worldData == null
                ? VillageFavorConfig.fallbackDataFile()
                : worldData.resolve("raid_enhancement_patch_village_favor.properties");
    }

    private static Path worldDataDir(ServerLevel level) {
        try {
            if (level == null) {
                return null;
            }
            Object server = level.getClass().getMethod("getServer").invoke(level);
            if (server == null) {
                return null;
            }
            Class<?> levelResource = Class.forName("net.minecraft.world.level.storage.LevelResource");
            Object rootResource = null;
            for (String fieldName : List.of("ROOT", "LEVEL_DATA_FILE")) {
                try {
                    Field field = levelResource.getField(fieldName);
                    rootResource = field.get(null);
                    if (rootResource != null) {
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (rootResource == null) {
                return null;
            }
            Method getWorldPath = server.getClass().getMethod("getWorldPath", levelResource);
            Object path = getWorldPath.invoke(server, rootResource);
            if (path instanceof Path root) {
                Path data = root.resolve("data");
                Files.createDirectories(data);
                return data;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String recordKey(String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid) {
        return sanitize(dimensionId) + "@" + centerX + "_" + centerY + "_" + centerZ + "@" + playerUuid;
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.replace(':', '_').replace('/', '_').replace(' ', '_').replace('#', '_');
    }

    private static int legacyPointsToLevel(int points) {
        int value = Math.max(0, points);
        if (value >= 90) {
            return Math.min(VillageFavorConfig.MAX_FAVOR_LEVEL, 3);
        }
        if (value >= 40) {
            return Math.min(VillageFavorConfig.MAX_FAVOR_LEVEL, 2);
        }
        if (value >= 10) {
            return 1;
        }
        return value > 0 ? 1 : 0;
    }

    private static final class Builder {
        private String key;
        private String dimension = "unknown";
        private int centerX;
        private int centerY;
        private int centerZ;
        private int radius = VillageFavorConfig.VILLAGE_RADIUS;
        private UUID player;
        private int favorLevel = 1;
        private int victoryCount = 1;
        private int highestOmenLevelWon = 1;
        private int raidMeritScore = 0;
        private long createdTime;
        private long lastUpdatedTime;
        private long lastGiftTime;
        private long lastGreetingTime;
        private int totalClaimedGiftCount;
        private int giftClaimsInCurrentPeriod;
        private long giftPeriodStartTime;

        Builder(String ignored) {
        }

        void accept(String field, String value) {
            try {
                switch (field) {
                    case "key" -> key = value;
                    case "dimension" -> dimension = value == null || value.isBlank() ? "unknown" : value;
                    case "centerX" -> centerX = Integer.parseInt(value.trim());
                    case "centerY" -> centerY = Integer.parseInt(value.trim());
                    case "centerZ" -> centerZ = Integer.parseInt(value.trim());
                    case "radius" -> radius = Integer.parseInt(value.trim());
                    case "player" -> player = UUID.fromString(value.trim());
                    case "favorLevel" -> favorLevel = Integer.parseInt(value.trim());
                    case "victoryCount" -> victoryCount = Integer.parseInt(value.trim());
                    case "highestOmenLevelWon" -> highestOmenLevelWon = Integer.parseInt(value.trim());
                    case "raidMeritScore" -> raidMeritScore = Integer.parseInt(value.trim());
                    case "createdTime" -> createdTime = Long.parseLong(value.trim());
                    case "lastUpdatedTime" -> lastUpdatedTime = Long.parseLong(value.trim());
                    case "lastGiftTime" -> lastGiftTime = Long.parseLong(value.trim());
                    case "lastGreetingTime" -> lastGreetingTime = Long.parseLong(value.trim());
                    case "totalClaimedGiftCount" -> totalClaimedGiftCount = Integer.parseInt(value.trim());
                    case "giftClaimsInCurrentPeriod" -> giftClaimsInCurrentPeriod = Integer.parseInt(value.trim());
                    case "giftPeriodStartTime" -> giftPeriodStartTime = Long.parseLong(value.trim());
                    default -> {
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        VillageFavorRecord build() {
            if (player == null) {
                return null;
            }
            String finalKey = key == null || key.isBlank() ? recordKey(dimension, centerX, centerY, centerZ, player) : key;
            VillageFavorRecord record = new VillageFavorRecord(finalKey, dimension, centerX, centerY, centerZ, radius, player, createdTime);
            record.victoryCount = Math.max(1, victoryCount);
            record.highestOmenLevelWon = Math.max(1, highestOmenLevelWon);
            record.raidMeritScore = raidMeritScore > 0 ? raidMeritScore : Math.max(1, record.victoryCount);
            record.favorLevel = Math.max(1, Math.min(VillageFavorConfig.MAX_FAVOR_LEVEL,
                    favorLevel > 0 ? favorLevel : VillageFavorConfig.favorLevelFromScore(record.raidMeritScore)));
            record.lastUpdatedTime = Math.max(0L, lastUpdatedTime);
            record.lastGiftTime = Math.max(0L, lastGiftTime);
            record.lastGreetingTime = Math.max(0L, lastGreetingTime);
            record.totalClaimedGiftCount = Math.max(0, totalClaimedGiftCount);
            record.giftClaimsInCurrentPeriod = Math.max(0, giftClaimsInCurrentPeriod);
            record.giftPeriodStartTime = Math.max(0L, giftPeriodStartTime);
            return record;
        }
    }

    private static final class LegacyBuilder {
        private String dimension = "unknown";
        private int centerX;
        private int centerY;
        private int centerZ;
        private UUID player;
        private int points;
        private long lastUpdate;

        LegacyBuilder(String ignored) {
        }

        void accept(String field, String value) {
            try {
                switch (field) {
                    case "dimension" -> dimension = value == null || value.isBlank() ? "unknown" : value;
                    case "centerX" -> centerX = Integer.parseInt(value.trim());
                    case "centerY" -> centerY = Integer.parseInt(value.trim());
                    case "centerZ" -> centerZ = Integer.parseInt(value.trim());
                    case "player" -> player = UUID.fromString(value.trim());
                    case "points" -> points = Integer.parseInt(value.trim());
                    case "lastUpdate" -> lastUpdate = Long.parseLong(value.trim());
                    default -> {
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        VillageFavorRecord build() {
            if (player == null || points <= 0) {
                return null;
            }
            String key = recordKey(dimension, centerX, centerY, centerZ, player);
            VillageFavorRecord record = new VillageFavorRecord(key, dimension, centerX, centerY, centerZ,
                    VillageFavorConfig.VILLAGE_RADIUS, player, lastUpdate);
            record.raidMeritScore = Math.max(1, points);
            record.favorLevel = Math.max(1, legacyPointsToLevel(points));
            record.victoryCount = 1;
            record.lastUpdatedTime = Math.max(0L, lastUpdate);
            return record;
        }
    }
}
