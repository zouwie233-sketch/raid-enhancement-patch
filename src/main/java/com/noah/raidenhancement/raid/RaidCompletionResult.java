package com.noah.raidenhancement.raid;

import java.util.List;
import java.util.UUID;

/**
 * 0.9.1.4 read-only projection of the data already available at the victory
 * settlement boundary.
 *
 * <p>This record is not an authoritative gameplay source in 0.9.1.4. It is
 * created only by diagnostics so the existing settlement path can be audited
 * before any future consumer migration is considered.</p>
 */
public record RaidCompletionResult(
        String raidInstanceKey,
        String villageKey,
        String dimensionId,
        int centerX,
        int centerY,
        int centerZ,
        boolean victory,
        List<UUID> eligiblePlayerUuids,
        int omenLevel,
        int totalWaves,
        long completedGameTime
) {
    public RaidCompletionResult {
        raidInstanceKey = safe(raidInstanceKey);
        villageKey = safe(villageKey);
        dimensionId = safe(dimensionId);
        eligiblePlayerUuids = eligiblePlayerUuids == null ? List.of() : List.copyOf(eligiblePlayerUuids);
    }

    public int eligiblePlayerCount() {
        return eligiblePlayerUuids.size();
    }

    public String center() {
        return centerX + "," + centerY + "," + centerZ;
    }

    public boolean identityPresent() {
        return !"unknown".equals(raidInstanceKey)
                && !"unknown".equals(villageKey)
                && !"unknown".equals(dimensionId);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
