package com.noah.raidenhancement.raid.runtime;

import java.util.List;
import java.util.UUID;

/** Read-only battle-support view of one active village-security session. */
public record VillageSecurityRuntimeView(
        String raidKey,
        String dimensionId,
        int centerX,
        int centerY,
        int centerZ,
        List<UUID> securityGolemIds
) {
    public VillageSecurityRuntimeView {
        securityGolemIds = securityGolemIds == null ? List.of() : List.copyOf(securityGolemIds);
    }
}
