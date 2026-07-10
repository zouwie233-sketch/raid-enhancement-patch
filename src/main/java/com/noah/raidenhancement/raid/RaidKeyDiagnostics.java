package com.noah.raidenhancement.raid;

import com.noah.raidenhancement.config.KeyDiagnosticsConfig;
import com.noah.raidenhancement.favor.VillageFavorGatewayAudit;
import com.noah.raidenhancement.favor.VillageFavorRecord;
import com.noah.raidenhancement.favor.VillageFavorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Opt-in key-chain diagnostics with 0.9.1.2 read-only Key Service audit fields.
 *
 * This class is intentionally read-only: it prints candidate/actual keys and
 * BossBar state snapshots, but does not decide raid completion, settlement
 * history, favor growth, BossBar progress, gift cooldowns, reward values, or
 * persistence paths.
 */
public final class RaidKeyDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("RaidEnhancementPatchKeyDiag");
    private static final Map<String, Long> LAST_LOG_BY_EVENT_AND_KEY = new LinkedHashMap<>();
    private static boolean warnedFileOutput;
    private static boolean warnedVictorySettlementBoundaryAudit;

    private RaidKeyDiagnostics() {
    }

    public static void logStartup(String modVersion) {
        String state = KeyDiagnosticsConfig.ENABLED ? "enabled" : "disabled";
        emitAlways("startup", "version=" + safe(modVersion)
                + " configStage=" + KeyDiagnosticsConfig.CONFIG_STAGE
                + " enabled=" + KeyDiagnosticsConfig.ENABLED
                + " state=" + state
                + " raidDiscovery=" + KeyDiagnosticsConfig.LOG_RAID_DISCOVERY
                + " settlement=" + KeyDiagnosticsConfig.LOG_SETTLEMENT
                + " favor=" + KeyDiagnosticsConfig.LOG_FAVOR
                + " bossbar=" + KeyDiagnosticsConfig.LOG_BOSSBAR
                + " storagePaths=" + KeyDiagnosticsConfig.LOG_STORAGE_PATHS
                + " intervalTicks=" + KeyDiagnosticsConfig.LOG_INTERVAL_TICKS
                + " " + KeyDebugService.startupMarker()
                + " " + KeyDebugService.boundarySummary()
                + " " + VictorySettlementBoundaryAudit.startupMarker()
                + " " + VillageFavorGatewayAudit.startupMarker()
                + " note=diagnostic-only-no-gameplay-behavior-change.");
    }

    public static void logRaidDiscovery(String phase,
                                        ServerLevel level,
                                        String stateKey,
                                        String dimensionId,
                                        int centerX,
                                        int centerY,
                                        int centerZ,
                                        int sessionRaidId,
                                        long firstSeenGameTime,
                                        long gameTime,
                                        Object nativeRaid) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_RAID_DISCOVERY) {
            return;
        }
        if (!shouldLog("raid-discovery:" + phase, stateKey, gameTime)) {
            return;
        }
        String villageKey = villageKey(dimensionId, centerX, centerY, centerZ);
        String raidInstanceCandidate = raidInstanceCandidate(dimensionId, stateKey, sessionRaidId, firstSeenGameTime, nativeRaid);
        emit("raid-discovery", "phase=" + safe(phase)
                + " dimensionId=" + safe(dimensionId)
                + " center=" + center(centerX, centerY, centerZ)
                + " state.key=" + safe(stateKey)
                + " villageKey=" + villageKey
                + " raidInstanceKeyCandidate=" + raidInstanceCandidate
                + " raidSessionKey=" + safe(stateKey)
                + " sessionRaidId=" + sessionRaidId
                + " nativeRaidIdentity=" + nativeIdentity(nativeRaid)
                + " firstSeenGameTime=" + firstSeenGameTime
                + " gameTime=" + gameTime
                + KeyDebugService.auditFields("raid-discovery", dimensionId, centerX, centerY, centerZ,
                raidInstanceCandidate, villageKey, raidInstanceCandidate, "unknown")
                + " levelDimension=" + dimensionId(level) + ".");
    }

    public static void logSettlement(String phase,
                                     ServerLevel level,
                                     String raidKey,
                                     String dimensionId,
                                     int centerX,
                                     int centerY,
                                     int centerZ,
                                     String settlementKey,
                                     long gameTime,
                                     int omenLevel,
                                     int totalWaves,
                                     Collection<? extends Player> eligiblePlayers) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_SETTLEMENT) {
            return;
        }
        String villageKey = villageKey(dimensionId, centerX, centerY, centerZ);
        String settlement = safe(settlementKey);
        String raidInstanceCandidate = settlement.contains("@raidInstance:")
                ? settlement
                : raidInstanceCandidate(dimensionId, raidKey, raidKey == null ? 0 : raidKey.hashCode(), gameTime, null);
        String settlementKeyMode = settlement.contains("@raidInstance:") ? "raidInstance" : "legacyVillageCenter";
        String playerKeys = playerFavorKeys(dimensionId, centerX, centerY, centerZ, eligiblePlayers);
        emit("settlement", "phase=" + safe(phase)
                + " dimensionId=" + safe(dimensionId)
                + " center=" + center(centerX, centerY, centerZ)
                + " raidKey=" + safe(raidKey)
                + " villageKey=" + villageKey
                + " raidInstanceKeyCandidate=" + raidInstanceCandidate
                + " settlementKey=" + settlement
                + " settlementKeyMode=" + settlementKeyMode
                + " bossBarKeyCandidate=" + safe(raidKey)
                + " raidSessionKeyCandidate=" + safe(raidKey)
                + " omenLevel=" + omenLevel
                + " totalWaves=" + totalWaves
                + " eligiblePlayerFavorKeys=" + playerKeys
                + KeyDebugService.auditFields("settlement", dimensionId, centerX, centerY, centerZ,
                raidInstanceCandidate, villageKey, settlement, "listed-in-eligiblePlayerFavorKeys")
                + " completedGameTime=" + gameTime
                + " levelDimension=" + dimensionId(level) + ".");
        logVictorySettlementBoundary(phase, dimensionId, centerX, centerY, centerZ,
                raidInstanceCandidate, villageKey, settlementKeyMode, gameTime,
                omenLevel, totalWaves, eligiblePlayers);
    }

    private static void logVictorySettlementBoundary(String phase,
                                                      String dimensionId,
                                                      int centerX,
                                                      int centerY,
                                                      int centerZ,
                                                      String raidInstanceKey,
                                                      String villageKey,
                                                      String settlementKeyMode,
                                                      long gameTime,
                                                      int omenLevel,
                                                      int totalWaves,
                                                      Collection<? extends Player> eligiblePlayers) {
        if (!VictorySettlementBoundaryAudit.isBoundaryPhase(phase)) {
            return;
        }
        try {
            List<UUID> eligiblePlayerUuids = playerUuids(eligiblePlayers);
            boolean eligiblePlayersResolved = eligiblePlayers != null;
            RaidCompletionResult result = VictorySettlementBoundaryAudit.project(
                    raidInstanceKey,
                    villageKey,
                    dimensionId,
                    centerX,
                    centerY,
                    centerZ,
                    eligiblePlayerUuids,
                    omenLevel,
                    totalWaves,
                    gameTime
            );
            emit("victory-settlement-boundary",
                    VictorySettlementBoundaryAudit.auditFields(
                            phase,
                            result,
                            eligiblePlayersResolved,
                            settlementKeyMode
                    ));
        } catch (Throwable throwable) {
            if (!warnedVictorySettlementBoundaryAudit) {
                warnedVictorySettlementBoundaryAudit = true;
                LOGGER.warn("[Raid Enhancement Patch][KeyDiag][victory-settlement-boundary] audit projection failed once and was suppressed: {}",
                        throwable.toString());
            }
        }
    }

    public static void logVillageFavorGateway(String phase,
                                                     ServerLevel level,
                                                     String dimensionId,
                                                     int centerX,
                                                     int centerY,
                                                     int centerZ,
                                                     UUID playerUuid,
                                                     int eligiblePlayerCount,
                                                     long gameTime,
                                                     int omenLevel,
                                                     boolean extraWaveCompleted,
                                                     boolean completionResultInput,
                                                     String gatewayEntry) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_FAVOR) {
            return;
        }
        String favorRecordKey = playerUuid == null
                ? "not-applicable"
                : VillageFavorState.recordKey(dimensionId, centerX, centerY, centerZ, playerUuid);
        emit("village-favor-gateway",
                VillageFavorGatewayAudit.auditFields(phase, gatewayEntry, eligiblePlayerCount, completionResultInput)
                        + " dimensionId=" + safe(dimensionId)
                        + " center=" + center(centerX, centerY, centerZ)
                        + " villageKey=" + villageKey(dimensionId, centerX, centerY, centerZ)
                        + " player=" + (playerUuid == null ? "not-applicable" : playerUuid)
                        + " favorRecordKey=" + safe(favorRecordKey)
                        + " omenLevel=" + omenLevel
                        + " extraWaveCompleted=" + extraWaveCompleted
                        + " gameTime=" + gameTime
                        + " levelDimension=" + dimensionId(level) + ".");
    }

    public static void logFavorRecord(String phase,
                                      ServerLevel level,
                                      String dimensionId,
                                      int centerX,
                                      int centerY,
                                      int centerZ,
                                      UUID playerUuid,
                                      String favorRecordKey,
                                      long gameTime,
                                      VillageFavorRecord record) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_FAVOR) {
            return;
        }
        emit("favor-record", "phase=" + safe(phase)
                + " dimensionId=" + safe(dimensionId)
                + " center=" + center(centerX, centerY, centerZ)
                + " villageKey=" + villageKey(dimensionId, centerX, centerY, centerZ)
                + " player=" + (playerUuid == null ? "null" : playerUuid)
                + " favorRecordKey=" + safe(favorRecordKey)
                + " record.key=" + (record == null ? "null" : safe(record.key))
                + " favorLevel=" + (record == null ? -1 : record.favorLevel)
                + " victoryCount=" + (record == null ? -1 : record.victoryCount)
                + " highestOmenLevelWon=" + (record == null ? -1 : record.highestOmenLevelWon)
                + " raidMeritScore=" + (record == null ? -1 : record.raidMeritScore)
                + KeyDebugService.auditFields("favor-record", dimensionId, centerX, centerY, centerZ,
                "unknown", villageKey(dimensionId, centerX, centerY, centerZ), "unknown", safe(favorRecordKey))
                + " gameTime=" + gameTime
                + " levelDimension=" + dimensionId(level) + ".");
    }

    public static void logFavorInteraction(String phase,
                                           ServerLevel level,
                                           String dimensionId,
                                           UUID playerUuid,
                                           double x,
                                           double y,
                                           double z,
                                           VillageFavorRecord record,
                                           long gameTime) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_FAVOR) {
            return;
        }
        emit("favor-interaction", "phase=" + safe(phase)
                + " dimensionId=" + safe(dimensionId)
                + " interactionPos=" + decimal(x) + "," + decimal(y) + "," + decimal(z)
                + " player=" + (playerUuid == null ? "null" : playerUuid)
                + " matchedVillageKey=" + (record == null ? "null" : villageKey(record.dimensionId, record.centerX, record.centerY, record.centerZ))
                + " matchedFavorRecordKey=" + (record == null ? "null" : safe(record.key))
                + " recordCenter=" + (record == null ? "null" : center(record.centerX, record.centerY, record.centerZ))
                + " radius=" + (record == null ? -1 : record.radius)
                + " gameTime=" + gameTime
                + " levelDimension=" + dimensionId(level) + ".");
    }

    public static void logBossbar(String phase,
                                  Object serverLevel,
                                  RaidEncounterSnapshot snapshot,
                                  long gameTime,
                                  int lastWave,
                                  int baselineRaiders,
                                  int lastAliveRaiders,
                                  float lastProgress) {
        logBossbar(phase, serverLevel, snapshot, gameTime, lastWave, baselineRaiders, lastAliveRaiders, lastProgress,
                -1, -1, -1, -1, "legacy", false, false, false, false, Float.NaN, Float.NaN, "legacy-diagnostic-call");
    }

    public static void logBossbar(String phase,
                                  Object serverLevel,
                                  RaidEncounterSnapshot snapshot,
                                  long gameTime,
                                  int lastWave,
                                  int baselineRaiders,
                                  int lastAliveRaiders,
                                  float lastProgress,
                                  int alive,
                                  int nativeCount,
                                  int sessionCount,
                                  int nearbyCount,
                                  String countSource,
                                  boolean waveChange,
                                  boolean baselineReset,
                                  boolean refillAttempt,
                                  boolean progressApplied,
                                  float computedProgress,
                                  float vanillaProgress,
                                  String decision) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_BOSSBAR || snapshot == null) {
            return;
        }
        String logKey = snapshot.key() + ":" + phase + ":" + snapshot.currentWave() + ":" + waveChange + ":" + refillAttempt + ":" + baselineRaiders + ":" + alive + ":" + countSource;
        if (!waveChange && !refillAttempt && !progressApplied && !shouldLog("bossbar:" + phase, logKey, gameTime)) {
            return;
        }
        emit("bossbar", "phase=" + safe(phase)
                + " dimensionId=" + safe(snapshot.dimensionId())
                + " center=" + center(snapshot.centerX(), snapshot.centerY(), snapshot.centerZ())
                + " snapshot.key=" + safe(snapshot.key())
                + " bossBarKey=" + safe(snapshot.key())
                + " raidSessionKeyCandidate=" + safe(snapshot.key())
                + " villageKey=" + villageKey(snapshot.dimensionId(), snapshot.centerX(), snapshot.centerY(), snapshot.centerZ())
                + " raidInstanceKeyCandidate=" + raidInstanceCandidate(snapshot.dimensionId(), snapshot.key(), safeHash(snapshot.key()), snapshot.gameTime(), null)
                + KeyDebugService.auditFields("bossbar", snapshot.dimensionId(), snapshot.centerX(), snapshot.centerY(), snapshot.centerZ(),
                raidInstanceCandidate(snapshot.dimensionId(), snapshot.key(), safeHash(snapshot.key()), snapshot.gameTime(), null),
                villageKey(snapshot.dimensionId(), snapshot.centerX(), snapshot.centerY(), snapshot.centerZ()), "unknown", "unknown")
                + " wave=" + snapshot.currentWave()
                + " currentWave=" + snapshot.currentWave()
                + " totalWaves=" + snapshot.totalWaves()
                + " previousLastWave=" + lastWave
                + " waveChange=" + waveChange
                + " baselineReset=" + baselineReset
                + " alive=" + alive
                + " baseline=" + baselineRaiders
                + " progress=" + progressText(computedProgress)
                + " countSource=" + safe(countSource)
                + " nativeCount=" + nativeCount
                + " sessionCount=" + sessionCount
                + " nearbyCount=" + nearbyCount
                + " lastAliveRaiders=" + lastAliveRaiders
                + " lastProgress=" + progressText(lastProgress)
                + " vanillaProgress=" + progressText(vanillaProgress)
                + " refillAttempt=" + refillAttempt
                + " progressApplied=" + progressApplied
                + " decision=" + safe(decision)
                + " snapshotGameTime=" + snapshot.gameTime()
                + " gameTime=" + gameTime
                + " snapshotAgeTicks=" + Math.max(0L, gameTime - snapshot.gameTime())
                + " staleSnapshotCandidate=" + (gameTime - snapshot.gameTime() > Math.max(100L, KeyDiagnosticsConfig.LOG_INTERVAL_TICKS))
                + " levelDimension=" + dimensionId(serverLevel) + ".");
    }

    public static void logFavorStoragePath(ServerLevel level, Path file, boolean fallback) {
        if (!KeyDiagnosticsConfig.ENABLED || !KeyDiagnosticsConfig.LOG_STORAGE_PATHS) {
            return;
        }
        String key = file == null ? "null" : file.toString();
        if (!shouldLog("favor-storage", key, 0L)) {
            return;
        }
        emit("favor-storage", "source=" + (fallback ? "config-fallback" : "world-data")
                + " file=" + key
                + " levelDimension=" + dimensionId(level)
                + (fallback ? " WARNING=config fallback is world-state storage and can cross-contaminate saves." : "")
                + ".");
    }

    public static String villageKey(String dimensionId, int centerX, int centerY, int centerZ) {
        return RaidKeyService.villageKey(dimensionId, centerX, centerY, centerZ);
    }

    public static String raidInstanceCandidate(String dimensionId, String stateOrRaidKey, int sessionRaidId, long firstSeenGameTime, Object nativeRaid) {
        return RaidKeyService.raidInstanceCandidate(dimensionId, stateOrRaidKey, sessionRaidId, firstSeenGameTime, nativeRaid);
    }

    private static void emit(String category, String message) {
        String line = prefix(category) + message;
        LOGGER.info(line);
        appendToDiagnosticFile(line);
    }

    private static void emitAlways(String category, String message) {
        String line = prefix(category) + message;
        LOGGER.info(line);
        appendToDiagnosticFile(line);
    }

    private static String prefix(String category) {
        String safeCategory = safe(category);
        if ("bossbar".equalsIgnoreCase(safeCategory)) {
            return "[Raid Enhancement Patch][KeyDiag][BossBarDiag][bossbar] ";
        }
        return "[Raid Enhancement Patch][KeyDiag][" + safeCategory + "] ";
    }

    private static void appendToDiagnosticFile(String line) {
        try {
            Path dir = KeyDiagnosticsConfig.configDir();
            Files.createDirectories(dir);
            Path file = dir.resolve("key_diagnostics.log");
            String stamped = Instant.now() + " " + line + System.lineSeparator();
            Files.writeString(file, stamped, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (IOException | RuntimeException throwable) {
            if (!warnedFileOutput) {
                warnedFileOutput = true;
                LOGGER.warn("[Raid Enhancement Patch][KeyDiag][file-output] failed to append config/raid_enhancement_patch/key_diagnostics.log: {}", throwable.toString());
            }
        }
    }

    private static String playerFavorKeys(String dimensionId, int centerX, int centerY, int centerZ, Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) {
            return "[]";
        }
        int max = Math.max(1, KeyDiagnosticsConfig.MAX_PLAYER_KEYS_PER_LINE);
        StringBuilder builder = new StringBuilder("[");
        int count = 0;
        int emitted = 0;
        for (Player player : players) {
            if (player == null) {
                continue;
            }
            count++;
            if (emitted >= max) {
                continue;
            }
            if (emitted > 0) {
                builder.append(", ");
            }
            builder.append(player.getUUID()).append("=")
                    .append(VillageFavorState.recordKey(dimensionId, centerX, centerY, centerZ, player.getUUID()));
            emitted++;
        }
        if (count > emitted) {
            builder.append(", ... +").append(count - emitted).append(" more");
        }
        builder.append(']');
        return builder.toString();
    }

    private static List<UUID> playerUuids(Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) {
            return List.of();
        }
        List<UUID> result = new java.util.ArrayList<>();
        for (Player player : players) {
            if (player != null && player.getUUID() != null && !result.contains(player.getUUID())) {
                result.add(player.getUUID());
            }
        }
        return List.copyOf(result);
    }

    private static boolean shouldLog(String event, String key, long gameTime) {
        String mapKey = safe(event) + "|" + safe(key);
        long interval = Math.max(20L, KeyDiagnosticsConfig.LOG_INTERVAL_TICKS);
        Long previous = LAST_LOG_BY_EVENT_AND_KEY.get(mapKey);
        if (previous != null && gameTime >= 0L && previous >= 0L && gameTime - previous < interval) {
            return false;
        }
        LAST_LOG_BY_EVENT_AND_KEY.put(mapKey, gameTime);
        if (LAST_LOG_BY_EVENT_AND_KEY.size() > 512) {
            int toRemove = Math.max(1, LAST_LOG_BY_EVENT_AND_KEY.size() - 384);
            for (String oldest : List.copyOf(LAST_LOG_BY_EVENT_AND_KEY.keySet())) {
                LAST_LOG_BY_EVENT_AND_KEY.remove(oldest);
                if (--toRemove <= 0) {
                    break;
                }
            }
        }
        return true;
    }

    private static String dimensionId(Object level) {
        try {
            Object dimension = level.getClass().getMethod("dimension").invoke(level);
            Object location = dimension.getClass().getMethod("location").invoke(dimension);
            return location == null ? "unknown" : location.toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static String nativeIdentity(Object nativeRaid) {
        if (nativeRaid == null) {
            return "null";
        }
        Integer id = nativeRaidNumericId(nativeRaid);
        return id == null
                ? "identity:" + System.identityHashCode(nativeRaid)
                : "id:" + id + "/identity:" + System.identityHashCode(nativeRaid);
    }

    private static Integer nativeRaidNumericId(Object nativeRaid) {
        for (String methodName : List.of("getId", "getRaidId", "id")) {
            try {
                Method method = nativeRaid.getClass().getMethod(methodName);
                Object result = method.invoke(nativeRaid);
                if (result instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String center(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static String decimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String progressText(float value) {
        if (Float.isNaN(value)) {
            return "NaN";
        }
        if (Float.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private static int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String sanitize(String input) {
        return safe(input).replace(':', '_').replace('/', '_').replace(' ', '_').replace('#', '_');
    }
}
