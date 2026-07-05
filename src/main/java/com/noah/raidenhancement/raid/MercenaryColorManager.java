package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.BattleSupportConfig;
import net.minecraft.server.level.ServerLevel;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent owner -> vanilla scoreboard color allocation for mercenary golem glow.
 *
 * This manager intentionally never adds players to scoreboard teams. Only mercenary
 * iron-golem entity UUID strings are added by MercenaryGolemController. Team names
 * use a dedicated prefix so later quest/task-book team systems can use their own
 * namespace without being polluted by mercenary glow teams.
 */
public final class MercenaryColorManager {
    private static final List<String> VANILLA_COLORS = List.of(
            "BLACK", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA",
            "DARK_RED", "DARK_PURPLE", "GOLD", "GRAY",
            "DARK_GRAY", "BLUE", "GREEN", "AQUA",
            "RED", "LIGHT_PURPLE", "YELLOW", "WHITE"
    );

    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("BLACK", "black"),
            Map.entry("DARK_BLUE", "dblue"),
            Map.entry("DARK_GREEN", "dgreen"),
            Map.entry("DARK_AQUA", "daqua"),
            Map.entry("DARK_RED", "dred"),
            Map.entry("DARK_PURPLE", "dpurp"),
            Map.entry("GOLD", "gold"),
            Map.entry("GRAY", "gray"),
            Map.entry("DARK_GRAY", "dgray"),
            Map.entry("BLUE", "blue"),
            Map.entry("GREEN", "green"),
            Map.entry("AQUA", "aqua"),
            Map.entry("RED", "red"),
            Map.entry("LIGHT_PURPLE", "lpurp"),
            Map.entry("YELLOW", "yellow"),
            Map.entry("WHITE", "white")
    );

    private static final Map<UUID, String> PLAYER_COLORS = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean warnedLoadFailure;
    private static boolean warnedSaveFailure;

    private MercenaryColorManager() {
    }

    public static synchronized String colorForOwner(ServerLevel level, UUID ownerId) {
        if (!BattleSupportConfig.MERCENARY_GLOWING_PER_PLAYER_COLOR || ownerId == null) {
            return normalizeColor(BattleSupportConfig.MERCENARY_GLOWING_TEAM_COLOR, "GOLD");
        }
        loadIfNeeded();
        String existing = normalizeColor(PLAYER_COLORS.get(ownerId), null);
        if (existing != null) {
            return existing;
        }
        List<String> palette = configuredPalette();
        if (palette.isEmpty()) {
            palette = List.of("GOLD");
        }
        String assigned = null;
        if (BattleSupportConfig.MERCENARY_GLOWING_EXCLUSIVE_PLAYER_COLORS) {
            Set<String> used = new LinkedHashSet<>(PLAYER_COLORS.values());
            for (String color : palette) {
                if (!used.contains(color)) {
                    assigned = color;
                    break;
                }
            }
            if (assigned == null && isReusePolicy()) {
                assigned = palette.get(Math.floorMod(ownerId.hashCode(), palette.size()));
            }
        } else {
            assigned = palette.get(Math.floorMod(ownerId.hashCode(), palette.size()));
        }
        if (assigned == null) {
            return null;
        }
        PLAYER_COLORS.put(ownerId, assigned);
        save();
        return assigned;
    }

    public static synchronized String colorForRestoredEntity(ServerLevel level, UUID ownerId, String savedColor) {
        String normalizedSaved = normalizeColor(savedColor, null);
        if (BattleSupportConfig.MERCENARY_GLOWING_PER_PLAYER_COLOR && ownerId != null) {
            String ownerColor = colorForOwner(level, ownerId);
            if (ownerColor != null) {
                return ownerColor;
            }
        }
        if (normalizedSaved != null) {
            return normalizedSaved;
        }
        return normalizeColor(BattleSupportConfig.MERCENARY_GLOWING_TEAM_COLOR, "GOLD");
    }

    public static synchronized void releaseOwnerColorIfConfigured(UUID ownerId, boolean ownerStillHasActiveMercenaries) {
        if (ownerId == null || ownerStillHasActiveMercenaries) {
            return;
        }
        if (!BattleSupportConfig.MERCENARY_GLOWING_RELEASE_COLOR_WHEN_NO_ACTIVE_MERCENARIES) {
            return;
        }
        loadIfNeeded();
        if (PLAYER_COLORS.remove(ownerId) != null) {
            save();
        }
    }

    public static String teamNameForColor(String color) {
        String normalizedColor = normalizeColor(color, "GOLD");
        String prefix = sanitizeTeamPrefix(BattleSupportConfig.MERCENARY_GLOWING_TEAM_PREFIX);
        String suffix = ABBREVIATIONS.getOrDefault(normalizedColor, normalizedColor.toLowerCase(Locale.ROOT).replace("_", ""));
        String name = prefix + suffix;
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    public static synchronized Set<String> allMercenaryTeamNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add(safeTeamName(BattleSupportConfig.MERCENARY_GLOWING_TEAM_NAME));
        for (String color : VANILLA_COLORS) {
            names.add(teamNameForColor(color));
        }
        for (String color : configuredPalette()) {
            names.add(teamNameForColor(color));
        }
        loadIfNeeded();
        for (String color : PLAYER_COLORS.values()) {
            names.add(teamNameForColor(color));
        }
        return names;
    }

    public static boolean isValidColor(String color) {
        return normalizeColor(color, null) != null;
    }

    public static String normalizeColor(String color, String fallback) {
        String normalized = color == null ? null : color.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (normalized != null && VANILLA_COLORS.contains(normalized)) {
            return normalized;
        }
        if (fallback == null) {
            return null;
        }
        String normalizedFallback = fallback.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return VANILLA_COLORS.contains(normalizedFallback) ? normalizedFallback : "GOLD";
    }

    private static List<String> configuredPalette() {
        String raw = BattleSupportConfig.MERCENARY_GLOWING_COLOR_PALETTE;
        if (raw == null || raw.isBlank()) {
            return List.of("GOLD", "YELLOW", "GREEN", "AQUA", "BLUE", "LIGHT_PURPLE", "RED", "DARK_PURPLE", "DARK_RED", "DARK_AQUA", "WHITE", "GRAY");
        }
        List<String> colors = new ArrayList<>();
        for (String part : raw.split(",")) {
            String normalized = normalizeColor(part, null);
            if (normalized != null && !colors.contains(normalized)) {
                colors.add(normalized);
            }
        }
        return Collections.unmodifiableList(colors);
    }

    private static boolean isReusePolicy() {
        String policy = BattleSupportConfig.MERCENARY_GLOWING_COLOR_EXHAUSTED_POLICY;
        return policy != null && policy.trim().equalsIgnoreCase("REUSE");
    }

    private static String sanitizeTeamPrefix(String configured) {
        String prefix = configured == null || configured.isBlank() ? "rep_m_" : configured.trim().toLowerCase(Locale.ROOT);
        prefix = prefix.replaceAll("[^a-z0-9_]", "_");
        if (prefix.length() > 10) {
            prefix = prefix.substring(0, 10);
        }
        if (prefix.isBlank()) {
            prefix = "rep_m_";
        }
        return prefix;
    }

    private static String safeTeamName(String configured) {
        String name = configured == null || configured.isBlank() ? "rep_merc_gold" : configured.trim();
        name = name.replaceAll("[^A-Za-z0-9_\\-.+]", "_");
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private static void loadIfNeeded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = stateFile();
        if (!Files.exists(file)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            for (String key : properties.stringPropertyNames()) {
                try {
                    UUID owner = UUID.fromString(key.trim());
                    String color = normalizeColor(properties.getProperty(key), null);
                    if (color != null) {
                        PLAYER_COLORS.put(owner, color);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid state lines.
                }
            }
        } catch (Throwable throwable) {
            if (!warnedLoadFailure) {
                warnedLoadFailure = true;
                System.out.println("[Raid Enhancement Patch] Mercenary player color state load failed once and was suppressed: " + throwable);
            }
        }
    }

    private static void save() {
        try {
            Path file = stateFile();
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            for (Map.Entry<UUID, String> entry : PLAYER_COLORS.entrySet()) {
                if (entry.getKey() != null && isValidColor(entry.getValue())) {
                    properties.setProperty(entry.getKey().toString(), entry.getValue());
                }
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                properties.store(writer, "Raid Enhancement Patch mercenary owner color state. Player UUID -> vanilla scoreboard color.");
            }
        } catch (Throwable throwable) {
            if (!warnedSaveFailure) {
                warnedSaveFailure = true;
                System.out.println("[Raid Enhancement Patch] Mercenary player color state save failed once and was suppressed: " + throwable);
            }
        }
    }

    private static Path stateFile() {
        return Path.of("config", "raid_enhancement_patch", "mercenary_player_colors.properties");
    }
}
