package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.compat.ComponentCompat;
import com.noah.raidenhancement.compat.MobEffectCompat;
import com.noah.raidenhancement.config.VictorySettlementConfig;
import com.noah.raidenhancement.favor.VillageFavorSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.AABB;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * First alpha settlement layer: one-time victory settlement, JSON rewards,
 * battlefield sweep conversion, equal XP rewards, and player-specific village favor.
 */
public final class VictorySettlementController {
    private static final Map<String, PendingSweep> PENDING_SWEEPS = new LinkedHashMap<>();
    private static final Map<String, Long> FAVOR_MESSAGE_COOLDOWN = new HashMap<>();
    private static boolean settlementHistoryLoaded;
    private static boolean favorLoaded;
    private static final Set<String> SETTLED_RAIDS = new HashSet<>();
    private static final Map<String, FavorRecord> FAVOR_RECORDS = new LinkedHashMap<>();
    private static boolean warnedTickFailure;
    private static boolean warnedRegistryFailure;
    private static boolean warnedSweepFailure;

    private VictorySettlementController() {
    }

    public static void tick(ServerLevel level) {
        if (level == null || !VictorySettlementConfig.ENABLED) {
            return;
        }
        try {
            ensureLoaded();
            long gameTime = level.getGameTime();
            processPendingSweeps(level, gameTime);
            // 0.8.9.9.9: village favor V1 is interaction-driven. Do not scan players/villagers every tick.
            // Long-term gratitude is handled by VillageFavorEvents on player-villager interaction.
        } catch (Throwable throwable) {
            if (!warnedTickFailure) {
                warnedTickFailure = true;
                System.out.println("[Raid Enhancement Patch] Victory settlement tick failed once and was suppressed: " + throwable);
            }
        }
    }

    public static void recordPlayerSupportUse(ServerLevel level, Player player) {
        if (level == null || player == null || !VictorySettlementConfig.ENABLED) {
            return;
        }
        String dimension = dimensionId(level);
        for (RaidSession session : RaidSessionManager.activeSessions()) {
            if (session == null || session.isClosed() || !dimension.equals(session.dimensionId())) {
                continue;
            }
            if (isPlayerNearTrackedRaider(level, player, session, VictorySettlementConfig.SETTLEMENT_RADIUS + 64)
                    || RaidSessionManager.activeSessionCount() == 1) {
                session.markSettlementParticipant(player.getUUID(), level.getGameTime());
            }
        }
    }

