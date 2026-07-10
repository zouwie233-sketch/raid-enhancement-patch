package com.noah.raidenhancement.favor;

import com.noah.raidenhancement.raid.RaidCompletionResult;
import com.noah.raidenhancement.raid.RaidKeyDiagnostics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Settlement-facing boundary for VillageFavor reads and raid-victory writes.
 *
 * <p>0.9.1.5 is intentionally a compatibility bridge. Existing runtime calls
 * enter here, diagnostics describe the boundary, and the actual behavior is
 * delegated unchanged to VillageFavorSystem.</p>
 */
public final class VillageFavorGateway {
    private VillageFavorGateway() {
    }

    public static int favorLevelFor(ServerLevel level,
                                    String dimensionId,
                                    int centerX,
                                    int centerY,
                                    int centerZ,
                                    UUID playerUuid) {
        audit("read-favor-level-before-reward", level, dimensionId, centerX, centerY, centerZ,
                playerUuid, playerUuid == null ? 0 : 1, gameTime(level), 0, false, false, "legacy-arguments");
        return VillageFavorSystem.favorLevelFor(level, dimensionId, centerX, centerY, centerZ, playerUuid);
    }

    public static void recordRaidVictory(ServerLevel level,
                                         String dimensionId,
                                         int centerX,
                                         int centerY,
                                         int centerZ,
                                         Collection<? extends Player> players,
                                         long gameTime) {
        recordRaidVictory(level, dimensionId, centerX, centerY, centerZ, players, gameTime, 1, "NORMAL", false);
    }

    public static void recordRaidVictory(ServerLevel level,
                                         String dimensionId,
                                         int centerX,
                                         int centerY,
                                         int centerZ,
                                         Collection<? extends Player> players,
                                         long gameTime,
                                         int omenLevel,
                                         String difficultyName,
                                         boolean extraWaveCompleted) {
        int eligiblePlayerCount = playerCount(players);
        audit("before-delegate-legacy", level, dimensionId, centerX, centerY, centerZ,
                null, eligiblePlayerCount, gameTime, omenLevel, extraWaveCompleted, false, "legacy-arguments");
        VillageFavorSystem.recordRaidVictory(level, dimensionId, centerX, centerY, centerZ, players, gameTime,
                omenLevel, difficultyName, extraWaveCompleted);
        audit("after-delegate-legacy", level, dimensionId, centerX, centerY, centerZ,
                null, eligiblePlayerCount, gameTime, omenLevel, extraWaveCompleted, false, "legacy-arguments");
    }

    /**
     * Future consumer-migration entry. It is available for architecture
     * verification, but 0.9.1.5 does not route VictorySettlement through this
     * overload yet.
     */
    public static void recordRaidVictory(ServerLevel level,
                                         RaidCompletionResult result,
                                         Collection<? extends Player> players,
                                         String difficultyName,
                                         boolean extraWaveCompleted) {
        if (result == null) {
            audit("rejected-null-completion-result", level, "unknown", 0, 0, 0,
                    null, playerCount(players), gameTime(level), 0, extraWaveCompleted, true, "raid-completion-result");
            return;
        }
        int eligiblePlayerCount = playerCount(players);
        audit("before-delegate-completion-result", level, result.dimensionId(),
                result.centerX(), result.centerY(), result.centerZ(), null, eligiblePlayerCount,
                result.completedGameTime(), result.omenLevel(), extraWaveCompleted, true, "raid-completion-result");
        VillageFavorSystem.recordRaidVictory(level, result.dimensionId(), result.centerX(), result.centerY(), result.centerZ(),
                players, result.completedGameTime(), result.omenLevel(), difficultyName, extraWaveCompleted);
        audit("after-delegate-completion-result", level, result.dimensionId(),
                result.centerX(), result.centerY(), result.centerZ(), null, eligiblePlayerCount,
                result.completedGameTime(), result.omenLevel(), extraWaveCompleted, true, "raid-completion-result");
    }

    private static void audit(String phase,
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
        try {
            RaidKeyDiagnostics.logVillageFavorGateway(phase, level, dimensionId, centerX, centerY, centerZ,
                    playerUuid, eligiblePlayerCount, gameTime, omenLevel, extraWaveCompleted,
                    completionResultInput, gatewayEntry);
        } catch (Throwable ignored) {
            // Diagnostics must never block a favor read or write.
        }
    }

    private static int playerCount(Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) {
            return 0;
        }
        java.util.Set<UUID> unique = new java.util.HashSet<>();
        for (Player player : players) {
            if (player == null) {
                continue;
            }
            try {
                UUID uuid = player.getUUID();
                if (uuid != null) {
                    unique.add(uuid);
                }
            } catch (Throwable ignored) {
                // Count is diagnostic only; delegation remains authoritative.
            }
        }
        return unique.size();
    }

    private static long gameTime(ServerLevel level) {
        if (level == null) {
            return 0L;
        }
        try {
            Object value = level.getClass().getMethod("getGameTime").invoke(level);
            return value instanceof Number number ? number.longValue() : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }
}
