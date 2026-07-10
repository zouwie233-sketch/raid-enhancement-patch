package com.noah.raidenhancement.raid;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 0.9.1.4 diagnostic-only boundary projection for victory settlement.
 *
 * <p>No reward, duplicate guard, VillageFavor, cleanup, key, or persistence
 * decision is delegated to this class in this version.</p>
 */
public final class VictorySettlementBoundaryAudit {
    public static final String STAGE = "0.9.1.4-victory-settlement-boundary-audit-alpha";
    public static final String MODE = "audit-projection-only-no-runtime-consumer-change";

    private VictorySettlementBoundaryAudit() {
    }

    public static String startupMarker() {
        return "victorySettlementBoundaryStage=" + STAGE
                + " victorySettlementAuditMode=" + MODE
                + " raidCompletionResultRuntimeAuthority=false"
                + " runtimeSettlementOrderChanged=false"
                + " duplicateGuardChanged=false"
                + " rewardDispatchChanged=false"
                + " villageFavorWriteChanged=false"
                + " cleanupChanged=false"
                + " keyFormatChanged=false";
    }

    public static boolean isBoundaryPhase(String phase) {
        return "before-history-check".equals(phase)
                || "accepted-before-rewards".equals(phase);
    }

    public static RaidCompletionResult project(String raidInstanceKey,
                                                String villageKey,
                                                String dimensionId,
                                                int centerX,
                                                int centerY,
                                                int centerZ,
                                                Collection<UUID> eligiblePlayerUuids,
                                                int omenLevel,
                                                int totalWaves,
                                                long completedGameTime) {
        List<UUID> players = eligiblePlayerUuids == null
                ? List.of()
                : eligiblePlayerUuids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        return new RaidCompletionResult(
                raidInstanceKey,
                villageKey,
                dimensionId,
                centerX,
                centerY,
                centerZ,
                true,
                players,
                omenLevel,
                totalWaves,
                completedGameTime
        );
    }

    public static String auditFields(String phase,
                                     RaidCompletionResult result,
                                     boolean eligiblePlayersResolved,
                                     String settlementKeyMode) {
        if (result == null) {
            return startupMarker()
                    + " raidCompletionResultCreated=false"
                    + " boundaryPhase=" + safe(phase)
                    + " boundaryProjectionFailure=true";
        }
        return startupMarker()
                + " raidCompletionResultCreated=true"
                + " boundaryPhase=" + safe(phase)
                + " boundaryPhaseRole=" + phaseRole(phase)
                + " completionResultMode=audit-projection-not-authoritative-runtime-source"
                + " completionRaidInstanceKey=" + safe(result.raidInstanceKey())
                + " completionVillageKey=" + safe(result.villageKey())
                + " completionDimension=" + safe(result.dimensionId())
                + " completionCenter=" + result.center()
                + " completionVictory=" + result.victory()
                + " completionEligiblePlayersResolved=" + eligiblePlayersResolved
                + " completionEligiblePlayerCount=" + result.eligiblePlayerCount()
                + " completionEligiblePlayers=" + compactPlayers(result.eligiblePlayerUuids())
                + " completionOmenLevel=" + result.omenLevel()
                + " completionTotalWaves=" + result.totalWaves()
                + " completionGameTime=" + result.completedGameTime()
                + " completionIdentityPresent=" + result.identityPresent()
                + " completionSettlementKeyMode=" + safe(settlementKeyMode)
                + " completionFieldCompleteness=" + fieldCompleteness(eligiblePlayersResolved)
                + " consumersStillUseLegacyArguments=true";
    }

    private static String phaseRole(String phase) {
        if ("before-history-check".equals(phase)) {
            return "pre-duplicate-guard-snapshot";
        }
        if ("accepted-before-rewards".equals(phase)) {
            return "accepted-settlement-snapshot-before-reward-dispatch";
        }
        return "other";
    }

    private static String fieldCompleteness(boolean eligiblePlayersResolved) {
        return eligiblePlayersResolved
                ? "identity-outcome-participants-omen-waves-time"
                : "identity-outcome-omen-waves-time-participants-pending";
    }

    private static String compactPlayers(List<UUID> players) {
        if (players == null || players.isEmpty()) {
            return "[]";
        }
        int max = Math.min(8, players.size());
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(players.get(i));
        }
        if (players.size() > max) {
            builder.append(",...+").append(players.size() - max);
        }
        return builder.append(']').toString();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