    public static void onVillageRaidCompleted(ServerLevel level,
                                              String raidKey,
                                              String dimensionId,
                                              int centerX,
                                              int centerY,
                                              int centerZ,
                                              String difficultyName,
                                              int omenLevel,
                                              int totalWaves,
                                              boolean victory,
                                              int preparedWaves,
                                              int defenseFailures,
                                              int timeoutPenalties,
                                              int damagedVillagerEvents,
                                              int timeoutDamagedVillagerEvents,
                                              long gameTime) {
        if (!VictorySettlementConfig.ENABLED || level == null || raidKey == null || raidKey.isBlank() || !victory) {
            return;
        }
        try {
            ensureLoaded();
            String settlementKey = settlementKey(dimensionId, raidKey);
            RaidKeyDiagnostics.logSettlement("before-history-check", level, raidKey, dimensionId, centerX, centerY, centerZ,
                    settlementKey, gameTime, omenLevel, Math.max(preparedWaves, totalWaves), null);
            if (SETTLED_RAIDS.contains(settlementKey)) {
                RaidKeyDiagnostics.logSettlement("duplicate-blocked-by-history", level, raidKey, dimensionId, centerX, centerY, centerZ,
                        settlementKey, gameTime, omenLevel, Math.max(preparedWaves, totalWaves), null);
                return;
            }
            SETTLED_RAIDS.add(settlementKey);
            saveSettlementHistory();

            RaidSession session = RaidSessionManager.get(raidKey).orElse(null);
            VictoryTier tier = VictoryTier.grade(defenseFailures, timeoutPenalties, damagedVillagerEvents, timeoutDamagedVillagerEvents);
            Collection<UUID> storedParticipants = session == null ? List.of() : session.settlementParticipants();
            List<Player> eligiblePlayers = eligiblePlayers(level, centerX, centerY, centerZ, storedParticipants);
            int effectiveOmen = Math.max(1, Math.min(5, omenLevel <= 0 && session != null ? session.omenLevel() : omenLevel));
            int effectiveTotalWaves = Math.max(preparedWaves, Math.max(totalWaves, session == null ? 0 : session.totalWaves()));
            RaidKeyDiagnostics.logSettlement("accepted-before-rewards", level, raidKey, dimensionId, centerX, centerY, centerZ,
                    settlementKey, gameTime, effectiveOmen, effectiveTotalWaves, eligiblePlayers);
            boolean extraWaveCompleted = effectiveTotalWaves > 8 || preparedWaves > 8;

            int baseXp = JsonRewardTable.baseExperience(difficultyName, effectiveOmen);
            int favorDelta = favorDelta(tier, difficultyName, effectiveOmen, extraWaveCompleted);
            int rewardedPlayers = 0;
            int totalItemStacks = 0;
            for (Player player : eligiblePlayers) {
                if (player == null || !player.isAlive()) {
                    continue;
                }
                int oldFavorLevel = VillageFavorSystem.favorLevelFor(level, dimensionId, centerX, centerY, centerZ, player.getUUID());
                double favorXpBonus = VictorySettlementConfig.FAVOR_XP_BONUS_ENABLED ? JsonRewardTable.favorXpBonus(oldFavorLevel) : 0.0D;
                int xp = (int) Math.round(baseXp * JsonRewardTable.gradeMultiplier(tier) * (1.0D + favorXpBonus));
                if (xp > 0) {
                    player.giveExperiencePoints(xp);
                }
                // 0.8.9.8.4: custom 9-11 wave chains can keep the native Raid from
                // reliably applying Hero of the Village. Grant/refresh it during the
                // authoritative settlement so 8-wave native raids and extended raids
                // both end with the expected vanilla-style victory effect.
                MobEffectCompat.addVisibleEffect(player, MobEffectCompat.HERO_OF_THE_VILLAGE_NAMES,
                        48000, Math.max(0, effectiveOmen - 1));
                List<ItemStack> rewards = JsonRewardTable.roll(tier);
                for (ItemStack stack : rewards) {
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    totalItemStacks++;
                    giveOrDrop(level, player, stack.copy());
                }
                // 0.8.9.9.9: village favor is now written through the structural V1 state module after all rewards are resolved.
                // The old points map is kept only for legacy migration helpers and is no longer authoritative here.
                rewardedPlayers++;
            }
            VillageFavorSystem.recordRaidVictory(level, dimensionId, centerX, centerY, centerZ, eligiblePlayers, gameTime,
                    effectiveOmen, difficultyName, extraWaveCompleted);

            if (VictorySettlementConfig.SHOW_CHAT_MESSAGES) {
                sendSettlementMessage(level, centerX, centerY, centerZ,
                        "[村庄防卫结算] " + tier.displayName() + "：目标波次 " + effectiveTotalWaves
                                + "，防线崩溃 " + Math.max(0, defenseFailures)
                                + " 次，清剿超时 " + Math.max(0, timeoutPenalties)
                                + " 次，奖励玩家 " + rewardedPlayers + " 名。");
                sendSettlementMessage(level, centerX, centerY, centerZ,
                        "[村庄防卫结算] 奖励已按评级 JSON 发放，基础经验=" + baseXp
                                + "，奖品堆数=" + totalItemStacks + "。战场扫荡将在短暂延迟后执行。");
            }

            if (VictorySettlementConfig.BATTLEFIELD_SWEEP_ENABLED) {
                PENDING_SWEEPS.put(settlementKey, new PendingSweep(settlementKey, dimensionId, centerX, centerY, centerZ,
                        gameTime + Math.max(0, VictorySettlementConfig.BATTLEFIELD_SWEEP_DELAY_TICKS),
                        new ArrayList<>(eligiblePlayers.stream().map(Player::getUUID).toList())));
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Victory settlement failed for raid " + raidKey + " and was suppressed: " + throwable);
        }
    }

    public static ItemStack makeItemStack(String itemId, int count) {
        if (itemId == null || itemId.isBlank() || count <= 0) {
            return ItemStack.EMPTY;
        }
        Item item = resolveItem(itemId.trim());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack((ItemLike) item, Math.max(1, count));
        } catch (Throwable throwable) {
            return ItemStack.EMPTY;
        }
    }

