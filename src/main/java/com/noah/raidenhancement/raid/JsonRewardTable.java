package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.VictorySettlementConfig;
import com.noah.raidenhancement.config.VillageFavorConfig;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight JSON-ish reward reader for config/raid_enhancement_patch/settlement_rewards/*.json. */
public final class JsonRewardTable {
    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{([^{}]*)}", Pattern.DOTALL);

    private JsonRewardTable() {
    }

    public static List<ItemStack> roll(VictoryTier tier) {
        if (tier == null) {
            return List.of();
        }
        String fileName = switch (tier) {
            case PERFECT -> "perfect_victory.json";
            case VICTORY -> "victory.json";
            case COSTLY -> "costly_victory.json";
        };
        return rollFile(VictorySettlementConfig.rewardDir().resolve(fileName));
    }

    public static List<ItemStack> rollVillageFavorGift() {
        return rollFile(VillageFavorConfig.giftLootFile());
    }

    public static List<SweepExchangeEntry> loadSweepEntries() {
        Path file = VictorySettlementConfig.rewardDir().resolve("sweep_exchange.json");
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (!bool(json, "enabled", true)) {
                return List.of();
            }
            List<SweepExchangeEntry> entries = new ArrayList<>();
            for (String object : entryObjects(json)) {
                String item = string(object, "item", "");
                if (item.isBlank()) {
                    continue;
                }
                entries.add(new SweepExchangeEntry(item, bool(object, "enabled", true),
                        integer(object, "emeraldValue", 0), integer(object, "experienceValue", 0)));
            }
            return entries;
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Failed to load sweep_exchange.json; sweep conversion skipped once: " + throwable);
            return List.of();
        }
    }

    public static int baseExperience(String difficultyName, int omenLevel) {
        Path file = VictorySettlementConfig.rewardDir().resolve("experience_rewards.json");
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (!bool(json, "enabled", true)) {
                return 0;
            }
            String difficulty = difficultyName == null ? "NORMAL" : difficultyName.trim().toUpperCase(Locale.ROOT);
            String array = arrayForKey(json, difficulty);
            if (array.isBlank()) {
                array = arrayForKey(json, "NORMAL");
            }
            int[] values = parseIntArray(array);
            if (values.length <= 0) {
                return 0;
            }
            int index = Math.max(0, Math.min(values.length - 1, Math.max(1, omenLevel) - 1));
            return Math.max(0, values[index]);
        } catch (Throwable throwable) {
            int omen = Math.max(1, Math.min(5, omenLevel));
            String difficulty = difficultyName == null ? "NORMAL" : difficultyName.trim().toUpperCase(Locale.ROOT);
            return switch (difficulty) {
                case "EASY" -> new int[]{160, 220, 300, 380, 480}[omen - 1];
                case "HARD" -> new int[]{700, 900, 1100, 1300, 1507}[omen - 1];
                default -> new int[]{360, 480, 620, 780, 960}[omen - 1];
            };
        }
    }

    public static double gradeMultiplier(VictoryTier tier) {
        Path file = VictorySettlementConfig.rewardDir().resolve("experience_rewards.json");
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            String key = tier == null ? "victory" : tier.id();
            return decimal(json, key, switch (tier == null ? VictoryTier.VICTORY : tier) {
                case PERFECT -> 1.15D;
                case VICTORY -> 1.0D;
                case COSTLY -> 0.7D;
            });
        } catch (Throwable ignored) {
            return switch (tier == null ? VictoryTier.VICTORY : tier) {
                case PERFECT -> 1.15D;
                case VICTORY -> 1.0D;
                case COSTLY -> 0.7D;
            };
        }
    }

    public static double favorXpBonus(int level) {
        Path file = VictorySettlementConfig.rewardDir().resolve("favor_rewards.json");
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (!bool(json, "enabled", true)) {
                return 0.0D;
            }
            double[] values = parseDoubleArray(arrayForKey(json, "xpBonusByLevel"));
            if (values.length <= 0) {
                return 0.0D;
            }
            int index = Math.max(0, Math.min(values.length - 1, level));
            return Math.max(0.0D, values[index]);
        } catch (Throwable ignored) {
            return switch (Math.max(0, Math.min(5, level))) {
                case 1 -> 0.05D;
                case 2 -> 0.08D;
                case 3 -> 0.12D;
                case 4 -> 0.16D;
                case 5 -> 0.20D;
                default -> 0.0D;
            };
        }
    }

    public static List<ItemStack> rollFile(Path file) {
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (!bool(json, "enabled", true)) {
                return List.of();
            }
            List<ItemStack> result = new ArrayList<>();
            for (String object : entryObjects(json)) {
                String itemId = string(object, "item", "");
                if (itemId.isBlank()) {
                    continue;
                }
                double chance = decimal(object, "chance", 1.0D);
                if (chance < 1.0D && ThreadLocalRandom.current().nextDouble() > chance) {
                    continue;
                }
                int min = integer(object, "min", 1);
                int max = Math.max(min, integer(object, "max", min));
                int count = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
                ItemStack stack = VictorySettlementController.makeItemStack(itemId, count);
                if (stack != null && !stack.isEmpty()) {
                    result.add(stack);
                }
            }
            return result;
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Failed to load settlement reward table " + file + "; rewards skipped once: " + throwable);
            return List.of();
        }
    }

    private static List<String> entryObjects(String json) {
        String entries = sectionArray(json, "entries");
        if (entries.isBlank()) {
            return List.of();
        }
        List<String> objects = new ArrayList<>();
        Matcher matcher = OBJECT_PATTERN.matcher(entries);
        while (matcher.find()) {
            objects.add(matcher.group(1));
        }
        return objects;
    }

    private static String sectionArray(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String arrayForKey(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static boolean bool(String object, String key, boolean fallback) {
        String value = rawValue(object, key);
        if (value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("true")) {
            return true;
        }
        if (normalized.startsWith("false")) {
            return false;
        }
        return fallback;
    }

    private static int integer(String object, String key, int fallback) {
        String value = rawValue(object, key);
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", "").trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static double decimal(String object, String key, double fallback) {
        String value = rawValue(object, key);
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.replaceAll("[^0-9eE+\\-.]", "").trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String string(String object, String key, String fallback) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(object);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static String rawValue(String object, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*([^,}\\n]+)");
        Matcher matcher = pattern.matcher(object);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static int[] parseIntArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return new int[0];
        }
        String[] parts = raw.split(",");
        int[] values = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            try {
                values[count++] = Integer.parseInt(part.trim().replaceAll("[^0-9-]", ""));
            } catch (Throwable ignored) {
            }
        }
        return java.util.Arrays.copyOf(values, count);
    }

    private static double[] parseDoubleArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return new double[0];
        }
        String[] parts = raw.split(",");
        double[] values = new double[parts.length];
        int count = 0;
        for (String part : parts) {
            try {
                values[count++] = Double.parseDouble(part.trim().replaceAll("[^0-9eE+\\-.]", ""));
            } catch (Throwable ignored) {
            }
        }
        return java.util.Arrays.copyOf(values, count);
    }

    public record SweepExchangeEntry(String itemId, boolean enabled, int emeraldValue, int experienceValue) {
    }
}