    private static void processPendingSweeps(ServerLevel level, long gameTime) {
        if (PENDING_SWEEPS.isEmpty()) {
            return;
        }
        String dimension = dimensionId(level);
        Iterator<Map.Entry<String, PendingSweep>> iterator = PENDING_SWEEPS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingSweep sweep = iterator.next().getValue();
            if (sweep == null || !dimension.equals(sweep.dimensionId)) {
                continue;
            }
            if (gameTime < sweep.executeGameTime) {
                continue;
            }
            iterator.remove();
            executeSweep(level, sweep);
        }
    }

    private static void executeSweep(ServerLevel level, PendingSweep sweep) {
        try {
            List<JsonRewardTable.SweepExchangeEntry> entries = JsonRewardTable.loadSweepEntries();
            if (entries.isEmpty()) {
                return;
            }
            Map<String, JsonRewardTable.SweepExchangeEntry> byItem = new HashMap<>();
            for (JsonRewardTable.SweepExchangeEntry entry : entries) {
                if (entry != null && entry.enabled()) {
                    byItem.put(entry.itemId(), entry);
                }
            }
            if (byItem.isEmpty()) {
                return;
            }
            int radius = Math.max(1, VictorySettlementConfig.BATTLEFIELD_SWEEP_RADIUS);
            int vertical = Math.max(1, VictorySettlementConfig.BATTLEFIELD_SWEEP_VERTICAL_RADIUS);
            Object aabb = new AABB(sweep.centerX - radius, sweep.centerY - vertical, sweep.centerZ - radius,
                    sweep.centerX + radius, sweep.centerY + vertical, sweep.centerZ + radius);
            List<?> items = getItemEntities(level, aabb);
            int scanned = 0;
            int removed = 0;
            int emeraldValue = 0;
            int xpValue = 0;
            for (Object object : items) {
                if (scanned >= Math.max(1, VictorySettlementConfig.BATTLEFIELD_SWEEP_MAX_ITEMS)) {
                    break;
                }
                if (removed >= Math.max(1, VictorySettlementConfig.BATTLEFIELD_SWEEP_MAX_ITEMS_PER_TICK)) {
                    break;
                }
                scanned++;
                if (!(object instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                    continue;
                }
                ItemStack stack = itemEntity.getItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String id = itemId(stack.getItem());
                JsonRewardTable.SweepExchangeEntry entry = byItem.get(id);
                if (entry == null) {
                    continue;
                }
                int count = Math.max(1, stack.getCount());
                emeraldValue += Math.max(0, entry.emeraldValue()) * count;
                xpValue += Math.max(0, entry.experienceValue()) * count;
                itemEntity.discard();
                removed++;
            }
            List<Player> players = playersByUuid(level, sweep.rewardPlayerUuids);
            if (!players.isEmpty()) {
                if (VictorySettlementConfig.BATTLEFIELD_SWEEP_CONVERT_TO_EMERALDS && emeraldValue > 0) {
                    distributeEmeralds(level, players, emeraldValue);
                }
                if (VictorySettlementConfig.BATTLEFIELD_SWEEP_CONVERT_TO_XP && xpValue > 0) {
                    int xpEach = Math.max(0, xpValue / Math.max(1, players.size()));
                    int remainder = Math.max(0, xpValue % Math.max(1, players.size()));
                    for (int i = 0; i < players.size(); i++) {
                        Player player = players.get(i);
                        if (player != null && player.isAlive()) {
                            player.giveExperiencePoints(xpEach + (i < remainder ? 1 : 0));
                        }
                    }
                }
            }
            sendSettlementMessage(level, sweep.centerX, sweep.centerY, sweep.centerZ,
                    "[战场扫荡] 已回收 " + removed + " 组配置战利品，折算绿宝石 " + emeraldValue
                            + "，折算经验 " + xpValue + "。未在白名单内的掉落物已保留。 ");
        } catch (Throwable throwable) {
            if (!warnedSweepFailure) {
                warnedSweepFailure = true;
                System.out.println("[Raid Enhancement Patch] Battlefield sweep failed once and was suppressed: " + throwable);
            }
        }
    }

    private static void distributeEmeralds(ServerLevel level, List<Player> players, int totalEmeralds) {
        if (players == null || players.isEmpty() || totalEmeralds <= 0) {
            return;
        }
        int each = totalEmeralds / players.size();
        int remainder = totalEmeralds % players.size();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player == null || !player.isAlive()) {
                continue;
            }
            int count = each + (i < remainder ? 1 : 0);
            if (count <= 0) {
                continue;
            }
            ItemStack stack = makeItemStack("minecraft:emerald", count);
            giveOrDrop(level, player, stack);
        }
    }

    private static void giveOrDrop(ServerLevel level, Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        boolean added = false;
        if (VictorySettlementConfig.GIVE_REWARDS_TO_INVENTORY) {
            try {
                added = player.addItem(stack);
            } catch (Throwable ignored) {
                added = false;
            }
        }
        if (!added && VictorySettlementConfig.DROP_REWARDS_IF_INVENTORY_FULL) {
            try {
                Method dropMethod = player.getClass().getMethod("drop", ItemStack.class, boolean.class);
                dropMethod.invoke(player, stack, false);
            } catch (Throwable ignored) {
                if (level != null) {
                    ItemEntity entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
                    level.addFreshEntity(entity);
                }
            }
        }
    }

    private static List<Player> eligiblePlayers(ServerLevel level, int centerX, int centerY, int centerZ, Collection<UUID> storedParticipants) {
        int radius = Math.max(16, VictorySettlementConfig.SETTLEMENT_RADIUS);
        long radiusSq = (long) radius * (long) radius;
        Map<UUID, Player> players = new LinkedHashMap<>();
        for (Object object : playersSnapshot(level)) {
            if (!(object instanceof Player player) || !player.isAlive()) {
                continue;
            }
            double dx = player.getX() - (centerX + 0.5D);
            double dy = player.getY() - centerY;
            double dz = player.getZ() - (centerZ + 0.5D);
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                players.put(player.getUUID(), player);
            }
        }
        if (storedParticipants != null) {
            for (UUID uuid : storedParticipants) {
                Player player = findPlayer(level, uuid);
                if (player != null && player.isAlive()) {
                    players.put(uuid, player);
                }
            }
        }
        return new ArrayList<>(players.values());
    }

    private static List<Player> playersByUuid(ServerLevel level, Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        List<Player> players = new ArrayList<>();
        for (UUID uuid : uuids) {
            Player player = findPlayer(level, uuid);
            if (player != null && player.isAlive()) {
                players.add(player);
            }
        }
        return players;
    }

    private static Player findPlayer(ServerLevel level, UUID uuid) {
        if (level == null || uuid == null) {
            return null;
        }
        for (Object object : playersSnapshot(level)) {
            if (object instanceof Player player && uuid.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    private static boolean isPlayerNearTrackedRaider(ServerLevel level, Player player, RaidSession session, int radius) {
        if (level == null || player == null || session == null) {
            return false;
        }
        double radiusSq = (double) radius * (double) radius;
        for (RaiderRecord record : session.trackedRaiders()) {
            if (record == null || record.entityUuid() == null) {
                continue;
            }
            Entity entity = level.getEntity(record.entityUuid());
            if (entity == null) {
                continue;
            }
            double dx = player.getX() - entity.getX();
            double dy = player.getY() - entity.getY();
            double dz = player.getZ() - entity.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private static int favorDelta(VictoryTier tier, String difficultyName, int omenLevel, boolean extraWaveCompleted) {
        int points = switch (tier == null ? VictoryTier.VICTORY : tier) {
            case PERFECT -> VictorySettlementConfig.FAVOR_PERFECT_POINTS;
            case VICTORY -> VictorySettlementConfig.FAVOR_VICTORY_POINTS;
            case COSTLY -> VictorySettlementConfig.FAVOR_COSTLY_POINTS;
        };
        if ("HARD".equalsIgnoreCase(difficultyName)) {
            points += VictorySettlementConfig.FAVOR_HARD_BONUS;
        }
        if (omenLevel >= 5) {
            points += VictorySettlementConfig.FAVOR_OMEN5_BONUS;
        }
        if (extraWaveCompleted) {
            points += VictorySettlementConfig.FAVOR_EXTRA_WAVE_BONUS;
        }
        return Math.max(0, points);
    }

    private static void addFavor(String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid, int delta, long gameTime) {
        if (playerUuid == null || delta <= 0) {
            return;
        }
        String key = favorKey(dimensionId, centerX, centerY, centerZ, playerUuid);
        FavorRecord record = FAVOR_RECORDS.get(key);
        int points = Math.max(0, delta + (record == null ? 0 : record.points));
        FAVOR_RECORDS.put(key, new FavorRecord(key, dimensionId, centerX, centerY, centerZ, playerUuid, points, gameTime));
    }

    private static int favorPoints(String key) {
        FavorRecord record = FAVOR_RECORDS.get(key);
        return record == null ? 0 : Math.max(0, record.points);
    }

    private static int favorLevel(int points) {
        int value = Math.max(0, points);
        if (value >= 250) {
            return 5;
        }
        if (value >= 160) {
            return 4;
        }
        if (value >= 90) {
            return 3;
        }
        if (value >= 40) {
            return 2;
        }
        if (value >= 10) {
            return 1;
        }
        return 0;
    }

    private static String favorLevelName(int level) {
        return switch (Math.max(0, Math.min(5, level))) {
            case 1 -> "记名恩人";
            case 2 -> "村庄友人";
            case 3 -> "防卫功臣";
            case 4 -> "守城英雄";
            case 5 -> "主城守护者";
            default -> "无";
        };
    }

    private static void processFavorEntryMessages(ServerLevel level, long gameTime) {
        if (!VictorySettlementConfig.FAVOR_ENABLED || !VictorySettlementConfig.FAVOR_ENTRY_MESSAGES_ENABLED || FAVOR_RECORDS.isEmpty()) {
            return;
        }
        if (gameTime % Math.max(20, VictorySettlementConfig.FAVOR_ENTRY_SCAN_INTERVAL_TICKS) != 0L) {
            return;
        }
        String dimension = dimensionId(level);
        int radius = Math.max(16, VictorySettlementConfig.FAVOR_ENTRY_RADIUS);
        long radiusSq = (long) radius * (long) radius;
        for (Object object : playersSnapshot(level)) {
            if (!(object instanceof Player player) || !player.isAlive()) {
                continue;
            }
            UUID uuid = player.getUUID();
            for (FavorRecord record : FAVOR_RECORDS.values()) {
                if (record == null || !uuid.equals(record.playerUuid) || !dimension.equals(record.dimensionId)) {
                    continue;
                }
                double dx = player.getX() - (record.centerX + 0.5D);
                double dy = player.getY() - record.centerY;
                double dz = player.getZ() - (record.centerZ + 0.5D);
                if (dx * dx + dy * dy + dz * dz > radiusSq) {
                    continue;
                }
                String cooldownKey = record.key + "@" + uuid;
                long last = FAVOR_MESSAGE_COOLDOWN.getOrDefault(cooldownKey, 0L);
                if (gameTime - last < Math.max(200, VictorySettlementConfig.FAVOR_ENTRY_MESSAGE_COOLDOWN_TICKS)) {
                    continue;
                }
                FAVOR_MESSAGE_COOLDOWN.put(cooldownKey, gameTime);
                int levelValue = favorLevel(record.points);
                if (levelValue > 0) {
                    sendPlayerMessage(player, "[村庄恩情] 这里的村民记得你。当前恩情等级："
                            + favorLevelName(levelValue) + "（" + record.points + "）。");
                }
                break;
            }
        }
    }

    private static void sendSettlementMessage(ServerLevel level, int centerX, int centerY, int centerZ, String message) {
        if (!VictorySettlementConfig.SHOW_CHAT_MESSAGES || level == null || message == null || message.isBlank()) {
            return;
        }
        int radius = Math.max(64, VictorySettlementConfig.SETTLEMENT_RADIUS);
        long radiusSq = (long) radius * (long) radius;
        for (Object object : playersSnapshot(level)) {
            if (!(object instanceof Player player) || !player.isAlive()) {
                continue;
            }
            double dx = player.getX() - (centerX + 0.5D);
            double dy = player.getY() - centerY;
            double dz = player.getZ() - (centerZ + 0.5D);
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                sendPlayerMessage(player, message);
            }
        }
    }

    private static void sendPlayerMessage(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        try {
            Component component = ComponentCompat.literal(message);
            if (component != null) {
                player.sendSystemMessage(component);
            }
        } catch (Throwable ignored) {
            // Chat feedback must never affect settlement logic.
        }
    }

    private static void ensureLoaded() {
        if (!settlementHistoryLoaded) {
            settlementHistoryLoaded = true;
            loadSettlementHistory();
        }
        if (!favorLoaded) {
            favorLoaded = true;
            loadFavor();
        }
    }

    private static String settlementKey(String dimensionId, String raidKey) {
        return sanitize(dimensionId == null ? "unknown" : dimensionId) + "@" + sanitize(raidKey == null ? "unknown" : raidKey);
    }

    private static String favorKey(String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid) {
        return sanitize(dimensionId == null ? "unknown" : dimensionId) + "@" + centerX + "_" + centerY + "_" + centerZ + "@" + playerUuid;
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.replace(':', '_').replace('/', '_').replace(' ', '_').replace('#', '_');
    }

    private static void loadSettlementHistory() {
        Path file = VictorySettlementConfig.settlementHistoryFile();
        if (!Files.exists(file)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            for (String key : properties.stringPropertyNames()) {
                if (key != null && !key.isBlank()) {
                    SETTLED_RAIDS.add(key);
                }
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Failed to load victory settlement history; duplicate guard may be memory-only this run: " + throwable);
        }
    }

    private static void saveSettlementHistory() {
        try {
            Path file = VictorySettlementConfig.settlementHistoryFile();
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            for (String key : SETTLED_RAIDS) {
                properties.setProperty(key, "settled");
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                properties.store(writer, "Raid Enhancement Patch victory settlement one-time history");
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Failed to save victory settlement history: " + throwable);
        }
    }

    private static void loadFavor() {
        Path file = VictorySettlementConfig.favorFile();
        if (!Files.exists(file)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            Map<String, BuilderFavor> builders = new HashMap<>();
            for (String name : properties.stringPropertyNames()) {
                int dot = name.indexOf('.');
                if (dot <= 0) {
                    continue;
                }
                String key = name.substring(0, dot);
                String field = name.substring(dot + 1);
                BuilderFavor builder = builders.computeIfAbsent(key, BuilderFavor::new);
                String value = properties.getProperty(name, "");
                builder.accept(field, value);
            }
            for (BuilderFavor builder : builders.values()) {
                FavorRecord record = builder.build();
                if (record != null) {
                    FAVOR_RECORDS.put(record.key, record);
                }
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Failed to load village favor data; favor will continue in memory this run: " + throwable);
        }
    }

    private static void saveFavor() {
        try {
            Path file = VictorySettlementConfig.favorFile();
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            int i = 0;
            for (FavorRecord record : FAVOR_RECORDS.values()) {
                if (record == null) {
                    continue;
                }
                String prefix = "favor" + i++;
                properties.setProperty(prefix + ".key", record.key);
                properties.setProperty(prefix + ".dimension", record.dimensionId);
                properties.setProperty(prefix + ".centerX", Integer.toString(record.centerX));
                properties.setProperty(prefix + ".centerY", Integer.toString(record.centerY));
                properties.setProperty(prefix + ".centerZ", Integer.toString(record.centerZ));
                properties.setProperty(prefix + ".player", record.playerUuid.toString());
                properties.setProperty(prefix + ".points", Integer.toString(record.points));
                properties.setProperty(prefix + ".lastUpdate", Long.toString(record.lastUpdateGameTime));
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                properties.store(writer, "Raid Enhancement Patch village favor data");
            }
        } catch (Throwable throwable) {
            System.out.println("[Raid Enhancement Patch] Failed to save village favor data: " + throwable);
        }
    }

    private static List<?> playersSnapshot(ServerLevel level) {
        try {
            Method playersMethod = level.getClass().getMethod("players");
            Object result = playersMethod.invoke(level);
            if (result instanceof List<?> list) {
                return List.copyOf(list);
            }
        } catch (ReflectiveOperationException ignored) {
            // Fail closed.
        }
        return List.of();
    }

    private static String dimensionId(ServerLevel level) {
        try {
            Object dimension = level.getClass().getMethod("dimension").invoke(level);
            Object location = dimension == null ? null : dimension.getClass().getMethod("location").invoke(dimension);
            return location == null ? "unknown" : location.toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static List<?> getItemEntities(ServerLevel level, Object aabb) {
        try {
            Method method = findMethod(level.getClass(), "getEntitiesOfClass", new Object[]{ItemEntity.class, aabb});
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(level, ItemEntity.class, aabb);
                if (result instanceof List<?> list) {
                    return list;
                }
            }
        } catch (Throwable ignored) {
            // Try generic fallback.
        }
        try {
            java.util.function.Predicate<Object> predicate = object -> object instanceof ItemEntity;
            Method method = findMethod(level.getClass(), "getEntities", new Object[]{null, aabb, predicate});
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(level, null, aabb, predicate);
                if (result instanceof List<?> list) {
                    return list;
                }
            }
        } catch (Throwable ignored) {
            // Empty result.
        }
        return List.of();
    }

    private static Method findMethod(Class<?> type, String name, Object[] args) {
        for (Method method : type.getMethods()) {
            if (methodMatches(method, name, args)) {
                return method;
            }
        }
        Class<?> cursor = type;
        while (cursor != null) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (methodMatches(method, name, args)) {
                    return method;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private static boolean methodMatches(Method method, String name, Object[] args) {
        if (method == null || !method.getName().equals(name) || method.getParameterCount() != args.length) {
            return false;
        }
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            Class<?> type = types[i];
            if (type.isPrimitive()) {
                continue;
            }
            if (arg instanceof Class<?> classArg && Class.class.isAssignableFrom(type)) {
                continue;
            }
            if (!type.isAssignableFrom(arg.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static String itemId(Item item) {
        if (item == null) {
            return "";
        }
        try {
            Object registry = Class.forName("net.minecraft.core.registries.BuiltInRegistries").getField("ITEM").get(null);
            Method method = findOneArgMethod(registry.getClass(), "getKey", item);
            if (method != null) {
                Object result = method.invoke(registry, item);
                return result == null ? "" : result.toString();
            }
        } catch (Throwable ignored) {
            // Fall back below.
        }
        return "";
    }

    private static Item resolveItem(String itemId) {
        try {
            Object resourceLocation = resourceLocation(itemId);
            if (resourceLocation == null) {
                return null;
            }
            Object registry = Class.forName("net.minecraft.core.registries.BuiltInRegistries").getField("ITEM").get(null);
            Object result = null;
            Method optional = findOneArgMethodByName(registry.getClass(), "getOptional", resourceLocation);
            if (optional != null) {
                Object optionalResult = optional.invoke(registry, resourceLocation);
                if (optionalResult instanceof Optional<?> opt && opt.isPresent()) {
                    result = unwrapHolder(opt.get());
                }
            }
            if (result == null) {
                for (String methodName : List.of("getValue", "get")) {
                    Method get = findOneArgMethodByName(registry.getClass(), methodName, resourceLocation);
                    if (get != null) {
                        result = unwrapHolder(get.invoke(registry, resourceLocation));
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
            if (result instanceof Optional<?> opt && opt.isPresent()) {
                result = unwrapHolder(opt.get());
            }
            if (result instanceof Item item && itemId.equals(itemId(item))) {
                return item;
            }
        } catch (Throwable throwable) {
            if (!warnedRegistryFailure) {
                warnedRegistryFailure = true;
                System.out.println("[Raid Enhancement Patch] Item registry lookup failed once; JSON rewards may skip missing items: " + throwable);
            }
        }
        return null;
    }


    private static Object unwrapHolder(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Item) {
            return value;
        }
        try {
            Method method = value.getClass().getMethod("value");
            Object unwrapped = method.invoke(value);
            return unwrapped == null ? value : unwrapped;
        } catch (Throwable ignored) {
            return value;
        }
    }

    private static Object resourceLocation(String id) throws ReflectiveOperationException {
        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        for (Method method : resourceLocationClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != String.class) {
                continue;
            }
            if (!"parse".equals(method.getName()) && !"tryParse".equals(method.getName())) {
                continue;
            }
            Object result = method.invoke(null, id);
            if (result != null) {
                return result;
            }
        }
        int colon = id.indexOf(':');
        if (colon > 0) {
            for (Method method : resourceLocationClass.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && method.getName().equals("fromNamespaceAndPath")
                        && method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == String.class
                        && method.getParameterTypes()[1] == String.class) {
                    return method.invoke(null, id.substring(0, colon), id.substring(colon + 1));
                }
            }
        }
        Constructor<?> constructor = resourceLocationClass.getConstructor(String.class);
        return constructor.newInstance(id);
    }

    private static Method findOneArgMethod(Object typeObject, String name, Object arg) {
        Class<?> type = typeObject instanceof Class<?> classObject ? classObject : typeObject.getClass();
        return findOneArgMethodByName(type, name, arg);
    }

    private static Method findOneArgMethodByName(Class<?> type, String name, Object arg) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            if (arg == null || parameter.isAssignableFrom(arg.getClass()) || parameter == Object.class) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private record PendingSweep(String key, String dimensionId, int centerX, int centerY, int centerZ, long executeGameTime,
                                List<UUID> rewardPlayerUuids) {
    }

    private record FavorRecord(String key, String dimensionId, int centerX, int centerY, int centerZ, UUID playerUuid,
                               int points, long lastUpdateGameTime) {
    }

    private static final class BuilderFavor {
        private final String propertyPrefix;
        private String key;
        private String dimensionId = "unknown";
        private int centerX;
        private int centerY;
        private int centerZ;
        private UUID playerUuid;
        private int points;
        private long lastUpdate;

        BuilderFavor(String propertyPrefix) {
            this.propertyPrefix = propertyPrefix;
        }

        void accept(String field, String value) {
            try {
                switch (field) {
                    case "key" -> key = value;
                    case "dimension" -> dimensionId = value == null || value.isBlank() ? "unknown" : value;
                    case "centerX" -> centerX = Integer.parseInt(value.trim());
                    case "centerY" -> centerY = Integer.parseInt(value.trim());
                    case "centerZ" -> centerZ = Integer.parseInt(value.trim());
                    case "player" -> playerUuid = UUID.fromString(value.trim());
                    case "points" -> points = Integer.parseInt(value.trim());
                    case "lastUpdate" -> lastUpdate = Long.parseLong(value.trim());
                    default -> {
                    }
                }
            } catch (Throwable ignored) {
                // Ignore malformed fields.
            }
        }

        FavorRecord build() {
            if (playerUuid == null) {
                return null;
            }
            String finalKey = key == null || key.isBlank() ? propertyPrefix : key;
            return new FavorRecord(finalKey, dimensionId, centerX, centerY, centerZ, playerUuid, Math.max(0, points), Math.max(0L, lastUpdate));
        }
    }
}
